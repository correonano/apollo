package io.muun.apollo.presentation.ui.fragments.analytics

import io.muun.apollo.presentation.ui.base.BaseView

interface AnalyticsSettingsView: BaseView {

    fun update(enabled: Boolean)

    fun setLoading(loading: Boolean)
}