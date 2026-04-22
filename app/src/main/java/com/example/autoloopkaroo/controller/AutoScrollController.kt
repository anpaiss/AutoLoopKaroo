package com.example.autoloopkaroo.controller

import android.content.Context
import android.util.Log
import com.example.autoloopkaroo.R
import com.example.autoloopkaroo.data.ScrollConfig
import com.example.autoloopkaroo.data.saveScrollEnabled
import com.example.autoloopkaroo.data.scrollConfigFlow
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.ActiveRidePage
import io.hammerhead.karooext.models.ActiveRideProfile
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.PerformHardwareAction
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideProfile
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.ShowMapPage
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

private const val TAG = "AutoScrollController"

enum class ScrollState { INACTIVE, SCROLLING, NEAR_CUE, POST_TURN }

class AutoScrollController(
    private val context: Context,
    private val karooSystem: KarooSystemService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile private var scrollState = ScrollState.INACTIVE
    @Volatile private var stateBeforeCue = ScrollState.INACTIVE
    @Volatile private var rideActive = false

    private var currentPageIndex = 0
    private var pages: List<RideProfile.Page> = emptyList()
    private var config = ScrollConfig()

    private var scrollJob: Job? = null
    @Volatile private var started = false

    private var lastDistanceToTurn = Double.MAX_VALUE
    private var odometerAtTurn = 0.0
    private var currentOdometer = 0.0
    private var currentSpeedMs = 0f
    private var nearCueJob: Job? = null

    private val consumerIds = mutableListOf<String>()
    private var navConsumerIds = mutableListOf<String>()

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true

        scope.launch {
            context.scrollConfigFlow().debounce(300L).collect { cfg ->
                val enabledChanged = cfg.isEnabled != config.isEnabled
                config = cfg
                if (enabledChanged) {
                    if (rideActive) {
                        if (cfg.isEnabled && scrollState == ScrollState.INACTIVE) {
                            enterScrolling()
                        } else if (!cfg.isEnabled) {
                            enterInactive()
                        }
                    } else {
                        notifyToggle(cfg.isEnabled)
                    }
                }
            }
        }

        consumerIds += karooSystem.addConsumer { state: RideState ->
            when (state) {
                RideState.Recording -> {
                    rideActive = true
                    subscribeToNavigationStreams()
                    if (config.isEnabled && scrollState == ScrollState.INACTIVE) {
                        scope.launch { enterScrolling() }
                    }
                }
                else -> {
                    rideActive = false
                    pauseNavigationStreams()
                    if (scrollState != ScrollState.INACTIVE) {
                        scrollJob?.cancel()
                        nearCueJob?.cancel()
                        scrollState = ScrollState.INACTIVE
                        stateBeforeCue = ScrollState.INACTIVE
                        scope.launch { notifyToggle(false) }
                    }
                }
            }
        }

        consumerIds += karooSystem.addConsumer { event: ActiveRideProfile ->
            pages = event.profile.pages.take(10)
        }

        consumerIds += karooSystem.addConsumer { event: ActiveRidePage ->
            val idx = pages.indexOf(event.page)
            if (idx >= 0) currentPageIndex = idx
        }
    }

    private fun subscribeToNavigationStreams() {
        if (navConsumerIds.isNotEmpty()) return

        navConsumerIds += karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SPEED)
        ) { event: OnStreamState ->
            val state = event.state
            if (state is StreamState.Streaming) {
                val speedMs = state.dataPoint.values[DataType.Field.SPEED] ?: return@addConsumer
                currentSpeedMs = speedMs.toFloat()
            }
        }

        navConsumerIds += karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.DISTANCE_TO_NEXT_TURN)
        ) { event: OnStreamState ->
            val state = event.state
            if (state is StreamState.Streaming) {
                val dist = state.dataPoint.values[DataType.Field.DISTANCE_TO_NEXT_TURN]
                    ?: return@addConsumer
                onDistanceToTurn(dist)
            }
        }

        navConsumerIds += karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.DISTANCE)
        ) { event: OnStreamState ->
            val state = event.state
            if (state is StreamState.Streaming) {
                val dist = state.dataPoint.values[DataType.Field.DISTANCE]
                    ?: return@addConsumer
                currentOdometer = dist
                if (scrollState == ScrollState.POST_TURN) {
                    val traveled = currentOdometer - odometerAtTurn
                    if (traveled >= config.postTurnDistanceM) onPostTurnComplete()
                }
            }
        }
    }

    private fun pauseNavigationStreams() {
        navConsumerIds.forEach { karooSystem.removeConsumer(it) }
        navConsumerIds.clear()
        lastDistanceToTurn = Double.MAX_VALUE
        currentSpeedMs = 0f
    }

    private fun onDistanceToTurn(dist: Double) {
        val threshold = config.effectiveNearCueDistanceM(currentSpeedMs).toDouble()
        when (scrollState) {
            ScrollState.SCROLLING -> {
                if (dist < threshold) {
                    Log.d(TAG, "Near cue at ${dist}m (threshold=${threshold}m) → map")
                    stateBeforeCue = ScrollState.SCROLLING
                    scrollState = ScrollState.NEAR_CUE
                    scrollJob?.cancel()
                    startNearCueTimeout()
                    try {
                        karooSystem.dispatch(ShowMapPage(zoom = false))
                    } catch (e: Exception) {
                        Log.e(TAG, "dispatch ShowMapPage failed", e)
                    }
                }
            }
            ScrollState.INACTIVE -> {
                if (dist < threshold) {
                    stateBeforeCue = ScrollState.INACTIVE
                    scrollState = ScrollState.NEAR_CUE
                    startNearCueTimeout()
                    try {
                        karooSystem.dispatch(ShowMapPage(zoom = false))
                    } catch (e: Exception) {
                        Log.e(TAG, "dispatch ShowMapPage failed", e)
                    }
                }
            }
            ScrollState.NEAR_CUE -> {
                if (dist > threshold && lastDistanceToTurn < threshold) {
                    Log.d(TAG, "Turn completed → POST_TURN")
                    nearCueJob?.cancel()
                    odometerAtTurn = currentOdometer
                    scrollState = ScrollState.POST_TURN
                }
            }
            ScrollState.POST_TURN -> {}
        }
        lastDistanceToTurn = dist
    }

    private fun startNearCueTimeout() {
        nearCueJob?.cancel()
        nearCueJob = scope.launch {
            delay(60_000L)
            if (isActive && scrollState == ScrollState.NEAR_CUE) {
                Log.w(TAG, "NEAR_CUE timeout → forcing POST_TURN")
                odometerAtTurn = currentOdometer
                scrollState = ScrollState.POST_TURN
            }
        }
    }

    private fun onPostTurnComplete() {
        Log.d(TAG, "Post-turn distance reached → back to $stateBeforeCue")
        when (stateBeforeCue) {
            ScrollState.SCROLLING -> enterScrolling()
            else -> scrollState = ScrollState.INACTIVE
        }
    }

    fun toggle() {
        scope.launch {
            val newEnabled = !config.isEnabled
            context.saveScrollEnabled(newEnabled)
        }
    }

    private fun enterScrolling() {
        scrollState = ScrollState.SCROLLING
        notifyToggle(true)
        startScrollLoop()
    }

    private fun enterInactive() {
        scrollState = ScrollState.INACTIVE
        scrollJob?.cancel()
        notifyToggle(false)
    }

    private fun startScrollLoop() {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            while (true) {
                val dwell = config.dwellForPage(currentPageIndex)
                if (dwell == 0L) {
                    delay(300L)
                } else {
                    delay(dwell.coerceAtLeast(1000L))
                }
                if (scrollState == ScrollState.SCROLLING && pages.size > 1) {
                    try {
                        karooSystem.dispatch(PerformHardwareAction.TopRightPress)
                    } catch (e: Exception) {
                        Log.e(TAG, "dispatch TopRightPress failed", e)
                    }
                }
            }
        }
    }

    private fun notifyToggle(enabled: Boolean) {
        val tones: List<PlayBeepPattern.Tone> = if (enabled) {
            listOf(
                PlayBeepPattern.Tone(frequency = 1000, durationMs = 80),
                PlayBeepPattern.Tone(frequency = null, durationMs = 40),
                PlayBeepPattern.Tone(frequency = 1500, durationMs = 120)
            )
        } else {
            listOf(PlayBeepPattern.Tone(frequency = 800, durationMs = 200))
        }
        try {
            karooSystem.dispatch(
                InRideAlert(
                    id = "autoloop_toggle_${System.currentTimeMillis()}",
                    icon = R.drawable.ic_autoloop,
                    title = context.getString(if (enabled) R.string.alert_scroll_on else R.string.alert_scroll_off),
                    detail = null,
                    autoDismissMs = 2_000L,
                    backgroundColor = if (enabled) R.color.alert_on_background else R.color.alert_off_background,
                    textColor = R.color.alert_text
                )
            )
            karooSystem.dispatch(PlayBeepPattern(tones = tones))
        } catch (e: Exception) {
            Log.e(TAG, "dispatch notify failed", e)
        }
    }

    fun stop() {
        scrollJob?.cancel()
        nearCueJob?.cancel()
        navConsumerIds.forEach { karooSystem.removeConsumer(it) }
        navConsumerIds.clear()
        consumerIds.forEach { karooSystem.removeConsumer(it) }
        consumerIds.clear()
        scope.cancel()
    }
}
