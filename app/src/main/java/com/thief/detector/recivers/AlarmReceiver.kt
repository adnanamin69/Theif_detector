package com.thief.detector.recivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import com.thief.detector.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHARGER_REMOVE = "charger_remove"
        const val MOTION_DETECTED = "motion_detected"
        const val POCKET_REMOVE = "pocket_removed"
        const val STOP_ALARM = "stop"
        private var mediaPlayer: MediaPlayer? = null
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("INTENTACTION", "onReceive: ${intent?.action}")
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                mediaPlayer?.stop()
                mediaPlayer = null
            }
            STOP_ALARM -> {
                mediaPlayer?.stop()
                mediaPlayer = null
            }
            CHARGER_REMOVE -> {
                playAlarm(context, R.raw.charger_removed)
            }
            MOTION_DETECTED -> {
                playAlarm(context, R.raw.alarm_siren)
            }
            POCKET_REMOVE -> {
                playAlarm(context, R.raw.pocket_remove)
            }
        }


    }

    fun playAlarm(context: Context?, alarm: Int = R.raw.alarm_siren) {
        if (mediaPlayer == null)
        {
            mediaPlayer = MediaPlayer.create(context, alarm)
            mediaPlayer?.start()
        }
        else if (mediaPlayer?.isPlaying == false)
        {
            mediaPlayer?.start()
        }

        Log.i("INTENTACTION", "media player: ${mediaPlayer?.isPlaying}")

    }
}