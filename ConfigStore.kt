package com.alphaxk.pubgtool

import android.content.Context
import android.content.SharedPreferences

data class ToolConfig(
    // ESP Players
    var espPlayersEnabled: Boolean = false,
    var espSnaplineTop: Boolean = false,
    var espSnaplineBottom: Boolean = true,
    var espBox: Boolean = true,
    var espName: Boolean = true,
    var espHealth: Boolean = true,
    var espWeapon: Boolean = false,
    var espSkeleton: Boolean = false,
    var espDistance: Boolean = false,
    var maxDistance: Int = 300, // 1 to 300

    // ESP Loot
    var espLootEnabled: Boolean = false,
    var lootShowName: Boolean = true,
    var lootFontSize: Int = 14,
    var lootTextColor: Int = 0xFFFFFFFF.toInt(), // Default white

    // ESP Vehicles
    var espVehiclesEnabled: Boolean = false,

    // Aimbot
    var aimbotEnabled: Boolean = false,
    var aimbotCircle: Boolean = true,
    var aimFov: Float = 15.0f, // Circle size
    var aimBone: Int = 1, // 0: Stomach, 1: Chest, 2: Head
    var aimForce: Int = 20, // 1 to 100 (%)
    var aimIgnoreBots: Boolean = true,
    var aimTargetPriority: Int = 1, // 0: Any, 1: Closest, 2: Crosshair
    var aimCondition: Int = 0, // 0: Scope only, 1: Always

    // Menu
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
            espPlayersEnabled = p.getBoolean("esp_players", false),
            espSnaplineTop = p.getBoolean("esp_top", false),
            espSnaplineBottom = p.getBoolean("esp_bottom", true),
            espBox = p.getBoolean("esp_box", true),
            espName = p.getBoolean("esp_name", true),
            espHealth = p.getBoolean("esp_health", true),
            espWeapon = p.getBoolean("esp_weapon", false),
            espSkeleton = p.getBoolean("esp_skeleton", false),
            espDistance = p.getBoolean("esp_distance", false),
            maxDistance = p.getInt("esp_max_dist", 300),
            espLootEnabled = p.getBoolean("esp_loot", false),
            lootShowName = p.getBoolean("loot_name", true),
            lootFontSize = p.getInt("loot_size", 14),
            lootTextColor = p.getInt("loot_color", 0xFFFFFFFF.toInt()),
            espVehiclesEnabled = p.getBoolean("esp_vehicles", false),
            aimbotEnabled = p.getBoolean("aimbot", false),
            aimbotCircle = p.getBoolean("aim_circle", true),
            aimFov = p.getFloat("aim_fov", 15.0f),
            aimBone = p.getInt("aim_bone", 1),
            aimForce = p.getInt("aim_force", 20),
            aimIgnoreBots = p.getBoolean("aim_ignore_bots", true),
            aimTargetPriority = p.getInt("aim_priority", 1),
            aimCondition = p.getInt("aim_condition", 0),
            menuAlpha = p.getFloat("menu_alpha", 0.85f)
        )
    }

    fun save(cfg: ToolConfig) {
        prefs?.edit()
            ?.putBoolean("esp_players", cfg.espPlayersEnabled)
            ?.putBoolean("esp_top", cfg.espSnaplineTop)
            ?.putBoolean("esp_bottom", cfg.espSnaplineBottom)
            ?.putBoolean("esp_box", cfg.espBox)
            ?.putBoolean("esp_name", cfg.espName)
            ?.putBoolean("esp_health", cfg.espHealth)
            ?.putBoolean("esp_weapon", cfg.espWeapon)
            ?.putBoolean("esp_skeleton", cfg.espSkeleton)
            ?.putBoolean("esp_distance", cfg.espDistance)
            ?.putInt("esp_max_dist", cfg.maxDistance)
            ?.putBoolean("esp_loot", cfg.espLootEnabled)
            ?.putBoolean("loot_name", cfg.lootShowName)
            ?.putInt("loot_size", cfg.lootFontSize)
            ?.putInt("loot_color", cfg.lootTextColor)
            ?.putBoolean("esp_vehicles", cfg.espVehiclesEnabled)
            ?.putBoolean("aimbot", cfg.aimbotEnabled)
            ?.putBoolean("aim_circle", cfg.aimbotCircle)
            ?.putFloat("aim_fov", cfg.aimFov)
            ?.putInt("aim_bone", cfg.aimBone)
            ?.putInt("aim_force", cfg.aimForce)
            ?.putBoolean("aim_ignore_bots", cfg.aimIgnoreBots)
            ?.putInt("aim_priority", cfg.aimTargetPriority)
            ?.putInt("aim_condition", cfg.aimCondition)
            ?.putFloat("menu_alpha", cfg.menuAlpha)
            ?.apply()
    }
}
