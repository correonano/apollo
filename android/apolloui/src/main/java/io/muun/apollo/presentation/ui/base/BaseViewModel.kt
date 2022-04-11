package io.muun.apollo.presentation.ui.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.subscriptions.CompositeSubscription

abstract class BaseViewModel: ViewModel() {
    protected val compositeSubscription: CompositeSubscription = CompositeSubscription()

    override fun onCleared() {
        compositeSubscription.clear()
    }

    abstract fun init(bundle: Bundle?)


    protected fun <T>subscribeTo(observable: Observable<T>, onNext: Action1<T>) : Subscription {
        return observable.observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, this::handleError)
            .apply {compositeSubscription.add(this)}
    }

    protected fun handleError(error: Throwable) {
        //do nothing :)
    }
}