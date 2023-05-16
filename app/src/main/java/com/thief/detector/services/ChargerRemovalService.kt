package com.thief.detector.services;

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thief.detector.recivers.AlarmReceiver


class ChargerRemovalService : Service() {
    var charging: Boolean = false

    private var alarmReceiver = AlarmReceiver()

    private lateinit var batteryStatusReceiver: BroadcastReceiver

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()


        batteryStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val chargingStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                Log.i(
                    "ChargerStatus",
                    "onReceive: plugged=$plugged, chargingStatus=$chargingStatus"
                )
                if (plugged == 2)
                    charging = true

                if (plugged == 0 && charging && chargingStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    playAlarm()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground()
        registerBatteryStatusReceiver()
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
        unregisterBatteryStatusReceiver()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForeground() {
        val id = "chargerRemove"
        val chan = NotificationChannel(
            id,
            "Charger Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chan)

        val notification = NotificationCompat.Builder(this, id)
            .setContentTitle("Charger")
            .setContentText("running")
            .setOngoing(true) // set ongoing to make the notification non-removable
            .build()

        // set FLAG_FOREGROUND_SERVICE to make the notification non-removable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(3, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else
            startForeground(3, notification)

    }

    private fun registerBatteryStatusReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryStatusReceiver, intentFilter)
    }

    private fun unregisterBatteryStatusReceiver() {
        unregisterReceiver(batteryStatusReceiver)
        unregisterReceiver(alarmReceiver)

    }

    private fun playAlarm() {
        sendBroadcast(
            Intent(
                this,
                AlarmReceiver::class.java
            ).setAction(AlarmReceiver.CHARGER_REMOVE)
        )
        charging = false    }
}