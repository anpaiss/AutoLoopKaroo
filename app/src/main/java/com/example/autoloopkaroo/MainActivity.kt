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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoloopkaroo.data.MAX_PAGES
import com.example.autoloopkaroo.data.ScrollConfig
import com.example.autoloopkaroo.data.saveScrollConfig
import com.example.autoloopkaroo.data.saveScrollEnabled
import com.example.autoloopkaroo.data.scrollConfigFlow
import com.example.autoloopkaroo.ui.theme.AutoLoopKarooTheme
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val savedConfig by context.scrollConfigFlow().collectAsState(initial = ScrollConfig())

    val localPageDwells = remember(savedConfig) {
        mutableStateListOf(*Array(MAX_PAGES) { i ->
            (savedConfig.dwellForPage(i) / 1000f)
        })
    }
    var localNearCueM by remember(savedConfig) {
        mutableFloatStateOf(savedConfig.nearCueDistanceM)
    }
    var localPostTurnM by remember(savedConfig) {
        mutableFloatStateOf(savedConfig.postTurnDistanceM)
    }
    var localSoundEnabled by remember(savedConfig) {
        androidx.compose.runtime.mutableStateOf(savedConfig.soundEnabled)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.config_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.config_toggle_label),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = savedConfig.isEnabled,
                    onCheckedChange = { scope.launch { context.saveScrollEnabled(it) } }
                )
            }
            Text(
                text = stringResource(R.string.config_toggle_hint),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.config_sound_label),
                    fontSize = 16.sp
                )
                Switch(
                    checked = localSoundEnabled,
                    onCheckedChange = { localSoundEnabled = it }
                )
            }

            HorizontalDivider()
            Text(
                text = stringResource(R.string.config_dwell_title),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            for (i in 0 until MAX_PAGES) {
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
                    steps = 239,
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
                    steps = 239,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "${localPostTurnM.toInt()}${stringResource(R.string.config_meters_suffix)}")
            }

            Button(
                onClick = {
                    scope.launch {
                        context.saveScrollConfig(
                            ScrollConfig(
                                isEnabled = savedConfig.isEnabled,
                                pageDwellMs = localPageDwells.map { (it * 1000).toLong() },
                                nearCueDistanceM = localNearCueM,
                                postTurnDistanceM = localPostTurnM,
                                soundEnabled = localSoundEnabled
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(stringResource(R.string.config_save))
            }
        }
    }
}
