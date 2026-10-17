package com.example.autoloopkaroo

import android.util.Log
import com.anpaiss.karooswitches.SwitchesDataType
import com.example.autoloopkaroo.controller.AutoScrollController
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

private const val TAG = "AutoLoop"

class AutoLoopExtension : KarooExtension("autoloop-karoo", "1.2.0") {

    private lateinit var karooSystem: KarooSystemService
    private lateinit var controller: AutoScrollController

    override val types by lazy {
        listOf(AutoScrollDataType("autoloop-karoo", applicationContext), SwitchesDataType("autoloop-karoo"))
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Extension onCreate")

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
