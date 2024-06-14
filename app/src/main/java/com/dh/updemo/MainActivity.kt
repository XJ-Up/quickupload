package com.dh.updemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPostNotificationsPermission()
        findViewById<Button>(R.id.singleFile).setOnClickListener {
            startActivity(Intent(this, SingleFileUploadActivity::class.java))
        }
        findViewById<Button>(R.id.singleMultiple).setOnClickListener {
            startActivity(Intent(this, MultipleSingleFileUploadsActivity::class.java))
        }
        findViewById<Button>(R.id.multipleSimultaneous).setOnClickListener {
            startActivity(Intent(this, UploadMultipleFilesSimultaneouslyActivity::class.java))
        }

    }

    private fun checkPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}