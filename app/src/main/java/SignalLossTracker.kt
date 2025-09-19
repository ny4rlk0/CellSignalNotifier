package com.ny4rlk0.cellsignalnotifier

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.app.NotificationManager

object SignalLossTracker {
    private var isSignalLost = false
    private var pendingAlert = false
    private var handler: Handler? = null

    fun handleSignalLoss(context: Context, confirmedLoss: Boolean, message: String) {
        if (confirmedLoss) {
            if (!pendingAlert) {
                pendingAlert = true
                handler = Handler(Looper.getMainLooper())
                handler?.postDelayed({
                    if (isSignalLost) {
                        showSignalLostNotification(context, message)
                        playAlertSound(context)
                    }
                    pendingAlert = false
                }, 5000)
            }
            isSignalLost = true
        } else {
            isSignalLost = false
            pendingAlert = false
            handler?.removeCallbacksAndMessages(null)
        }
    }

    private fun showSignalLostNotification(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, "signal_channel")
            .setContentTitle("⚠️ Signal Alert")
            .setContentText("Signal lost. Tap for details.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    private fun playAlertSound(context: Context) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.signal_lost)
        mediaPlayer.setOnCompletionListener { it.release() }
        mediaPlayer.start()
    }
}
