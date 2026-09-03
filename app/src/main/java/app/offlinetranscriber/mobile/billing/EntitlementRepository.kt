package app.offlinetranscriber.mobile.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntitlementRepository(
    billingRepository: BillingRepository
) {
    val entitlement: Flow<Entitlement> =
        billingRepository.state.map { it.entitlement }

    val isPro: Flow<Boolean> =
        entitlement.map { it == Entitlement.PRO }

    fun hasFeature(
        entitlement: Entitlement,
        feature: ProFeature
    ): Boolean =
        FeatureAccessPolicy.hasAccess(entitlement, feature)
}
