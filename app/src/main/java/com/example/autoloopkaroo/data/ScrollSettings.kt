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

data class ScrollConfig(
    val isEnabled: Boolean = false,
    val pageDwellMs: List<Long> = emptyList(),
    val nearCueDistanceM: Float = DEFAULT_NEAR_CUE_M,
    val postTurnDistanceM: Float = DEFAULT_POST_TURN_M,
    val soundEnabled: Boolean = true
) {
    fun dwellForPage(index: Int): Long =
        pageDwellMs.getOrElse(index) { DEFAULT_DWELL_MS }
}

object PrefsKeys {
    val IS_ENABLED = booleanPreferencesKey("is_enabled")
    val PAGE_DWELL_MS = stringPreferencesKey("page_dwell_ms")
    val NEAR_CUE_DISTANCE_M = floatPreferencesKey("near_cue_distance_m")
    val POST_TURN_DISTANCE_M = floatPreferencesKey("post_turn_distance_m")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
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
            soundEnabled = prefs[PrefsKeys.SOUND_ENABLED] ?: true
        )
    }

suspend fun Context.saveScrollEnabled(enabled: Boolean) {
    dataStore.edit { it[PrefsKeys.IS_ENABLED] = enabled }
}

suspend fun Context.saveScrollSettings(
    pageDwellMs: List<Long>,
    nearCueDistanceM: Float,
    postTurnDistanceM: Float,
    soundEnabled: Boolean
) {
    dataStore.edit { prefs ->
        prefs[PrefsKeys.PAGE_DWELL_MS] = pageDwellMs.toPageDwellString()
        prefs[PrefsKeys.NEAR_CUE_DISTANCE_M] = nearCueDistanceM
        prefs[PrefsKeys.POST_TURN_DISTANCE_M] = postTurnDistanceM
        prefs[PrefsKeys.SOUND_ENABLED] = soundEnabled
    }
}
