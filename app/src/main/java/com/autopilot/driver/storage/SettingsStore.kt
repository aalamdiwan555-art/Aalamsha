package com.autopilot.driver.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aalam_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val minimumPrice = doublePreferencesKey("minimum_price")
        val maximumPrice = doublePreferencesKey("maximum_price")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val captureGranted = booleanPreferencesKey("capture_granted")
        val autopilotEnabled = booleanPreferencesKey("autopilot_enabled")
    }

    data class StoredSettings(
        val minimumPrice: Double = 100.0,
        val maximumPrice: Double = 150.0,
        val onboardingComplete: Boolean = false,
        val captureGranted: Boolean = false,
        val autopilotEnabled: Boolean = false,
    )

    val settings: Flow<StoredSettings> = context.dataStore.data.map { prefs ->
        StoredSettings(
            minimumPrice = prefs[Keys.minimumPrice] ?: 100.0,
            maximumPrice = prefs[Keys.maximumPrice] ?: 150.0,
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
            captureGranted = prefs[Keys.captureGranted] ?: false,
            autopilotEnabled = prefs[Keys.autopilotEnabled] ?: false,
        )
    }

    suspend fun savePriceRange(minimum: Double, maximum: Double) {
        context.dataStore.edit { it[Keys.minimumPrice] = minimum; it[Keys.maximumPrice] = maximum }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.onboardingComplete] = value }
    }

    suspend fun setCaptureGranted(value: Boolean) {
        context.dataStore.edit { it[Keys.captureGranted] = value }
    }

    suspend fun setAutopilotEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.autopilotEnabled] = value }
    }
}
