package com.medkeen.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "medkeen_settings")

object SettingsDataStore {
    private val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")

    fun biometricLockEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    suspend fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }
}
