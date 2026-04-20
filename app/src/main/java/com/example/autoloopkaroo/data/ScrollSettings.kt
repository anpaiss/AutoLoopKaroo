package com.example.autoloopkaroo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autoloop_settings")

const val DEFAULT_DWELL_MS = 5_000L
const val DEFAULT_NEAR_CUE_M = 25f
const val DEFAULT_POST_TURN_M = 25f

data class ScrollConfig(
    val isEnabled: Boolean = false,
    val dwellMs: Long = DEFAULT_DWELL_MS,
    val nearCueDistanceM: Float = DEFAULT_NEAR_CUE_M,
    val postTurnDistanceM: Float = DEFAULT_POST_TURN_M
)

object PrefsKeys {
    val IS_ENABLED = booleanPreferencesKey("is_enabled")
    val DWELL_MS = longPreferencesKey("dwell_ms")
    val NEAR_CUE_DISTANCE_M = floatPreferencesKey("near_cue_distance_m")
    val POST_TURN_DISTANCE_M = floatPreferencesKey("post_turn_distance_m")
}

fun Context.scrollConfigFlow(): Flow<ScrollConfig> =
    dataStore.data.map { prefs ->
        ScrollConfig(
            isEnabled = prefs[PrefsKeys.IS_ENABLED] ?: false,
            dwellMs = prefs[PrefsKeys.DWELL_MS] ?: DEFAULT_DWELL_MS,
            nearCueDistanceM = prefs[PrefsKeys.NEAR_CUE_DISTANCE_M] ?: DEFAULT_NEAR_CUE_M,
            postTurnDistanceM = prefs[PrefsKeys.POST_TURN_DISTANCE_M] ?: DEFAULT_POST_TURN_M
        )
    }

suspend fun Context.saveScrollEnabled(enabled: Boolean) {
    dataStore.edit { it[PrefsKeys.IS_ENABLED] = enabled }
}

suspend fun Context.saveScrollConfig(config: ScrollConfig) {
    dataStore.edit { prefs ->
        prefs[PrefsKeys.IS_ENABLED] = config.isEnabled
        prefs[PrefsKeys.DWELL_MS] = config.dwellMs
        prefs[PrefsKeys.NEAR_CUE_DISTANCE_M] = config.nearCueDistanceM
        prefs[PrefsKeys.POST_TURN_DISTANCE_M] = config.postTurnDistanceM
    }
}
