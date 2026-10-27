package com.alphaxk.pubgtool

import android.content.Context
import android.content.SharedPreferences

data class ToolConfig(
    var espEnabled: Boolean = true,
    var aimbotEnabled: Boolean = false,
    var aimFov: Float = 6.0f,
    var aimSmoothing: Float = 0.85f,
    var aimSensitivity: Float = 0.35f,
    var bonePriority: Int = 1,
    var teamFilter: Int = -1,
    var maxDistance: Int = 40000,
    var menuAlpha: Float = 0.85f
)

object ConfigStore {
    private const val PREFS = "alphaxk_tool"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun load(): ToolConfig {
        val p = prefs ?: return ToolConfig()
        return ToolConfig(
            espEnabled = p.getBoolean("esp", true),
            aimbotEnabled = p.getBoolean("aimbot", false),
            aimFov = p.getFloat("fov", 6.0f),
            aimSmoothing = p.getFloat("smoothing", 0.85f),
            aimSensitivity = p.getFloat("sensitivity", 0.35f),
            bonePriority = p.getInt("bone", 1),
            teamFilter = p.getInt("team", -1),
            maxDistance = p.getInt("distance", 40000),
            menuAlpha = p.getFloat("alpha", 0.85f)
        )
    }

    fun save(cfg: ToolConfig) {
        prefs?.edit()
            ?.putBoolean("esp", cfg.espEnabled)
            ?.putBoolean("aimbot", cfg.aimbotEnabled)
            ?.putFloat("fov", cfg.aimFov)
            ?.putFloat("smoothing", cfg.aimSmoothing)
            ?.putFloat("sensitivity", cfg.aimSensitivity)
            ?.putInt("bone", cfg.bonePriority)
            ?.putInt("team", cfg.teamFilter)
            ?.putInt("distance", cfg.maxDistance)
            ?.putFloat("alpha", cfg.menuAlpha)
            ?.apply()
    }
}
