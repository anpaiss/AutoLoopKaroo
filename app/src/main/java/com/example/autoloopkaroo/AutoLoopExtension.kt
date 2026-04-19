package com.example.autoloopkaroo

import android.util.Log
import com.example.autoloopkaroo.controller.AutoScrollController
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

private const val TAG = "AutoLoop"

class AutoLoopExtension : KarooExtension("autoloop-karoo", "1.0.2") {

    private lateinit var karooSystem: KarooSystemService
    private lateinit var controller: AutoScrollController

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Extension onCreate")

        // getTypes() is not open in base class — inject via reflection before AppStore reads it
        try {
            val field = KarooExtension::class.java.getDeclaredField("types")
            field.isAccessible = true
            field.set(this, listOf(AutoScrollDataType("autoloop-karoo", applicationContext)))
            Log.i(TAG, "DataType registered via reflection")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register DataType", e)
        }

        karooSystem = KarooSystemService(applicationContext)
        controller = AutoScrollController(applicationContext, karooSystem)
        karooSystem.connect {
            Log.i(TAG, "KarooSystem connected")
            controller.start()
        }
    }

    override fun onBonusAction(actionId: String) {
        Log.i(TAG, "onBonusAction: $actionId")
        if (actionId == "toggle_scroll") controller.toggle()
    }

    override fun onDestroy() {
        controller.stop()
        karooSystem.disconnect()
        super.onDestroy()
    }
}
