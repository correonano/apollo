package io.muun.apollo.presentation.ui.fragments.analytics

import android.os.Bundle
import io.muun.apollo.domain.action.base.ActionState
import io.muun.apollo.domain.action.user.MockUpdateUserPreferencesAction
import io.muun.apollo.domain.model.user.UserPreferences
import io.muun.apollo.domain.selector.UserPreferencesSelector
import io.muun.apollo.presentation.analytics.AnalyticsEvent.S_SETTINGS_ANALYTICS
import io.muun.apollo.presentation.ui.base.ParentPresenter
import io.muun.apollo.presentation.ui.base.SingleFragmentPresenter
import io.muun.apollo.presentation.ui.base.di.PerFragment
import rx.Observable
import javax.inject.Inject

@PerFragment
class AnalyticsSettingsPresenter
@Inject constructor(
    private val userPreferencesSelector: UserPreferencesSelector,
    private val updateUserPreferences: MockUpdateUserPreferencesAction
): SingleFragmentPresenter<AnalyticsSettingsView, ParentPresenter>() {

    override fun setUp(arguments: Bundle) {
        super.setUp(arguments)

        val combined = Observable.combineLatest(
            userPreferencesSelector.watch(),
            updateUserPreferences.state,
            UserPreferences::to
        )

        subscribeTo(combined) { pair ->
            handleState(pair.first, pair.second)
        }
    }

    private fun handleState(prefs: UserPreferences, state: ActionState<Unit>) {
        when (state.kind) {
            ActionState.Kind.EMPTY, ActionState.Kind.ERROR -> {
                view.setLoading(false)
                view.update(prefs.analyticsEnable)
                analytics.requestOutOpt(prefs.analyticsEnable)
            }
            ActionState.Kind.LOADING -> {
                view.setLoading(true)
            }
            ActionState.Kind.VALUE -> Unit
        }
    }

    fun toggle(enable: Boolean) {
        updateUserPreferences.run { prefs ->
            prefs.copy(analyticsEnable = enable)
        }
    }

    override fun getEntryEvent() = S_SETTINGS_ANALYTICS()
}
