package com.alphaxk.pubgtool

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.ln

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var config = ToolConfig()

    private var bubbleInitX = 0
    private var bubbleInitY = 0
    private var bubbleTouchX = 0f
    private var bubbleTouchY = 0f
    private var bubbleDragging = false

    private var menuInitX = 0
    private var menuInitY = 0
    private var menuTouchX = 0f
    private var menuTouchY = 0f
    private var menuDragging = false

    private var aimThread: Thread? = null
    private var aimRunning = false

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
        stopAimbotLoop()
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
                        bubbleDragging = true
                        bubbleParams.x = bubbleInitX + dx.toInt()
                        bubbleParams.y = bubbleInitY + dy.toInt()
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
        menuView = menu
        menu.visibility = View.GONE

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
                        menuDragging = true
                        menuParams.x = menuInitX + dx.toInt()
                        menuParams.y = menuInitY + dy.toInt()
                        windowManager.updateViewLayout(menu, menuParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        val alphaSeek = menu.findViewById<SeekBar>(R.id.seek_alpha)
        val alphaLabel = menu.findViewById<TextView>(R.id.label_alpha)
        menu.alpha = config.menuAlpha
        alphaSeek.progress = (((config.menuAlpha - MENU_MIN_ALPHA) / (MENU_MAX_ALPHA - MENU_MIN_ALPHA)) * 100f).toInt().coerceIn(0, 100)
        alphaLabel.text = getString(R.string.label_opacity, (config.menuAlpha * 100).toInt())
        alphaSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val a = MENU_MIN_ALPHA + (progress / 100f) * (MENU_MAX_ALPHA - MENU_MIN_ALPHA)
                config.menuAlpha = a; menu.alpha = a
                alphaLabel.text = getString(R.string.label_opacity, (a * 100).toInt())
                ConfigStore.save(config)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val espSwitch = menu.findViewById<Switch>(R.id.switch_esp)
        espSwitch.isChecked = config.espEnabled
        espSwitch.setOnCheckedChangeListener { _, c -> config.espEnabled = c; ConfigStore.save(config); notifyConfigChanged() }

        val aimSwitch = menu.findViewById<Switch>(R.id.switch_aimbot)
        aimSwitch.isChecked = config.aimbotEnabled
        aimSwitch.setOnCheckedChangeListener { _, c ->
            config.aimbotEnabled = c
            ConfigStore.save(config)
            notifyConfigChanged()
            if (c) startAimbotLoop() else stopAimbotLoop()
        }

        val fovSeek = menu.findViewById<SeekBar>(R.id.seek_fov)
        val fovLabel = menu.findViewById<TextView>(R.id.label_fov)
        fovSeek.progress = (((config.aimFov - 1f) / 19f) * 100f).toInt().coerceIn(0, 100)
        fovLabel.text = getString(R.string.label_fov, config.aimFov)
        fovSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val f = 1f + (progress / 100f) * 19f
                config.aimFov = f; fovLabel.text = getString(R.string.label_fov, f)
                ConfigStore.save(config); notifyConfigChanged()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val smoothSeek = menu.findViewById<SeekBar>(R.id.seek_smoothing)
        val smoothLabel = menu.findViewById<TextView>(R.id.label_smoothing)
        smoothSeek.progress = (config.aimSmoothing * 100f).toInt().coerceIn(0, 100)
        smoothLabel.text = getString(R.string.label_smoothing, (config.aimSmoothing * 100).toInt())
        smoothSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val s = progress / 100f
                config.aimSmoothing = s
                smoothLabel.text = getString(R.string.label_smoothing, (s * 100).toInt())
                ConfigStore.save(config); notifyConfigChanged()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val boneBtn = menu.findViewById<Button>(R.id.btn_bone)
        boneBtn.text = getString(if (config.bonePriority == 1) R.string.target_head else R.string.target_chest)
        boneBtn.setOnClickListener {
            config.bonePriority = if (config.bonePriority == 1) 0 else 1
            boneBtn.text = getString(if (config.bonePriority == 1) R.string.target_head else R.string.target_chest)
            ConfigStore.save(config); notifyConfigChanged()
        }

        menu.findViewById<Button>(R.id.btn_close).setOnClickListener { menu.visibility = View.GONE }

        windowManager.addView(menu, menuParams)
    }

    private fun startAimbotLoop() {
        if (aimRunning) return
        aimRunning = true
        val injector = ShizukuInputInjector()
        aimThread = Thread {
            while (aimRunning) {
                val dx = 0f // يحددها الـ Host Core لاحقاً
                val dy = 0f
                if (dx != 0f || dy != 0f) {
                    injector.injectMove(dx, dy)
                }
                Thread.sleep(16)
            }
        }.also { it.start() }
    }

    private fun stopAimbotLoop() {
        aimRunning = false
        aimThread?.join(1000)
        aimThread = null
    }

    private fun notifyConfigChanged() {
        val intent = Intent(ACTION_CONFIG_CHANGED).apply {
            setPackage(packageName)
            putExtra("esp", config.espEnabled)
            putExtra("aimbot", config.aimbotEnabled)
            putExtra("fov", config.aimFov)
            putExtra("smoothing", config.aimSmoothing)
            putExtra("bone", config.bonePriority)
        }
        sendBroadcast(intent)
    }
}
