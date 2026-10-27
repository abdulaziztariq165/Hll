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
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val overlayRequestCode = 1001
    private val shizukuRequestCode = 1002
    private lateinit var statusText: TextView

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuRequestCode) {
                val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
                Toast.makeText(
                    this,
                    if (granted) "تم منح صلاحية Shizuku" else "تم رفض صلاحية Shizuku",
                    Toast.LENGTH_SHORT
                ).show()
                if (granted) startOverlayService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigStore.init(this)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        findViewById<Button>(R.id.btn_grant).setOnClickListener { requestOverlayPermission() }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!canDrawOverlays()) {
                Toast.makeText(this, R.string.msg_no_permission, Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return@setOnClickListener
            }
            requestShizukuPermission()
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, R.string.msg_service_stopped, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku غير مشغل — افتح تطبيق Shizuku أولاً", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "إصدار Shizuku قديم جداً", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startOverlayService()
            return
        }
        Shizuku.requestPermission(shizukuRequestCode)
    }

    private fun startOverlayService() {
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                overlayRequestCode
            )
        }
    }
}
