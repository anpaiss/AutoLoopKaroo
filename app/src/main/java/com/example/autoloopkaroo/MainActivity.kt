package com.example.autoloopkaroo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoloopkaroo.data.AppSettings
import com.example.autoloopkaroo.data.DEFAULT_DWELL_MS
import com.example.autoloopkaroo.data.MAX_PAGES
import com.example.autoloopkaroo.data.ProfileEntry
import com.example.autoloopkaroo.data.ProfileSettings
import com.example.autoloopkaroo.data.appSettingsFlow
import com.example.autoloopkaroo.data.deleteProfile
import com.example.autoloopkaroo.data.saveProfileSettings
import com.example.autoloopkaroo.data.setSoundEnabled
import com.example.autoloopkaroo.ui.theme.AutoLoopKarooTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTOSAVE_DEBOUNCE_MS = 500L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoLoopKarooTheme {
                ConfigScreen(context = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(context: android.content.Context) {
    val appSettings by context.appSettingsFlow().collectAsState(initial = AppSettings())

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.config_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val profileList = remember(appSettings.profiles, appSettings.activeProfileId) {
                appSettings.profiles.values.sortedWith(
                    compareByDescending<ProfileEntry> { it.id == appSettings.activeProfileId }
                        .thenBy { it.name.lowercase() }
                )
            }

            if (profileList.isEmpty()) {
                EmptyProfilesView()
            } else {
                ProfilesEditor(
                    context = context,
                    profiles = profileList,
                    activeProfileId = appSettings.activeProfileId,
                    soundEnabled = appSettings.soundEnabled
                )
            }
        }
    }
}

@Composable
private fun EmptyProfilesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.config_no_profiles_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = stringResource(R.string.config_no_profiles_hint),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilesEditor(
    context: android.content.Context,
    profiles: List<ProfileEntry>,
    activeProfileId: String?,
    soundEnabled: Boolean
) {
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember(profiles.map { it.id }) {
        mutableStateOf(profiles.indexOfFirst { it.id == activeProfileId }.coerceAtLeast(0))
    }
    val selectedProfile = profiles[selectedTabIndex.coerceIn(0, profiles.lastIndex)]
    var pendingDelete by remember { mutableStateOf<ProfileEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            profiles.forEachIndexed { index, entry ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = if (entry.id == activeProfileId)
                                "${entry.name} ★"
                            else
                                entry.name
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileEditor(
                context = context,
                profile = selectedProfile,
                isActive = selectedProfile.id == activeProfileId,
                onRequestDelete = { pendingDelete = selectedProfile }
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.config_sound_label),
                    fontSize = 16.sp
                )
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { scope.launch { context.setSoundEnabled(it) } }
                )
            }
        }
    }

    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.config_delete_profile)) },
            text = { Text(stringResource(R.string.config_delete_profile_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { context.deleteProfile(target.id) }
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.config_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.config_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileEditor(
    context: android.content.Context,
    profile: ProfileEntry,
    isActive: Boolean,
    onRequestDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val initial = profile.settings
    val sliderCount = profile.pageCount.coerceIn(1, MAX_PAGES)

    val localPageDwells = remember(profile.id) {
        mutableStateListOf<Float>().apply {
            repeat(MAX_PAGES) { i ->
                add(initial.dwellForPage(i) / 1000f)
            }
        }
    }
    var localEnabled by remember(profile.id) { mutableStateOf(initial.isEnabled) }
    var localNearCueM by remember(profile.id) { mutableFloatStateOf(initial.nearCueDistanceM) }
    var localPostTurnM by remember(profile.id) { mutableFloatStateOf(initial.postTurnDistanceM) }
    var initialized by remember(profile.id) { mutableStateOf(false) }

    LaunchedEffect(profile.id) {
        repeat(MAX_PAGES) { i ->
            localPageDwells[i] = initial.dwellForPage(i) / 1000f
        }
        localEnabled = initial.isEnabled
        localNearCueM = initial.nearCueDistanceM
        localPostTurnM = initial.postTurnDistanceM
        initialized = true
    }

    LaunchedEffect(
        profile.id,
        localEnabled,
        localPageDwells.toList(),
        localNearCueM,
        localPostTurnM,
        initialized
    ) {
        if (!initialized) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        context.saveProfileSettings(
            profile.id,
            ProfileSettings(
                isEnabled = localEnabled,
                pageDwellMs = localPageDwells.map { (it * 1000).toLong() },
                nearCueDistanceM = localNearCueM,
                postTurnDistanceM = localPostTurnM
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = profile.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (isActive) {
                Text(
                    text = stringResource(R.string.config_active_profile_badge),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (!isActive) {
            TextButton(onClick = onRequestDelete) {
                Text(stringResource(R.string.config_delete_profile))
            }
        }
    }

    if (!profile.customized) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(R.string.config_using_defaults),
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.config_toggle_label),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = localEnabled,
            onCheckedChange = { localEnabled = it }
        )
    }
    Text(
        text = stringResource(R.string.config_toggle_hint),
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    HorizontalDivider()
    Text(
        text = stringResource(R.string.config_dwell_title),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
    for (i in 0 until sliderCount) {
        val dwellSec = localPageDwells[i]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.config_page_label, i + 1),
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Slider(
                value = dwellSec,
                onValueChange = { localPageDwells[i] = it },
                valueRange = 0f..60f,
                steps = 11,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (dwellSec == 0f)
                    stringResource(R.string.config_skip_label)
                else
                    "${dwellSec.toInt()}${stringResource(R.string.config_seconds_suffix)}",
                fontSize = 13.sp
            )
        }
    }

    HorizontalDivider()
    Text(
        text = stringResource(R.string.config_near_cue_distance_title),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = localNearCueM,
            onValueChange = { localNearCueM = it },
            valueRange = 10f..250f,
            steps = 47,
            modifier = Modifier.weight(1f)
        )
        Text(text = "${localNearCueM.toInt()}${stringResource(R.string.config_meters_suffix)}")
    }

    HorizontalDivider()
    Text(
        text = stringResource(R.string.config_post_turn_title),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = localPostTurnM,
            onValueChange = { localPostTurnM = it },
            valueRange = 10f..250f,
            steps = 47,
            modifier = Modifier.weight(1f)
        )
        Text(text = "${localPostTurnM.toInt()}${stringResource(R.string.config_meters_suffix)}")
    }
}
