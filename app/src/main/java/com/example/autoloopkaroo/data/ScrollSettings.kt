package com.example.autoloopkaroo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autoloop_settings")

const val DEFAULT_DWELL_MS = 5_000L

data class ScrollConfig(
    val isEnabled: Boolean = false,
    val dwellMs: Long = DEFAULT_DWELL_MS
)

object PrefsKeys {
    val IS_ENABLED = booleanPreferencesKey("is_enabled")
    val DWELL_MS = longPreferencesKey("dwell_ms")
}

fun Context.scrollConfigFlow(): Flow<ScrollConfig> =
    dataStore.data.map { prefs ->
        ScrollConfig(
            isEnabled = prefs[PrefsKeys.IS_ENABLED] ?: false,
            dwellMs = prefs[PrefsKeys.DWELL_MS] ?: DEFAULT_DWELL_MS
        )
    }

suspend fun Context.saveScrollEnabled(enabled: Boolean) {
    dataStore.edit { it[PrefsKeys.IS_ENABLED] = enabled }
}

suspend fun Context.saveScrollConfig(config: ScrollConfig) {
    dataStore.edit { prefs ->
        prefs[PrefsKeys.IS_ENABLED] = config.isEnabled
        prefs[PrefsKeys.DWELL_MS] = config.dwellMs
    }
}
