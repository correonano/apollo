package io.muun.apollo.presentation.ui.fragments.home

import io.muun.apollo.domain.model.BitcoinUnit
import io.muun.apollo.domain.model.Operation
import io.muun.apollo.presentation.ui.base.BaseView

interface HomeView : BaseView {

    fun setState(homeState: HomePresenter.HomeState)

    fun setNewOp(newOp: Operation, bitcoinUnit: BitcoinUnit)

    fun showTooltip()
}