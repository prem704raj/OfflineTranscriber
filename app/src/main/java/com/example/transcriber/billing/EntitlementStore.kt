package com.example.transcriber.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.entitlementDataStore by preferencesDataStore(
    name = "billing_entitlement"
)

data class CachedEntitlement(
    val isPro: Boolean = false,
    val lastVerifiedAt: Long = 0L
)

class EntitlementStore(
    private val context: Context
) {
    private object Keys {
        val pro = booleanPreferencesKey("pro_lifetime_owned")
        val verifiedAt = longPreferencesKey("pro_last_verified_at")
    }

    val cached: Flow<CachedEntitlement> =
        context.entitlementDataStore.data.map { prefs ->
            CachedEntitlement(
                isPro = prefs[Keys.pro] ?: false,
                lastVerifiedAt = prefs[Keys.verifiedAt] ?: 0L
            )
        }

    suspend fun setAuthoritative(isPro: Boolean) {
        context.entitlementDataStore.edit { prefs ->
            prefs[Keys.pro] = isPro
            prefs[Keys.verifiedAt] = System.currentTimeMillis()
        }
    }

    suspend fun clearLocalCache() {
        context.entitlementDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
