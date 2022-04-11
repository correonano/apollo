package io.muun.apollo.presentation.ui.analytics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import io.muun.apollo.R
import io.muun.apollo.databinding.ActivityAnalyticsSettingsBinding
import io.muun.apollo.presentation.ui.base.BaseViewModelActivity

class AnalyticsSettingsActivity: BaseViewModelActivity<ActivityAnalyticsSettingsBinding, AnalyticsSettingsViewModel>() {

    override val layoutId = R.layout.activity_analytics_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.header.toolbar)
        binding.header.toolbar.setNavigationOnClickListener { onBackPressed() }
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(R.string.analytics_settings_title)
        }

        viewModel.navigator.observe(this) {
            when (it) {
                AnalyticsSettingsViewModel.Navigation.Error -> {
                    Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun inject() {
        component.inject(this)
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, AnalyticsSettingsActivity::class.java)
    }
}