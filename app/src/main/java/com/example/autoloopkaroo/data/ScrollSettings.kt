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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autoloop_settings")

const val MAX_PAGES = 10
const val DEFAULT_DWELL_MS = 5_000L
const val DEFAULT_NEAR_CUE_M = 25f
const val DEFAULT_POST_TURN_M = 10f

@Serializable
data class ProfileSettings(
    val isEnabled: Boolean = false,
    val pageDwellMs: List<Long> = emptyList(),
    val nearCueDistanceM: Float = DEFAULT_NEAR_CUE_M,
    val postTurnDistanceM: Float = DEFAULT_POST_TURN_M
) {
    fun dwellForPage(index: Int): Long =
        pageDwellMs.getOrElse(index) { DEFAULT_DWELL_MS }
}

@Serializable
data class ProfileEntry(
    val id: String,
    val name: String,
    val pageCount: Int,
    val customized: Boolean,
    val settings: ProfileSettings
)

@Serializable
data class AppSettings(
    val defaultSettings: ProfileSettings = ProfileSettings(),
    val profiles: Map<String, ProfileEntry> = emptyMap(),
    val soundEnabled: Boolean = true,
    val activeProfileId: String? = null
) {
    fun settingsFor(profileId: String?): ProfileSettings {
        val entry = profileId?.let { profiles[it] }
        return if (entry != null && entry.customized) entry.settings else defaultSettings
    }
}

private val json = Json { ignoreUnknownKeys = true }

private object PrefsKeys {
    val APP_SETTINGS = stringPreferencesKey("app_settings_json")

    val LEGACY_IS_ENABLED = booleanPreferencesKey("is_enabled")
    val LEGACY_PAGE_DWELL_MS = stringPreferencesKey("page_dwell_ms")
    val LEGACY_NEAR_CUE_DISTANCE_M = floatPreferencesKey("near_cue_distance_m")
    val LEGACY_POST_TURN_DISTANCE_M = floatPreferencesKey("post_turn_distance_m")
    val LEGACY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
}

private fun String.toPageDwellList(): List<Long> =
    split(",").mapNotNull { it.trim().toLongOrNull() }

private fun Preferences.toAppSettings(): AppSettings {
    val raw = this[PrefsKeys.APP_SETTINGS]
    if (raw != null) {
        return runCatching { json.decodeFromString<AppSettings>(raw) }
            .getOrElse { AppSettings() }
    }
    val legacyDefaults = ProfileSettings(
        isEnabled = this[PrefsKeys.LEGACY_IS_ENABLED] ?: false,
        pageDwellMs = this[PrefsKeys.LEGACY_PAGE_DWELL_MS]?.toPageDwellList() ?: emptyList(),
        nearCueDistanceM = this[PrefsKeys.LEGACY_NEAR_CUE_DISTANCE_M] ?: DEFAULT_NEAR_CUE_M,
        postTurnDistanceM = this[PrefsKeys.LEGACY_POST_TURN_DISTANCE_M] ?: DEFAULT_POST_TURN_M
    )
    return AppSettings(
        defaultSettings = legacyDefaults,
        soundEnabled = this[PrefsKeys.LEGACY_SOUND_ENABLED] ?: true
    )
}

fun Context.appSettingsFlow(): Flow<AppSettings> =
    dataStore.data.map { it.toAppSettings() }

suspend fun Context.updateAppSettings(transform: (AppSettings) -> AppSettings) {
    dataStore.edit { prefs ->
        val current = prefs.toAppSettings()
        val next = transform(current)
        prefs[PrefsKeys.APP_SETTINGS] = json.encodeToString(AppSettings.serializer(), next)
        prefs.remove(PrefsKeys.LEGACY_IS_ENABLED)
        prefs.remove(PrefsKeys.LEGACY_PAGE_DWELL_MS)
        prefs.remove(PrefsKeys.LEGACY_NEAR_CUE_DISTANCE_M)
        prefs.remove(PrefsKeys.LEGACY_POST_TURN_DISTANCE_M)
        prefs.remove(PrefsKeys.LEGACY_SOUND_ENABLED)
    }
}

suspend fun Context.observeProfile(id: String, name: String, pageCount: Int) {
    updateAppSettings { current ->
        val existing = current.profiles[id]
        val updatedEntry = when {
            existing == null -> ProfileEntry(
                id = id,
                name = name,
                pageCount = pageCount,
                customized = false,
                settings = current.defaultSettings.copy(isEnabled = false)
            )
            existing.name != name || existing.pageCount != pageCount ->
                existing.copy(name = name, pageCount = pageCount)
            else -> existing
        }
        current.copy(
            profiles = current.profiles + (id to updatedEntry),
            activeProfileId = id
        )
    }
}

suspend fun Context.setProfileEnabled(id: String, enabled: Boolean) {
    updateAppSettings { current ->
        val existing = current.profiles[id] ?: return@updateAppSettings current
        val nextSettings = existing.settings.copy(isEnabled = enabled)
        current.copy(
            profiles = current.profiles + (id to existing.copy(
                customized = true,
                settings = nextSettings
            ))
        )
    }
}

suspend fun Context.saveProfileSettings(id: String, settings: ProfileSettings) {
    updateAppSettings { current ->
        val existing = current.profiles[id] ?: return@updateAppSettings current
        current.copy(
            profiles = current.profiles + (id to existing.copy(
                customized = true,
                settings = settings
            ))
        )
    }
}

suspend fun Context.deleteProfile(id: String) {
    updateAppSettings { current ->
        if (current.activeProfileId == id) return@updateAppSettings current
        current.copy(profiles = current.profiles - id)
    }
}

suspend fun Context.setSoundEnabled(enabled: Boolean) {
    updateAppSettings { it.copy(soundEnabled = enabled) }
}
