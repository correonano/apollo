package io.muun.apollo.presentation.ui.analytics

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.muun.apollo.domain.action.base.ActionState
import io.muun.apollo.domain.action.user.MockUpdateUserPreferencesAction
import io.muun.apollo.domain.model.user.UserPreferences
import io.muun.apollo.domain.selector.UserPreferencesSelector
import io.muun.apollo.presentation.analytics.Analytics
import io.muun.apollo.presentation.ui.base.BaseViewModel
import rx.Observable
import javax.inject.Inject

class AnalyticsSettingsViewModel
@Inject constructor(val userPreferencesSelector: UserPreferencesSelector,
                    val updateUserPreferences: MockUpdateUserPreferencesAction,
                    val analytics: Analytics
): BaseViewModel() {

    private val _navigator = MutableLiveData<Navigation>()
    val navigator: LiveData<Navigation> = _navigator
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _analyticsEnable = MutableLiveData(false)
    val analyticsEnable: LiveData<Boolean> = _analyticsEnable


    override fun init(bundle: Bundle?) {
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
            ActionState.Kind.EMPTY -> {
                update(prefs)
                goTo(Navigation.Update)
            }
            ActionState.Kind.ERROR -> {
                update(prefs)
                goTo(Navigation.Error)
            }
            ActionState.Kind.LOADING -> {
                _loading.postValue(true)
            }
            ActionState.Kind.VALUE -> Unit
        }
    }

    fun update(prefs: UserPreferences) {
        _loading.postValue(false)
        _analyticsEnable.postValue(prefs.analyticsEnable)
        analytics.requestOutOpt(prefs.analyticsEnable)
    }

    fun onAnalyticsToggleCheck(enable: Boolean) {
        updateUserPreferences.run { prefs ->
            prefs.copy(analyticsEnable = enable)
        }
    }

    private fun goTo(nav: Navigation) {
        _navigator.postValue(nav)
    }

    sealed class Navigation {
        object Update : Navigation()
        object Error : Navigation()
    }
}