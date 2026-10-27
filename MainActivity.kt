package com.alphaxk.pubgtool

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val overlayRequestCode = 1001
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigStore.init(this)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)

        findViewById<Button>(R.id.btn_grant).setOnClickListener {
            requestOverlayPermission()
        }
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (canDrawOverlays()) {
                startOverlayService()
                Toast.makeText(this, R.string.msg_service_started, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.msg_no_permission, Toast.LENGTH_LONG).show()
                requestOverlayPermission()
            }
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, R.string.msg_service_stopped, Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        statusText.text = getString(
            if (canDrawOverlays()) R.string.status_granted else R.string.status_denied
        )
    }

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(this)
        else true

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ),
                overlayRequestCode
            )
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
