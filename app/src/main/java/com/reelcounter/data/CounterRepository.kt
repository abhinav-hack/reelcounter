package com.reelcounter.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayCount(val date: String, val count: Int)
data class MonthCount(val label: String, val count: Int)

class CounterRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reel_counts", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Synchronized
    fun incrementToday(): Int {
        val key = dateFormat.format(Date())
        val updated = preferences.getInt(key, 0) + 1
        preferences.edit().putInt(key, updated).apply()
        return updated
    }

    fun todayCount(): Int = preferences.getInt(dateFormat.format(Date()), 0)

    fun setServiceConnected(connected: Boolean) {
        preferences.edit().putBoolean(KEY_SERVICE_CONNECTED, connected).apply()
    }

    fun isServiceConnected(): Boolean =
        preferences.getBoolean(KEY_SERVICE_CONNECTED, false)

    fun lastEventDescription(): String =
        preferences.getString(KEY_LAST_EVENT, "No supported-app event received yet.")
            ?: "No supported-app event received yet."

    fun recordEvent(description: String) {
        preferences.edit().putString(KEY_LAST_EVENT, description).apply()
    }

    fun recentDays(numberOfDays: Int = 7): List<DayCount> {
        val calendar = Calendar.getInstance()
        return (numberOfDays - 1 downTo 0).map {
            val date = dateFormat.format(calendar.time)
            val day = DayCount(date, preferences.getInt(date, 0))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            day
        }.reversed()
    }

    fun monthTotals(numberOfMonths: Int = 6): List<MonthCount> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return (numberOfMonths - 1 downTo 0).map {
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            val total = preferences.all.entries.sumOf { entry ->
                val parsed = runCatching { dateFormat.parse(entry.key) }.getOrNull()
                val parsedCalendar = parsed?.let { Calendar.getInstance().apply { time = it } }
                if (parsedCalendar != null &&
                    parsedCalendar.get(Calendar.MONTH) == month &&
                    parsedCalendar.get(Calendar.YEAR) == year) {
                    (entry.value as? Int) ?: 0
                } else 0
            }
            MonthCount(monthFormat.format(calendar.time), total).also {
                calendar.add(Calendar.MONTH, -1)
            }
        }.reversed()
    }

    private companion object {
        const val KEY_SERVICE_CONNECTED = "service_connected"
        const val KEY_LAST_EVENT = "last_event"
    }
}
