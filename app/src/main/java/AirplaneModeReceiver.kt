package com.ny4rlk0.cellsignalnotifier
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

import android.widget.Toast
    class AirplaneModeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                val isAirplaneOn = intent.getBooleanExtra("state", false)
                if (isAirplaneOn) {
                    val message = """⚠️ Airplane Mode ⚠️""".trimIndent()

                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    //val mediaPlayer = MediaPlayer.create(context, R.raw.signal_lost)
                    //mediaPlayer.setOnCompletionListener { it.release() }
                    //mediaPlayer.start()
                }
            }
        }
    }

