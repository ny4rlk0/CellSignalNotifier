package com.ny4rlk0.cellsignalnotifier

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.*
import android.telephony.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
/*
* Alloy female 1 https://ttsmp3.com/ai
* SIGNAL DROP DETECTED!
The carrier network connection has been lost.
Your device is now off-grid,no calls, no texts, no data.
Please relocate to a signal-rich zone to reestablish contact with the Carrier Provider.
We appreciate your understanding during this blackout. Stay sharp, player.
* */
class SignalMonitorService : Service() {

    private lateinit var airplaneReceiver: AirplaneModeReceiver
    private lateinit var signalReceiver: BroadcastReceiver
    private lateinit var telephonyManager: TelephonyManager
    private var signalLossHandler: Handler? = null
    private var signalLossRunnable: Runnable? = null
    private var isSignalLost = false

    override fun onCreate() {
        super.onCreate()
        Log.d("SignalMonitorService", "Service created")

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        signalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("SignalMonitorService", "Broadcast received: ${intent.action}")
                if (intent.action == "android.intent.action.SERVICE_STATE") {
                    val serviceState = intent.getParcelableExtra<ServiceState>("android.telephony.extra.SERVICE_STATE")
                    if (serviceState?.state == ServiceState.STATE_OUT_OF_SERVICE) {
                        triggerSignalLostAlert("⚠️ Signal lost due to service state!")
                    }
                }
            }
        }

        val filter = IntentFilter("android.intent.action.SERVICE_STATE")
        registerReceiver(signalReceiver, filter)
    }

    private val signalListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)

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

            Log.d("SignalMonitorService", "SignalStrength level: $level")

            if (level == 0 && !isSignalLost) {
                isSignalLost = true
                signalLossHandler = Handler(Looper.getMainLooper())
                signalLossRunnable = Runnable {
                    triggerSignalLostAlert("📶 Signal bar empty for 5 seconds!")
                }
                signalLossHandler?.postDelayed(signalLossRunnable!!, 5000)
            } else if (level > 0 && isSignalLost) {
                isSignalLost = false
                signalLossHandler?.removeCallbacks(signalLossRunnable!!)
            }
        }
    }

    private fun triggerSignalLostAlert(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.signal_lost)
        mediaPlayer.setOnCompletionListener { it.release() }
        mediaPlayer.start()

        val notification = NotificationCompat.Builder(this, "signal_channel")
            .setContentTitle("⚠️ Signal Alert ⚠️")
            .setContentText("Carrier signal lost.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    override fun onDestroy() {
        unregisterReceiver(airplaneReceiver)
        unregisterReceiver(signalReceiver)
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE)
        signalLossHandler?.removeCallbacks(signalLossRunnable!!)
        Log.d("SignalMonitorService", "Service destroyed")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()

        try {
            startForeground(1, notification)
            Log.d("SignalMonitorService", "Foreground started")
        } catch (e: Exception) {
            Log.e("SignalMonitorService", "startForeground failed: ${e.message}")
            stopSelf()
        }

        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        airplaneReceiver = AirplaneModeReceiver()
        registerReceiver(airplaneReceiver, filter)
        Log.d("SignalMonitorService", "AirplaneModeReceiver registered")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "signal_channel",
                "Signal Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(null, null)
                setShowBadge(false)
                setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d("SignalMonitorService", "Notification channel created")
        }
    }

    private fun buildNotification(): Notification {
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.no_signal)
        Log.d("SignalMonitorService", "Building notification")
        return NotificationCompat.Builder(this, "signal_channel")
            .setContentTitle("Signal Monitor Active")
            .setContentText("Watching for signal drops...")
            .setSmallIcon(R.drawable.white_no_signal)
            //.setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
