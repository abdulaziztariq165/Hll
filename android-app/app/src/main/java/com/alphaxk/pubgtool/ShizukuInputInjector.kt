package com.alphaxk.pubgtool

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuInputInjector {

    companion object {
        private const val TAG = "ShizukuInjector"
        private const val INJECT_MODE_ASYNC = 0
        private const val AIM_POINTER_ID = 19
        private const val AIM_DEVICE_ID = -1
    }

    private val inputManagerProxy: Any? by lazy { buildProxy() }
    private val injectMethod: java.lang.reflect.Method? by lazy {
        inputManagerProxy?.let { proxy ->
            proxy.javaClass.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
        }
    }

    private fun buildProxy(): Any? {
        return try {
            if (!Shizuku.pingBinder() ||
                Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Shizuku not running or permission not granted")
                return null
            }
            val binder = SystemServiceHelper.getSystemService("input") ?: return null
            val wrapped = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            stubClass.getDeclaredMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, wrapped)
        } catch (t: Throwable) {
            Log.e(TAG, "buildProxy failed: ${t.message}")
            null
        }
    }

    fun injectMove(dx: Float, dy: Float) {
        val method = injectMethod ?: return
        val proxy = inputManagerProxy ?: return

        val now = SystemClock.uptimeMillis()
        val props = MotionEvent.PointerProperties().apply {
            id = AIM_POINTER_ID
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coords = MotionEvent.PointerCoords().apply {
            x = dx
            y = dy
            pressure = 1f
            size = 1f
        }
        val event = MotionEvent.obtain(
            now, now,
            MotionEvent.ACTION_MOVE,
            1, arrayOf(props), arrayOf(coords),
            0, 0, 1f, 1f,
            AIM_DEVICE_ID, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        try {
            method.invoke(proxy, event, INJECT_MODE_ASYNC)
        } catch (t: Throwable) {
            Log.e(TAG, "injectMove failed: ${t.message}")
        } finally {
            event.recycle()
        }
    }
}
