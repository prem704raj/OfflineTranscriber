package com.example.transcriber.billing

object FeatureAccessPolicy {

    const val FREE_COLLECTION_LIMIT = 3
    const val FREE_UNFINISHED_QUEUE_LIMIT = 3

    fun hasAccess(
        entitlement: Entitlement,
        feature: ProFeature
    ): Boolean {
        return entitlement == Entitlement.PRO
    }

    fun canCreateCollection(
        entitlement: Entitlement,
        currentCount: Int
    ): Boolean {
        return entitlement == Entitlement.PRO ||
            currentCount < FREE_COLLECTION_LIMIT
    }

    fun canEnqueue(
        entitlement: Entitlement,
        unfinishedCount: Int
    ): Boolean {
        return entitlement == Entitlement.PRO ||
            unfinishedCount < FREE_UNFINISHED_QUEUE_LIMIT
    }
}
