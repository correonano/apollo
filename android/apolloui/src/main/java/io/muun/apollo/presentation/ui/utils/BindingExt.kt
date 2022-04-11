package io.muun.apollo.presentation.ui.fragments.analytics

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("muun:visibleOrGone")
fun visibleOrGone(view: View, visible: Boolean) {
    view.visibility = if(visible) View.VISIBLE else View.GONE
}