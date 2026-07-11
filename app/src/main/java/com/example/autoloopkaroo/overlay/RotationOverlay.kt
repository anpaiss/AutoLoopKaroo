package com.example.autoloopkaroo.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import com.example.autoloopkaroo.R

private const val TAG = "RotationOverlay"

private const val BUTTON_SIZE_DP = 52
private const val BUTTON_BOTTOM_MARGIN_DP = 8
private const val BUTTON_BACKGROUND = 0xB3D74D01.toInt()

class RotationOverlay(
    private val context: Context,
    private val onTap: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var button: ImageView? = null
    private var lastState: Pair<Boolean, Boolean>? = null

    fun update(visible: Boolean, paused: Boolean) {
        val newState = visible to paused
        synchronized(this) {
            if (newState == lastState) return
            lastState = newState
        }
        handler.post {
            if (!visible) {
                removeButton()
            } else {
                if (button == null) addButton()
                button?.setImageResource(
                    if (paused) R.drawable.ic_overlay_play else R.drawable.ic_overlay_pause
                )
            }
        }
    }

    fun destroy() {
        synchronized(this) { lastState = false to false }
        handler.post { removeButton() }
    }

    private fun addButton() {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted, overlay disabled")
            return
        }
        val view = ImageView(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(BUTTON_BACKGROUND)
            }
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            setOnClickListener { onTap() }
        }
        val params = WindowManager.LayoutParams(
            dp(BUTTON_SIZE_DP),
            dp(BUTTON_SIZE_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(BUTTON_BOTTOM_MARGIN_DP)
        }
        try {
            windowManager.addView(view, params)
            button = view
        } catch (e: Exception) {
            Log.e(TAG, "addView failed", e)
        }
    }

    private fun removeButton() {
        button?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "removeView failed", e)
            }
        }
        button = null
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
