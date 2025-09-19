package com.ny4rlk0.cellsignalnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.util.Log

class SignalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.SERVICE_STATE" -> {
                val serviceState = intent.getParcelableExtra<ServiceState>("android.telephony.extra.SERVICE_STATE")
                Log.d("SignalReceiver", "ServiceState: ${serviceState?.state}")
                val isOutOfService = serviceState?.state == ServiceState.STATE_OUT_OF_SERVICE
                SignalLossTracker.handleSignalLoss(context, isOutOfService, """
                    ⚠️ SIGNAL DROP DETECTED ⚠️
                    The carrier network connection has been lost.
                    Your device is now off-grid—no calls, no texts, no data.
                    Please relocate to a signal-rich zone to reestablish contact with the Net.
                    Stay sharp, player. 🕶️
                """.trimIndent())
            }

            "android.intent.action.SIG_STR" -> {
                val signalStrength = intent.getParcelableExtra<SignalStrength>("android.telephony.extra.SIGNAL_STRENGTH")
                val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    signalStrength?.level ?: -1
                } else {
                    val rawGsm = signalStrength?.gsmSignalStrength ?: 99
                    when {
                        rawGsm <= 2 || rawGsm == 99 -> 0
                        rawGsm <= 8 -> 1
                        rawGsm <= 14 -> 2
                        rawGsm <= 20 -> 3
                        else -> 4
                    }
                }
                Log.d("SignalReceiver", "SignalStrength level: $level")
                SignalLossTracker.handleSignalLoss(context, level == 0, """
                    📶 SIGNAL BAR EMPTY 📶
                    Your device has zero signal strength.
                    You may still be registered to a network, but no usable signal is available.
                    Consider relocating or toggling airplane mode to force a reconnect.
                """.trimIndent())
            }
        }
    }
}
