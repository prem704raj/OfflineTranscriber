package com.example.transcriber.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        (application as TranscriberApplication).billingRepository

    val state = repository.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BillingUiState()
    )

    fun connect() = repository.connect()

    fun buy(activity: Activity) = repository.launchProPurchase(activity)

    fun restore() {
        viewModelScope.launch {
            repository.restorePurchase()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshAll()
        }
    }

    fun clearMessage() = repository.clearMessage()
}
