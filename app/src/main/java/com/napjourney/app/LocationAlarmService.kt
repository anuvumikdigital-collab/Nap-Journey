package com.napjourney.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlin.math.*

class LocationAlarmService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    private var targetLat = 0.0
    private var targetLon = 0.0
    private var radius = 1000
    private var ringtone: Ringtone? = null
    private var alarmTriggered = false
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START) {
            targetLat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
            targetLon = intent.getDoubleExtra(EXTRA_LON, 0.0)
            radius = intent.getIntExtra(EXTRA_RADIUS, 1000)
            alarmTriggered = false
            startForeground(NOTIFICATION_ID, buildNotification("Monitoring your journey"))
            beginLocationUpdates()
        }
        return START_STICKY
    }

    private fun beginLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, targetLat, targetLon, results)
                val distance = results[0]
                if (distance <= radius && !alarmTriggered) {
                    alarmTriggered = true
                    triggerAlarm()
                } else if (!alarmTriggered) {
                    val km = distance / 1000.0
                    val nm = String.format("%.1f km to alarm point", km)
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(nm))
                }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            fused.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun triggerAlarm() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
                .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 350, 700), 0))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 350, 700), 0))
            else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 700, 350, 700), 0)
        }
        val alarmIntent = Intent(this, AlarmActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        val pending = PendingIntent.getActivity(
            this, 44, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Nap-Journey: Destination nearby!")
            .setContentText("Wake up and get ready to get off the bus.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pending, true)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(ALARM_NOTIFICATION_ID, notification)
        try { startActivity(alarmIntent) } catch (_: Exception) { /* notification remains available */ }
    }

    private fun buildNotification(message: String): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Nap-Journey is active")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Journey monitoring", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            val alarmChannel = NotificationChannel(ALARM_CHANNEL_ID, "Destination alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(alarmChannel)
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        try { if (::locationCallback.isInitialized) fused.removeLocationUpdates(locationCallback) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.napjourney.START"
        const val ACTION_STOP = "com.napjourney.STOP"
        const val EXTRA_LAT = "target_lat"
        const val EXTRA_LON = "target_lon"
        const val EXTRA_RADIUS = "alarm_radius"
        const val CHANNEL_ID = "nap_journey_tracking"
        const val ALARM_CHANNEL_ID = "nap_journey_alarm"
        const val NOTIFICATION_ID = 3001
        const val ALARM_NOTIFICATION_ID = 3002
    }
}
