package com.example.autoloopkaroo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autoloop_settings")

const val MAX_PAGES = 10
const val DEFAULT_DWELL_MS = 5_000L
const val DEFAULT_NEAR_CUE_M = 25f
const val DEFAULT_POST_TURN_M = 10f
const val DEFAULT_NEAR_CUE_SECONDS = 5f
const val DEFAULT_MIN_NEAR_CUE_M = 15f

data class ScrollConfig(
    val isEnabled: Boolean = false,
    val pageDwellMs: List<Long> = emptyList(),
    // fixed-distance mode
    val nearCueDistanceM: Float = DEFAULT_NEAR_CUE_M,
    val postTurnDistanceM: Float = DEFAULT_POST_TURN_M,
    // speed-sensitive mode
    val useSpeedSensitive: Boolean = false,
    val nearCueSeconds: Float = DEFAULT_NEAR_CUE_SECONDS,
    val minNearCueDistanceM: Float = DEFAULT_MIN_NEAR_CUE_M
) {
    fun dwellForPage(index: Int): Long =
        pageDwellMs.getOrElse(index) { DEFAULT_DWELL_MS }

    fun effectiveNearCueDistanceM(speedMs: Float): Float =
        if (useSpeedSensitive)
            (speedMs * nearCueSeconds).coerceAtLeast(minNearCueDistanceM)
        else
            nearCueDistanceM
}

object PrefsKeys {
    val IS_ENABLED = booleanPreferencesKey("is_enabled")
    val PAGE_DWELL_MS = stringPreferencesKey("page_dwell_ms")
    val NEAR_CUE_DISTANCE_M = floatPreferencesKey("near_cue_distance_m")
    val POST_TURN_DISTANCE_M = floatPreferencesKey("post_turn_distance_m")
    val USE_SPEED_SENSITIVE = booleanPreferencesKey("use_speed_sensitive")
    val NEAR_CUE_SECONDS = floatPreferencesKey("near_cue_seconds")
    val MIN_NEAR_CUE_DISTANCE_M = floatPreferencesKey("min_near_cue_distance_m")
}

private fun String.toPageDwellList(): List<Long> =
    split(",").mapNotNull { it.trim().toLongOrNull() }

private fun List<Long>.toPageDwellString(): String = joinToString(",")

fun Context.scrollConfigFlow(): Flow<ScrollConfig> =
    dataStore.data.map { prefs ->
        ScrollConfig(
            isEnabled = prefs[PrefsKeys.IS_ENABLED] ?: false,
            pageDwellMs = prefs[PrefsKeys.PAGE_DWELL_MS]?.toPageDwellList() ?: emptyList(),
            nearCueDistanceM = prefs[PrefsKeys.NEAR_CUE_DISTANCE_M] ?: DEFAULT_NEAR_CUE_M,
            postTurnDistanceM = prefs[PrefsKeys.POST_TURN_DISTANCE_M] ?: DEFAULT_POST_TURN_M,
            useSpeedSensitive = prefs[PrefsKeys.USE_SPEED_SENSITIVE] ?: false,
            nearCueSeconds = prefs[PrefsKeys.NEAR_CUE_SECONDS] ?: DEFAULT_NEAR_CUE_SECONDS,
            minNearCueDistanceM = prefs[PrefsKeys.MIN_NEAR_CUE_DISTANCE_M] ?: DEFAULT_MIN_NEAR_CUE_M
        )
    }

suspend fun Context.saveScrollEnabled(enabled: Boolean) {
    dataStore.edit { it[PrefsKeys.IS_ENABLED] = enabled }
}

suspend fun Context.saveScrollConfig(config: ScrollConfig) {
    dataStore.edit { prefs ->
        prefs[PrefsKeys.IS_ENABLED] = config.isEnabled
        prefs[PrefsKeys.PAGE_DWELL_MS] = config.pageDwellMs.toPageDwellString()
        prefs[PrefsKeys.NEAR_CUE_DISTANCE_M] = config.nearCueDistanceM
        prefs[PrefsKeys.POST_TURN_DISTANCE_M] = config.postTurnDistanceM
        prefs[PrefsKeys.USE_SPEED_SENSITIVE] = config.useSpeedSensitive
        prefs[PrefsKeys.NEAR_CUE_SECONDS] = config.nearCueSeconds
        prefs[PrefsKeys.MIN_NEAR_CUE_DISTANCE_M] = config.minNearCueDistanceM
    }
}
