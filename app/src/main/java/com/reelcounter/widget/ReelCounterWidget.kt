package com.reelcounter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.widget.RemoteViews
import com.reelcounter.R
import com.reelcounter.MainActivity
import com.reelcounter.data.CounterRepository

class ReelCounterWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val count = CounterRepository(context).todayCount()
        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ids.forEach { id ->
            manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.widget_reel_counter).apply {
                setTextViewText(R.id.widget_count, "$count reels")
                setOnClickPendingIntent(R.id.widget_root, launchIntent)
                setOnClickPendingIntent(R.id.widget_count, launchIntent)
                setOnClickPendingIntent(R.id.widget_title, launchIntent)
            })
        }
    }
}
