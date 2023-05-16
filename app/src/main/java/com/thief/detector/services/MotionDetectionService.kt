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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thief.detector.recivers.AlarmReceiver
import kotlin.math.sqrt


class MotionDetectionService : Service() {

    private lateinit var alarmReceiver: AlarmReceiver
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground()
        registerSensorListener()
        registerScreenOnReceiver()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensorListener()
    }

    private fun registerScreenOnReceiver() {
        alarmReceiver = AlarmReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(alarmReceiver, intentFilter)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForeground() {
        val id = "motionDetector"
        val chan = NotificationChannel(
            id,
            "Motion Detection Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chan)

        val notification = NotificationCompat.Builder(this, id)
            .setContentTitle("Motion Detection Service")
            .setContentText("Service is running in the background")
            .setOngoing(true) // set ongoing to make the notification non-removable
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else
            startForeground(2, notification)

    }


    private fun registerSensorListener() {
        accelerometerSensor?.let {
            sensorManager.registerListener(
                accelerometerSensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(accelerometerSensorEventListener)
        unregisterReceiver(alarmReceiver)

    }

    private val accelerometerSensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val acceleration = sqrt(x * x + y * y + z * z)
                    if (acceleration > 15) {
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
            ).setAction(AlarmReceiver.MOTION_DETECTED)
        )
    }
}