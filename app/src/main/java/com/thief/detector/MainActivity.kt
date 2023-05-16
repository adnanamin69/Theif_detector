package com.thief.detector

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.thief.detector.databinding.ActivityMainBinding
import com.thief.detector.services.ChargerRemovalService
import com.thief.detector.services.MotionDetectionService
import com.thief.detector.services.PocketRemovalService


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var motion = false
    private var pocket = false
    private var charge = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            askNotificationPermission()
        else
            setupButtons()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
               setupButtons()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                Snackbar.make(
                    binding.root,
                    "Notification blocked",
                    Snackbar.LENGTH_LONG
                ).setAction("Settings") {
                    // Responds to click on the action
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }.show()
            }
            else -> {
                // The registered ActivityResultCallback gets the result of this request
                requestPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        motion = isServiceRunning(MotionDetectionService::class.java)
        pocket = isServiceRunning(PocketRemovalService::class.java)
        charge = isServiceRunning(ChargerRemovalService::class.java)
        updateStatus()
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupButtons()
        } else {
            Toast.makeText(this, "Notification Permission is deny", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupButtons() {
        binding.pocketRemovalButton.setOnClickListener {
            toggleService(PocketRemovalService::class.java)
            updateStatus()
        }

        binding.chargerRemovalButton.setOnClickListener {
            toggleService(ChargerRemovalService::class.java)
            updateStatus()
        }

        binding.motionDetectionButton.setOnClickListener {
            toggleService(MotionDetectionService::class.java)
            updateStatus()
        }
    }

    private fun toggleService(serviceClass: Class<*>) {
        val isRunning = isServiceRunning(serviceClass)

        if (isRunning)
            stopService(Intent(this, serviceClass))
        else
            startService(Intent(this, serviceClass))

        when (serviceClass) {
            PocketRemovalService::class.java -> pocket = !isRunning
            ChargerRemovalService::class.java -> charge = !isRunning
            MotionDetectionService::class.java -> motion = !isRunning
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatus() {
        binding.motionDetectionStatusTextView.text =
            getString(R.string.motion_detection_status) + getStatusText(motion)

        binding.pocketRemovalStatusTextView.text =
            getString(R.string.pocket_removal_detection_status) + getStatusText(pocket)

        binding.chargerRemovalStatusTextView.text =
            getString(R.string.charger_removal_detection_status) + getStatusText(charge)
    }

    private fun getStatusText(isRunning: Boolean): String {
        return if (isRunning) {
            getString(R.string.running)
        } else {
            getString(R.string.not_running)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}