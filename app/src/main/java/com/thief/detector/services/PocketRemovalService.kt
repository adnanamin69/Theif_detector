package com.thief.detector.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thief.detector.recivers.AlarmReceiver

class PocketRemovalService : Service() {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var alarmReceiver = AlarmReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground()
        registerSensorListener()
        registerScreenOnReceiver()
        return START_STICKY
    }

    private fun registerScreenOnReceiver() {
        alarmReceiver = AlarmReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(alarmReceiver, intentFilter)
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterSensorListener()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForeground() {
        val id = "pocketRemoval"
        val chan = NotificationChannel(
            id,
            "MPocket Removal Detection Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chan)

        val notification = NotificationCompat.Builder(this, id)
            .setContentTitle("Pocket Removal Detection Service")
            .setContentText("Service is running in the background")
            .setOngoing(true) // set ongoing to make the notification non-removable
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else
            startForeground(1, notification)

    }


    private fun registerSensorListener() {
        proximitySensor?.let {
            sensorManager.registerListener(
                proximitySensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(proximitySensorEventListener)
        unregisterReceiver(alarmReceiver)

    }

    private val proximitySensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_PROXIMITY) {
                    Log.i("PocketSensor", "onSensorChanged: ${it.values[0]}")
                    if (it.values[0] == 5f) {
                        playAlarm()
                    }
                }
            }
        }
    }

    private fun playAlarm() {
        sendBroadcast(
            Intent(
                this,
                AlarmReceiver::class.java
            ).setAction(AlarmReceiver.POCKET_REMOVE)
        )
    }
}
