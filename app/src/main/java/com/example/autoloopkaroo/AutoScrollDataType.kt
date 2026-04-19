package com.example.autoloopkaroo

import android.content.Context
import android.widget.RemoteViews
import com.example.autoloopkaroo.data.scrollConfigFlow
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoScrollDataType(
    extension: String,
    private val appContext: Context
) : DataTypeImpl(extension, "auto_scroll_status") {

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
        val job = CoroutineScope(Dispatchers.Main).launch {
            appContext.scrollConfigFlow().collect { cfg ->
                val views = RemoteViews(context.packageName, R.layout.datatype_autoscroll)
                views.setTextViewText(R.id.tv_value, if (cfg.isEnabled) "ON" else "OFF")
                emitter.updateView(views)
            }
        }
        emitter.setCancellable { job.cancel() }
    }
}
