package app.offlinetranscriber.mobile.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Single BillingClient manager for Google Play In-App Billing.
 *
 * NOTE: Google recommends server-side purchase verification. This application
 * runs 100% locally with no cloud backend by design. The client verifies
 * purchases directly with Google Play and caches the verified entitlement for
 * offline usage.
 */
class BillingRepository(
    context: Context,
    private val entitlementStore: EntitlementStore
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val _state = MutableStateFlow(BillingUiState())
    val state = _state.asStateFlow()

    private var rawProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            entitlementStore.cached.collectLatest { cached ->
                val entitlement = if (cached.isPro) {
                    Entitlement.PRO
                } else {
                    Entitlement.FREE
                }

                _state.value = _state.value.copy(
                    entitlement = entitlement
                )
            }
        }
    }

    fun connect() {
        if (billingClient.isReady) {
            scope.launch {
                refreshAll()
            }
            return
        }

        _state.value = _state.value.copy(
            connection = BillingConnectionState.Connecting
        )

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        connection = BillingConnectionState.Ready
                    )
                    scope.launch {
                        refreshAll()
                    }
                } else {
                    _state.value = _state.value.copy(
                        connection = BillingConnectionState.Unavailable(
                            billingResult.debugMessage.ifBlank {
                                "Google Play billing is unavailable."
                            }
                        )
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                // Auto-service reconnection is enabled.
                _state.value = _state.value.copy(
                    connection = BillingConnectionState.Connecting
                )
            }
        })
    }

    suspend fun refreshAll() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        _state.value = _state.value.copy(
            refreshing = true
        )

        queryProduct()
        queryOwnedPurchases()

        _state.value = _state.value.copy(
            refreshing = false
        )
    }

    suspend fun restorePurchase() {
        if (!billingClient.isReady) {
            connect()
            _state.value = _state.value.copy(
                message = "Connecting to Google Play…"
            )
            return
        }

        _state.value = _state.value.copy(
            refreshing = true,
            message = null
        )

        val owned = queryOwnedPurchases()

        _state.value = _state.value.copy(
            refreshing = false,
            message = if (owned) {
                "Pro purchase restored."
            } else {
                "No Pro purchase was found for this Google Play account."
            }
        )
    }

    fun launchProPurchase(activity: Activity) {
        val details = rawProductDetails
        val product = _state.value.product

        if (details == null || product == null) {
            _state.value = _state.value.copy(
                message = "Pro is not available for purchase right now. Try again."
            )
            connect()
            return
        }

        if (_state.value.entitlement == Entitlement.PRO) {
            _state.value = _state.value.copy(
                message = "Pro is already unlocked."
            )
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                if (product.offerToken.isNotBlank()) {
                    setOfferToken(product.offerToken)
                }
            }
            .build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, params)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                scope.launch {
                    queryOwnedPurchases()
                }
            } else {
                _state.value = _state.value.copy(
                    message = result.debugMessage.ifBlank {
                        "Unable to start purchase."
                    }
                )
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    processPurchases(
                        purchases = purchases.orEmpty(),
                        authoritativeEmpty = false
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(
                    message = null
                )
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch {
                    queryOwnedPurchases()
                }
            }

            else -> {
                _state.value = _state.value.copy(
                    message = billingResult.debugMessage.ifBlank {
                        "Purchase could not be completed."
                    }
                )
            }
        }
    }

    private suspend fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConstants.PRO_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val result = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (continuation.isActive) {
                    continuation.resume(billingResult to queryResult.productDetailsList)
                }
            }
        }

        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) {
            return
        }

        val details = result.second.firstOrNull {
            it.productId == BillingConstants.PRO_LIFETIME
        } ?: return

        val offer = selectBuyOffer(details) ?: return

        rawProductDetails = details

        _state.value = _state.value.copy(
            product = ProProduct(
                productId = details.productId,
                name = details.name,
                description = details.description,
                formattedPrice = offer.formattedPrice,
                offerToken = offer.offerToken.orEmpty()
            )
        )
    }

    private fun selectBuyOffer(
        details: ProductDetails
    ): ProductDetails.OneTimePurchaseOfferDetails? {
        val multiple = details.oneTimePurchaseOfferDetailsList.orEmpty()
        return multiple.firstOrNull {
            it.rentalDetails == null && it.preorderDetails == null
        } ?: details.oneTimePurchaseOfferDetails
    }

    private suspend fun queryOwnedPurchases(): Boolean {
        if (!billingClient.isReady) {
            return _state.value.entitlement == Entitlement.PRO
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (continuation.isActive) {
                    continuation.resume(billingResult to purchases)
                }
            }
        }

        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) {
            // Non-authoritative failure keeps cached ownership.
            return _state.value.entitlement == Entitlement.PRO
        }

        return processPurchases(
            purchases = result.second,
            authoritativeEmpty = true
        )
    }

    private suspend fun processPurchases(
        purchases: List<Purchase>,
        authoritativeEmpty: Boolean
    ): Boolean {
        var ownedPro = false
        var pendingPro = false

        purchases.forEach { purchase ->
            if (BillingConstants.PRO_LIFETIME !in purchase.products) {
                return@forEach
            }

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    ownedPro = true
                    if (!purchase.isAcknowledged) {
                        acknowledge(purchase.purchaseToken)
                    }
                }

                Purchase.PurchaseState.PENDING -> {
                    pendingPro = true
                }
            }
        }

        if (ownedPro || authoritativeEmpty) {
            entitlementStore.setAuthoritative(
                isPro = ownedPro
            )
        }

        _state.value = _state.value.copy(
            purchaseStatus = when {
                ownedPro -> PurchaseUiStatus.PURCHASED
                pendingPro -> PurchaseUiStatus.PENDING
                else -> PurchaseUiStatus.NONE
            },
            entitlement = if (ownedPro) {
                Entitlement.PRO
            } else if (authoritativeEmpty) {
                Entitlement.FREE
            } else {
                _state.value.entitlement
            }
        )

        return ownedPro
    }

    private suspend fun acknowledge(token: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.acknowledgePurchase(params) { _ ->
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(
            message = null
        )
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
