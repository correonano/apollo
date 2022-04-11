package io.muun.apollo.presentation.ui.fragments.analytics

import android.view.View
import butterknife.BindView
import com.google.android.material.switchmaterial.SwitchMaterial
import io.muun.apollo.R
import io.muun.apollo.presentation.ui.base.SingleFragment
import io.muun.apollo.presentation.ui.view.LoadingView
import io.muun.apollo.presentation.ui.view.MuunHeader


class AnalyticsSettingsFragment : SingleFragment<AnalyticsSettingsPresenter>(), AnalyticsSettingsView {

    @BindView(R.id.analytics_settings_section)
    lateinit var mainSection: View

    @BindView(R.id.analytics_settings_loading)
    lateinit var loadingView: LoadingView

    @BindView(R.id.analytics_settings_switch)
    lateinit var analyticsSwitch: SwitchMaterial

    override fun initializeUi(view: View?) {
        super.initializeUi(view)

        analyticsSwitch.setOnCheckedChangeListener { _, state ->
            presenter.toggle(state)
        }

        parentActivity.header.apply {
            showTitle(R.string.analytics_settings_title)
            setNavigation(MuunHeader.Navigation.BACK)
        }
    }

    override fun inject() {
        component.inject(this)
    }

    override fun getLayoutResource(): Int {
        return R.layout.fragment_analytics_settings
    }

    override fun update(enabled: Boolean) {
        analyticsSwitch.isChecked = enabled
    }

    override fun setLoading(loading: Boolean) {
        mainSection.visibility = if (loading) { View.GONE } else { View.VISIBLE }
        loadingView.visibility = if (loading) { View.VISIBLE } else { View.GONE }
    }
}