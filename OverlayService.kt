package com.alphaxk.pubgtool

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var config = ToolConfig()

    private var bubbleInitX = 0; private var bubbleInitY = 0
    private var bubbleTouchX = 0f; private var bubbleTouchY = 0f
    private var bubbleDragging = false

    private var menuInitX = 0; private var menuInitY = 0
    private var menuTouchX = 0f; private var menuTouchY = 0f
    private var menuDragging = false

    // Tabs containers
    private lateinit var layoutPlayers: View
    private lateinit var layoutLoot: View
    private lateinit var layoutVehicles: View
    private lateinit var layoutAimbot: View

    // Tab buttons
    private lateinit var tabPlayers: Button
    private lateinit var tabLoot: Button
    private lateinit var tabVehicles: Button
    private lateinit var tabAimbot: Button

    companion object {
        private const val CHANNEL_ID = "alphaxk_overlay"
        private const val NOTIF_ID = 6767
        private const val DRAG_THRESHOLD = 10
        private const val MENU_MIN_ALPHA = 0.30f
        private const val MENU_MAX_ALPHA = 1.00f
        const val ACTION_CONFIG_CHANGED = "com.alphaxk.pubgtool.CONFIG_CHANGED"
    }

    override fun onCreate() {
        super.onCreate()
        ConfigStore.init(this)
        config = ConfigStore.load()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        createBubble()
        createMenu()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        menuView?.let { runCatching { windowManager.removeView(it) } }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
    private fun overlayFlags(): Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private fun createBubble() {
        val bubble = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)
        bubbleView = bubble
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), overlayFlags(), PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 300 }

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { bubbleInitX = bubbleParams.x; bubbleInitY = bubbleParams.y; bubbleTouchX = event.rawX; bubbleTouchY = event.rawY; bubbleDragging = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - bubbleTouchX; val dy = event.rawY - bubbleTouchY
                    if (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD) {
                        bubbleDragging = true; bubbleParams.x = bubbleInitX + dx.toInt(); bubbleParams.y = bubbleInitY + dy.toInt()
                        windowManager.updateViewLayout(bubble, bubbleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!bubbleDragging) toggleMenu(); true }
                else -> false
            }
        }
        windowManager.addView(bubble, bubbleParams)
    }

    private fun toggleMenu() {
        menuView?.let { mv -> mv.visibility = if (mv.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
    }

    private fun createMenu() {
        val menu = LayoutInflater.from(this).inflate(R.layout.floating_menu, null)
        menuView = menu; menu.visibility = View.GONE

        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), overlayFlags(), PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 150; y = 350 }

        val dragHandle = menu.findViewById<View>(R.id.menu_drag_handle)
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { menuInitX = menuParams.x; menuInitY = menuParams.y; menuTouchX = event.rawX; menuTouchY = event.rawY; menuDragging = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - menuTouchX; val dy = event.rawY - menuTouchY
                    if (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD) {
                        menuDragging = true; menuParams.x = menuInitX + dx.toInt(); menuParams.y = menuInitY + dy.toInt()
                        windowManager.updateViewLayout(menu, menuParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> true; else -> false
            }
        }

        // -- Tabs Links --
        layoutPlayers = menu.findViewById(R.id.layout_players)
        layoutLoot = menu.findViewById(R.id.layout_loot)
        layoutVehicles = menu.findViewById(R.id.layout_vehicles)
        layoutAimbot = menu.findViewById(R.id.layout_aimbot)

        tabPlayers = menu.findViewById(R.id.tab_players)
        tabLoot = menu.findViewById(R.id.tab_loot)
        tabVehicles = menu.findViewById(R.id.tab_vehicles)
        tabAimbot = menu.findViewById(R.id.tab_aimbot)

        // Tabs Click Listeners
        tabPlayers.setOnClickListener { showTab(0) }
        tabLoot.setOnClickListener { showTab(1) }
        tabVehicles.setOnClickListener { showTab(2) }
        tabAimbot.setOnClickListener { showTab(3) }
        showTab(0) // Default

        // -- Alpha --
        val alphaSeek = menu.findViewById<SeekBar>(R.id.seek_alpha)
        val alphaLabel = menu.findViewById<TextView>(R.id.label_alpha)
        menu.alpha = config.menuAlpha
        alphaSeek.progress = (((config.menuAlpha - MENU_MIN_ALPHA) / (MENU_MAX_ALPHA - MENU_MIN_ALPHA)) * 100f).toInt().coerceIn(0, 100)
        alphaLabel.text = getString(R.string.label_opacity, (config.menuAlpha * 100).toInt())
        alphaSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val a = MENU_MIN_ALPHA + (progress / 100f) * (MENU_MAX_ALPHA - MENU_MIN_ALPHA)
                config.menuAlpha = a; menu.alpha = a; alphaLabel.text = getString(R.string.label_opacity, (a * 100).toInt())
                ConfigStore.save(config)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // ==========================================
        // 1. TAB ESP PLAYERS SETUP
        // ==========================================
        menu.findViewById<Switch>(R.id.switch_esp_players).apply {
            isChecked = config.espPlayersEnabled
            setOnCheckedChangeListener { _, c -> config.espPlayersEnabled = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_snapline_top).apply {
            isChecked = config.espSnaplineTop
            setOnCheckedChangeListener { _, c -> config.espSnaplineTop = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_snapline_bottom).apply {
            isChecked = config.espSnaplineBottom
            setOnCheckedChangeListener { _, c -> config.espSnaplineBottom = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_box).apply {
            isChecked = config.espBox
            setOnCheckedChangeListener { _, c -> config.espBox = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_name).apply {
            isChecked = config.espName
            setOnCheckedChangeListener { _, c -> config.espName = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_health).apply {
            isChecked = config.espHealth
            setOnCheckedChangeListener { _, c -> config.espHealth = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch>(R.id.switch_weapon).apply {
            isChecked = config.espWeapon
            setOnCheckedChangeListener { _, c -> config.espWeapon = c; ConfigStore.save(config); notifyConfigChanged() }
        }
        menu.findViewById<Switch
