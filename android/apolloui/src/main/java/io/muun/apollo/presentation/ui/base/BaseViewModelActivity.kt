package io.muun.apollo.presentation.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import io.muun.apollo.BR
import io.muun.apollo.presentation.app.ApolloApplication
import io.muun.apollo.presentation.ui.base.di.ActivityComponent
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

abstract class BaseViewModelActivity<B: ViewDataBinding, VM : BaseViewModel> : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    internal lateinit var viewModel: VM
    lateinit var binding: B

    open val layoutId = -1

    lateinit var component: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = (application as ApolloApplication).applicationComponent.activityComponent()
        inject()
        viewModel = buildViewModel()

        if (layoutId != -1) {
            binding = DataBindingUtil.setContentView(this, layoutId)
            binding.lifecycleOwner = this
            binding.setVariable(BR.viewModel, viewModel)
        }
        viewModel.init(intent.extras)
    }

    private fun buildViewModel(): VM {
        val viewModelClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>

        return ViewModelProvider(this, viewModelFactory).get(viewModelClass)
    }

    protected abstract fun inject()
}