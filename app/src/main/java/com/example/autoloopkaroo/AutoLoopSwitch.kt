package com.example.autoloopkaroo

import android.content.Context
import android.util.Log
import com.anpaiss.karooswitches.SwitchReceiver
import com.example.autoloopkaroo.data.saveScrollEnabled
import com.example.autoloopkaroo.data.scrollConfigFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * "Auto Loop" switch for the Switches popup (KarooSwitches library): the same
 * on/off as the toggle in the app and the bonus action. Declared in the manifest.
 */
class AutoLoopSwitch : SwitchReceiver() {

    override fun isOn(context: Context): Boolean =
        runBlocking { context.scrollConfigFlow().first().isEnabled }

    override fun setOn(context: Context, on: Boolean) {
        runBlocking { context.saveScrollEnabled(on) }
        Log.i("AutoLoop", "Switch: Auto Loop ${if (on) "ON" else "OFF"}")
    }
}
