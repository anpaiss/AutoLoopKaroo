package com.example.autoloopkaroo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.ScrollView
import android.widget.TextView
import com.anpaiss.karooswitches.SwitchesDataType
import io.hammerhead.karooext.aidl.IHandler
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ViewConfig

/**
 * FIELD PREVIEW, the same screen as in BetterETA, boschRange and BoschData: the
 * two fields drawn by their own startView at real size in the cells the Karoo
 * hands out, down to the smallest (a data page holds at most ten fields, five
 * rows of two, 238x126 px each), on the dark and on the light theme. The fields
 * show the real state of the Auto Loop switch.
 *
 * Each cell gets a real data type and a ViewEmitter whose handler applies the
 * RemoteViews here instead of sending them to the Karoo.
 */
class FieldPreviewActivity : Activity() {

    /** A cell as the Karoo describes it in ViewConfig; sizes read from the VIEW log lines of the k24. */
    private class Cell(val w: Int, val h: Int, val grid: Pair<Int, Int>, val textSp: Int)

    private companion object {
        val HALF = Cell(238, 126, 30 to 12, 41)
        val ROW  = Cell(478, 126, 60 to 12, 54)
    }

    private val emitters = ArrayList<ViewEmitter>()
    private lateinit var list: LinearLayout
    private var dark = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(ScrollView(this).apply { addView(list) })
    }

    override fun onStart() {
        super.onStart()
        rebuild()
    }

    override fun onStop() {
        stopViews()
        super.onStop()
    }

    private fun stopViews() {
        emitters.forEach { it.cancel() }
        emitters.clear()
    }

    private fun rebuild() {
        stopViews()
        list.removeAllViews()
        val page = if (dark) Color.BLACK else Color.WHITE
        val ink  = if (dark) Color.WHITE else Color.BLACK
        list.setBackgroundColor(page)

        list.addView(Button(this).apply {
            text = if (dark) "THEME: DARK" else "THEME: LIGHT"
            setOnClickListener { dark = !dark; rebuild() }
        })
        fun caption(text: String) = list.addView(TextView(this).apply {
            this.text = text; textSize = 11f; setTextColor(ink); alpha = 0.6f
            setPadding(8, 16, 8, 6)
        })

        val fields: List<DataTypeImpl> = listOf(
            AutoScrollDataType("preview", applicationContext),
            SwitchesDataType("preview"),
        )
        // The same context the fields get on the Karoo, with the theme forced
        val ctx = applicationContext.createConfigurationContext(Configuration(resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        })
        // Cells sit on a grey sheet two pixels apart, like the Karoo's own cell borders
        fun row(cell: Cell, types: List<DataTypeImpl>) = list.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.GRAY)
            setPadding(0, 1, 0, 1)
            for (type in types) addView(FrameLayout(context).apply {
                setBackgroundColor(page)
                start(ctx, type, cell, this)
            }, LinearLayout.LayoutParams(cell.w, cell.h).apply { setMargins(1, 0, 1, 0) })
        })

        caption("REAL SIZE · HALF CELLS ${HALF.w}×${HALF.h} (5 ROWS OF 2, THE SMALLEST)")
        row(HALF, fields)
        caption("FULL-WIDTH ROWS ${ROW.w}×${ROW.h}")
        fields.forEach { row(ROW, listOf(it)) }
    }

    /** Runs the field's own startView and shows what it emits inside [host]. */
    private fun start(ctx: Context, type: DataTypeImpl, cell: Cell, host: FrameLayout) {
        val handler = object : IHandler.Stub() {
            override fun onNext(bundle: Bundle) {
                // Anything else is a ViewEvent for the Karoo (header on/off): nothing to draw
                @Suppress("DEPRECATION")
                val views = bundle.getParcelable<RemoteViews>("view") ?: return
                runOnUiThread {
                    host.removeAllViews()
                    host.addView(views.apply(ctx, host), FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }
            override fun onError(msg: String?) {}
            override fun onComplete() {}
        }
        val emitter = ViewEmitter(packageName, handler)
        emitters += emitter
        // preview = true: the Switches field leaves out its tap, which here would open the popup
        type.startView(ctx, ViewConfig(cell.grid, cell.w to cell.h, cell.textSp, ViewConfig.Alignment.CENTER, preview = true), emitter)
    }
}
