package com.reelcounter.counting

import android.accessibilityservice.AccessibilityService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.reelcounter.data.CounterRepository
import com.reelcounter.widget.ReelCounterWidget

class ReelAccessibilityService : AccessibilityService() {
    private lateinit var repository: CounterRepository
    private lateinit var overlay: OverlayController
    private var lastCountAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = CounterRepository(this)
        overlay = OverlayController(this)
        val configuredInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            // Window transitions must be observed for every package so the
            // overlay can be hidden as soon as the user leaves a target app.
            packageNames = null
            notificationTimeout = 100
        }
        serviceInfo = configuredInfo
        repository.setServiceConnected(true)
        repository.recordEvent("Service connected; waiting for a forward vertical reel swipe.")
        Log.i(TAG, "Reel Counter accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName in TRACKED_PACKAGES) {
                overlay.show(repository.todayCount())
            } else {
                overlay.hide()
            }
            return
        }
        if (packageName !in TRACKED_PACKAGES) return

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        if (packageName != YOUTUBE_PACKAGE && event.source?.isScrollable != true) return
        if (!isForwardVerticalScroll(packageName, event)) return

        val now = System.currentTimeMillis()
        if (now - lastCountAt < SCROLL_DEBOUNCE_MILLIS) return
        lastCountAt = now
        repository.recordEvent("$packageName: ${eventTypeName(event.eventType)}")
        val count = repository.incrementToday()
        overlay.show(count)
        Log.i(TAG, "Counted scroll in $packageName: $count")
        val widgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(this, ReelCounterWidget::class.java))
        if (widgetIds.isNotEmpty()) ReelCounterWidget().onUpdate(this, widgetManager, widgetIds)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::repository.isInitialized) repository.setServiceConnected(false)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }

    private fun eventTypeName(eventType: Int): String = when (eventType) {
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "view scrolled"
        else -> "event $eventType"
    }

    private fun isForwardVerticalScroll(packageName: String, event: AccessibilityEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val verticalDelta = event.scrollDeltaY
            val horizontalDelta = event.scrollDeltaX
            if (verticalDelta == 0) return false
            if (kotlin.math.abs(verticalDelta) <= kotlin.math.abs(horizontalDelta)) return false
            return if (packageName == YOUTUBE_PACKAGE) {
                verticalDelta < 0
            } else {
                verticalDelta > 0
            }
        }

        // Older Android versions do not expose scroll deltas. The source is
        // still restricted to scrollable views, but direction cannot be known.
        return true
    }

    private companion object {
        const val TAG = "ReelCounter"
        // Accessibility may emit several scroll callbacks for one physical swipe.
        const val SCROLL_DEBOUNCE_MILLIS = 750L
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        val TRACKED_PACKAGES = setOf(
            "com.instagram.android",
            YOUTUBE_PACKAGE,
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.sharechat.nativeapp",
            "in.mohalla.video",
            "in.mohalla.videocompany",
            "com.mojapp.android"
        )
    }
}
