package com.alphaxk.pubgtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != OverlayService.ACTION_CONFIG_CHANGED) return
        ConfigStore.init(context)
        val cfg = ConfigStore.load()
        cfg.espEnabled = intent.getBooleanExtra("esp", cfg.espEnabled)
        cfg.aimbotEnabled = intent.getBooleanExtra("aimbot", cfg.aimbotEnabled)
        cfg.aimFov = intent.getFloatExtra("fov", cfg.aimFov)
        cfg.aimSmoothing = intent.getFloatExtra("smoothing", cfg.aimSmoothing)
        cfg.bonePriority = intent.getIntExtra("bone", cfg.bonePriority)
        ConfigStore.save(cfg)
        Log.d("ConfigReceiver", "تم تحديث الإعدادات: $cfg")
    }
}
