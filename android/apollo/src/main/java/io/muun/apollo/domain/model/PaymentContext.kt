package io.muun.apollo.domain.model

import io.muun.apollo.domain.model.user.User
import io.muun.apollo.domain.utils.FeeCalculator
import io.muun.common.Rules
import io.muun.common.model.ExchangeRateProvider
import io.muun.common.utils.BitcoinUtils
import io.muun.common.utils.Preconditions
import newop.PaymentContext
import org.javamoney.moneta.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount

/**
 * The contextual information required to analyze and process a PaymentRequest.
 */
class PaymentContext(
    val user: User,
    val exchangeRateWindow: ExchangeRateWindow,
    val feeWindow: FeeWindow,
    val nextTransactionSize: NextTransactionSize,
    val minFeeRate: Double = Rules.OP_MINIMUM_FEE_RATE
) {

    private val rateProvider = ExchangeRateProvider(exchangeRateWindow.rates)

    /** The total UI balance in the wallet, independent of fees, as calculated by NTS */
    val userBalance = nextTransactionSize.userBalance

    /** The total UTXO balance in the wallet, independent of fees, as calculated by NTS */
    val utxoBalance = nextTransactionSize.utxoBalance

    /** The recommended fee rates for the user to pick */
    private val feeOptions by lazy {
        feeWindow.targetedFees
            .mapValues { (confTarget, satoshisPerByte) ->
                val feeCalculator = FeeCalculator(satoshisPerByte, nextTransactionSize)

                FeeOption(
                    satoshisPerByte = satoshisPerByte,
                    confirmationTarget = confTarget,
                    feeCalculator = feeCalculator
                )
            }
            .toSortedMap()
    }

    val fastFeeOption = closestFeeOptionFasterThan(feeWindow.fastConfTarget)

    val mediumFeeOption = closestFeeOptionFasterThan(feeWindow.mediumConfTarget)

    val slowFeeOption = closestFeeOptionFasterThan(feeWindow.slowConfTarget)

    /**
     * Get the fee option with the minimum available fee rate that will hit a given confirmation
     * target. We make no guesses (no averages or interpolations), so we might overshoot the fee
     * if data is too sparse.
     * Note: the lower the confirmation target, the faster the tx will confirm, and greater the
     * fee(rate) will be.
     * Assumes feeOptions is a SortedMap sorted in ascending order, and has at least one item.
     */
    private fun closestFeeOptionFasterThan(confirmationTarget: Int): FeeOption {
        Preconditions.checkPositive(confirmationTarget)

        for (closestTarget in confirmationTarget downTo 1) {
            if (feeOptions.containsKey(closestTarget)) {
                // Found! This is the lowest fee rate that hits the given target.
                return feeOptions[closestTarget]!!
            }
        }

        // No result? This is odd, but not illogical. It means *all* of our available targets
        // are above the requested one. Let's use the fastest:
        val lowestTarget = feeOptions.firstKey()
        return feeOptions[lowestTarget]!!
    }

    /**
     * Assumes feeOptions is a SortedMap sorted in ascending order, and has at least one item.
     */
    fun estimateMaxTimeMsFor(feeRate: Double): Long {

        for (feeOption in feeOptions.values) {
            if (feeOption.satoshisPerByte <= feeRate) {
                return feeOption.maxTimeMs
            }
        }

        return feeOptions[feeOptions.lastKey()]!!.maxTimeMs
    }

    /**
     * Return the minimum acceptable fee rate, obtained from the lowest recommended value.
     */
    fun getMinimumFeeRate() = minFeeRate

    /**
     * Examine a PaymentRequest, and return a PaymentAnalysis.
     */
    fun analyze(payReq: PaymentRequest): PaymentAnalysis {
        checkNotNull(payReq.amount)

        return PaymentAnalyzer(this, payReq).analyze()
    }

    /**
     * Modify PaymentRequest amount, to perform slightly different analysis. For certain
     * PaymentRequest this can be a little cumbersome (swaps and Collect swaps) or don't make sense
     * (amount fixed).
     */
    fun analyzeUseAllFunds(payReq: PaymentRequest): PaymentAnalysis {

        val allFunds: MonetaryAmount = convert(userBalance, payReq.amount!!.currency)

        // TODO UseAllFunds shouldn't need amount, take userBalance from payCtx
        var simulatedUseAllFundsPayReq = payReq
            .withAmount(allFunds)
            .withTakeFeeFromAmount(true)

        if (payReq.swap?.bestRouteFees == null) {
           simulatedUseAllFundsPayReq = simulatedUseAllFundsPayReq
               .withSwapAmount(userBalance)
        }

        return analyze(simulatedUseAllFundsPayReq)
    }

    /**
     * Take a valid PaymentRequest (verify using `analyze`), and create a PreparedPayment.
     */
    fun prepare(payReq: PaymentRequest): PreparedPayment {
        val analysis = analyze(payReq)

        check(analysis.isValid())
        checkNotNull(analysis.fee) // these two are implied, but we hint the compiler
        checkNotNull(analysis.total)

        return PreparedPayment(
                analysis.amount,
                analysis.fee,
                analysis.payReq.description,
                analysis.rateWindow.windowHid,
                nextTransactionSize.extractOutpoints(),
                analysis.payReq
        )
    }

    /**
     * Convert a MonetaryAmount to Satoshis.
     */
    fun convertToSatoshis(amount: MonetaryAmount) =
        convert(amount, Monetary.getCurrency("BTC")).let(BitcoinUtils::bitcoinsToSatoshis)

    /**
     * Convert satoshis to a MonetaryAmount.
     */
    fun convert(amountInSatoshis: Long, targetUnit: CurrencyUnit) =
        convert(convertToBitcoin(amountInSatoshis), targetUnit)

    /**
     * Convert satoshis to a MonetaryAmount in BTC.
     */
    fun convertToBitcoin(amountInSatoshis: Long) =
        Money.of(amountInSatoshis, "BTC").scaleByPowerOfTen(-BitcoinUtils.BITCOIN_PRECISION)

    /**
     * Convert a MonetaryAmount to another currency.
     */
    fun convert(amount: MonetaryAmount, targetUnit: CurrencyUnit) =
        amount.with(rateProvider.getCurrencyConversion(targetUnit))

    /**
     * Convert satoshis to a complex BitcoinAmount.
     */
    fun convertToBitcoinAmount(amountInSatoshis: Long, inputCurrency: CurrencyUnit) =
        BitcoinAmount(
            amountInSatoshis,
            convert(amountInSatoshis, inputCurrency),
            convert(amountInSatoshis, user.getPrimaryCurrency(exchangeRateWindow))
        )

    /**
     * Adapt apollo's (java) model to libwallet's (go).
     */
    fun toLibwallet(submarineSwap: SubmarineSwap?): PaymentContext {
        val libwalletPayCtx = PaymentContext()
        libwalletPayCtx.feeWindow = feeWindow.toLibwallet()
        libwalletPayCtx.exchangeRateWindow = exchangeRateWindow.toLibwallet()
        libwalletPayCtx.nextTransactionSize = nextTransactionSize.toLibwallet()
        libwalletPayCtx.primaryCurrency = user.getPrimaryCurrency(exchangeRateWindow).currencyCode
        libwalletPayCtx.minFeeRateInSatsPerVByte = Rules.toSatsPerVbyte(minFeeRate)
        if (submarineSwap != null) {
            libwalletPayCtx.submarineSwap = submarineSwap.toLibwallet()
        }
        return libwalletPayCtx
    }
}
