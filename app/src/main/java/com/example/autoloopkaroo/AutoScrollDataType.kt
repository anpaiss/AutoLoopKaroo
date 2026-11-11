package com.example.autoloopkaroo

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.autoloopkaroo.data.scrollConfigFlow
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.anpaiss.karooswitches.R as KsR

/**
 * The old "AUTO" status field. It shows the same switch as the Switches field, so it is drawn
 * exactly like it (the library's layout and colours: grey caption, green ON / red OFF) instead
 * of a second style; kept because removing it would leave an empty cell on existing pages
 * (Andrea, 11/11/2026).
 */
class AutoScrollDataType(
    extension: String,
    private val appContext: Context
) : DataTypeImpl(extension, "auto_scroll_status") {

    private companion object {
        const val CAPTION_DP = 20f     // the caption row of ks_field
        const val STATE_FILL = 0.62f   // same rule as the Switches field: ON never down on the bottom border
        const val STATE_SP = 40f
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            appContext.scrollConfigFlow().collect { cfg ->
                val value: Double = if (cfg.isEnabled) 1.0 else 0.0
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId,
                            mapOf<String, Double>(DataType.Field.SINGLE to value),
                            null
                        )
                    )
                )
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val maxStateSp = ((config.viewSize.second / context.resources.displayMetrics.density - CAPTION_DP) * STATE_FILL).coerceAtLeast(12f)
        val job = CoroutineScope(Dispatchers.Main).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            appContext.scrollConfigFlow().collect { cfg ->
                val views = RemoteViews(context.packageName, KsR.layout.ks_field)
                views.setTextViewText(KsR.id.ks_caption, "AUTO LOOP")
                views.setTextViewText(KsR.id.ks_state, if (cfg.isEnabled) "ON" else "OFF")
                views.setTextViewTextSize(KsR.id.ks_state, TypedValue.COMPLEX_UNIT_SP, minOf(STATE_SP, maxStateSp))
                views.setTextColor(KsR.id.ks_state, context.getColor(if (cfg.isEnabled) KsR.color.ks_on else KsR.color.ks_off))
                emitter.updateView(views)
            }
        }
        emitter.setCancellable { job.cancel() }
    }
}
