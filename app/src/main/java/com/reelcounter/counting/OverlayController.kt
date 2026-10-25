package com.reelcounter.counting

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.reelcounter.R

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun show(count: Int) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Today: $count reels", Toast.LENGTH_SHORT).show()
            return
        }
        val label = view ?: TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.overlay_background))
                cornerRadius = 28f
            }
            setPadding(18, 10, 18, 10)
            textSize = 13f
            minWidth = 64
            gravity = Gravity.CENTER
            setOnTouchListener(DragListener())
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    x = 12
                    y = 120
                }
                windowManager.addView(this, params)
                layoutParams = params
                view = this
            } catch (error: WindowManager.BadTokenException) {
                Log.e(TAG, "Unable to display overlay", error)
                Toast.makeText(context, "Today: $count reels", Toast.LENGTH_SHORT).show()
            } catch (error: SecurityException) {
                Log.e(TAG, "Overlay permission was rejected", error)
                Toast.makeText(context, "Today: $count reels", Toast.LENGTH_SHORT).show()
            }
        }
        label.text = "$count reels"
    }

    fun hide() {
        view?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                // The system may already have removed the overlay.
            }
        }
        view = null
        layoutParams = null
    }

    private inner class DragListener : android.view.View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(view: android.view.View, event: MotionEvent): Boolean {
            val params = layoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (error: IllegalArgumentException) {
                        Log.w(TAG, "Overlay was removed while moving", error)
                    }
                    return true
                }
            }
            return true
        }
    }

    private companion object {
        const val TAG = "ReelCounterOverlay"
    }
}
