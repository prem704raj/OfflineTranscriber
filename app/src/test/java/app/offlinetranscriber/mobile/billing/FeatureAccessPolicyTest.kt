package app.offlinetranscriber.mobile.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessPolicyTest {

    @Test
    fun freeCannotUseProFeature() {
        assertFalse(
            FeatureAccessPolicy.hasAccess(
                Entitlement.FREE,
                ProFeature.STUDY_MODE
            )
        )
        assertFalse(
            FeatureAccessPolicy.hasAccess(
                Entitlement.FREE,
                ProFeature.ACCURATE_MODEL
            )
        )
        assertFalse(
            FeatureAccessPolicy.hasAccess(
                Entitlement.FREE,
                ProFeature.VIDEO_TRANSCRIPTION
            )
        )
        assertFalse(
            FeatureAccessPolicy.hasAccess(
                Entitlement.FREE,
                ProFeature.SUBTITLE_EXPORT
            )
        )
    }

    @Test
    fun proCanUseAllFeatures() {
        ProFeature.entries.forEach { feature ->
            assertTrue(
                FeatureAccessPolicy.hasAccess(
                    Entitlement.PRO,
                    feature
                )
            )
        }
    }

    @Test
    fun freeCollectionLimitIsThree() {
        assertTrue(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.FREE,
                0
            )
        )
        assertTrue(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.FREE,
                2
            )
        )
        assertFalse(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.FREE,
                3
            )
        )
        assertFalse(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.FREE,
                4
            )
        )
    }

    @Test
    fun proCollectionUnlimited() {
        assertTrue(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.PRO,
                0
            )
        )
        assertTrue(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.PRO,
                3
            )
        )
        assertTrue(
            FeatureAccessPolicy.canCreateCollection(
                Entitlement.PRO,
                999
            )
        )
    }

    @Test
    fun freeQueueLimitIsThreeUnfinished() {
        assertTrue(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.FREE,
                0
            )
        )
        assertTrue(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.FREE,
                2
            )
        )
        assertFalse(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.FREE,
                3
            )
        )
        assertFalse(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.FREE,
                5
            )
        )
    }

    @Test
    fun proQueueUnlimited() {
        assertTrue(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.PRO,
                0
            )
        )
        assertTrue(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.PRO,
                3
            )
        )
        assertTrue(
            FeatureAccessPolicy.canEnqueue(
                Entitlement.PRO,
                50
            )
        )
    }

    @Test
    fun billingProductIdConstant() {
        assertEquals("pro_lifetime", BillingConstants.PRO_LIFETIME)
    }
}
