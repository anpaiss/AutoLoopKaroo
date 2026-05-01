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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AutoScrollDataType(
    extension: String,
    private val appContext: Context
) : DataTypeImpl(extension, "auto_scroll_status") {

    private val streamScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = streamScope.launch {
            appContext.scrollConfigFlow()
                .map { it.isEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    val value: Double = if (enabled) 1.0 else 0.0
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
        val job = viewScope.launch {
            appContext.scrollConfigFlow()
                .map { it.isEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    val views = RemoteViews(context.packageName, R.layout.datatype_autoscroll)
                    views.setTextViewText(R.id.tv_value, if (enabled) "ON" else "OFF")
                    emitter.updateView(views)
                }
        }
        emitter.setCancellable { job.cancel() }
    }
}
