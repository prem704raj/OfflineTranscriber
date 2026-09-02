package com.example.transcriber.billing

sealed interface BillingConnectionState {
    data object Connecting : BillingConnectionState
    data object Ready : BillingConnectionState
    data class Unavailable(val message: String) : BillingConnectionState
}

enum class PurchaseUiStatus {
    NONE,
    PENDING,
    PURCHASED
}

data class ProProduct(
    val productId: String,
    val name: String,
    val description: String,
    val formattedPrice: String,
    val offerToken: String
)

data class BillingUiState(
    val connection: BillingConnectionState = BillingConnectionState.Connecting,
    val entitlement: Entitlement = Entitlement.FREE,
    val purchaseStatus: PurchaseUiStatus = PurchaseUiStatus.NONE,
    val product: ProProduct? = null,
    val message: String? = null,
    val refreshing: Boolean = false
)
