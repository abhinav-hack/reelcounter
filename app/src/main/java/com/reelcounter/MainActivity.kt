package com.reelcounter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelcounter.counting.ReelAccessibilityService
import com.reelcounter.data.CounterRepository
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReelCounterTheme { ReelCounterScreen() } }
    }
}

@androidx.compose.runtime.Composable
private fun ReelCounterTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = ComposeColor(0xFFFF9F1C),
            secondary = ComposeColor(0xFFFF3D81),
            background = ComposeColor(0xFFF8F7FC),
            surface = ComposeColor.White
        ),
        content = content
    )
}

@androidx.compose.runtime.Composable
private fun ReelCounterScreen() {
    val context = LocalContext.current
    val repository = remember { CounterRepository(context) }
    var refreshKey by remember { mutableStateOf(0) }
    val today = remember(refreshKey) { repository.todayCount() }
    val days = remember(refreshKey) { repository.recentDays() }
    val months = remember(refreshKey) { repository.monthTotals() }
    val enabled = remember(refreshKey) { isAccessibilityEnabled(context) }
    val serviceConnected = remember(refreshKey) { repository.isServiceConnected() }
    val lastEvent = remember(refreshKey) { repository.lastEventDescription() }
    val overlayEnabled = remember(refreshKey) {
        Settings.canDrawOverlays(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ComposeColor(0xFFF6F3FB)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(ComposeColor(0xFF17151F), ComposeColor(0xFF432452))
                                ),
                                RoundedCornerShape(28.dp)
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(com.reelcounter.R.drawable.reelcounter_app_icon),
                            contentDescription = "Reel Counter",
                            modifier = Modifier
                                .size(66.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                "Reel Counter",
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor.White
                            )
                            Text(
                                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                                    .format(System.currentTimeMillis()),
                                color = ComposeColor(0xFFE4D7F2),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor.Transparent)
                ) {
                    Column(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(
                                listOf(
                                    ComposeColor(0xFF17151F),
                                    ComposeColor(0xFF432452),
                                    ComposeColor(0xFFFF3D81)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        ).padding(24.dp)
                    ) {
                        Text("TODAY'S SCROLLS", color = ComposeColor(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$today", color = ComposeColor.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        Text("reels watched", color = ComposeColor(0xFFF4EFFF), fontSize = 16.sp)
                        Text("Keep your scrolling intentional.", color = ComposeColor(0xFFD9B9E8), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            item {
                SectionTitle("This week", "Your last seven days")
                WeeklyChart(days)
            }
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFFFEFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.border(1.dp, ComposeColor(0xFFE9E5F0), RoundedCornerShape(22.dp))
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        SectionTitle("Past months", "A quick look at your history")
                        months.forEach { month ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(month.label, color = ComposeColor(0xFF6F6B75))
                                Text("${month.count} reels", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("Get counting", "One-time permissions keep the counter running")
                SetupCard(
                    title = "Enable scroll detection",
                    body = when {
                        serviceConnected -> "Service is running. $lastEvent"
                        enabled -> "Permission is enabled, but the service has not connected. Turn it off and on again in Accessibility settings."
                        else -> "One Android approval is required so Reel Counter can detect reel changes. Tap Open settings, choose Reel Counter, then turn on Allow restricted settings if shown and enable the service."
                    },
                    action = if (enabled) "Enabled" else "Open settings",
                    complete = enabled
                ) {
                    openAccessibilitySettings(context)
                }
                Spacer(Modifier.height(8.dp))
                SetupCard(
                    title = "Show count over other apps",
                    body = if (overlayEnabled) "The counter appears only while a supported social app is in the foreground." else "Allow the counter to appear only while a supported social app is open.",
                    action = if (overlayEnabled) "Enabled" else "Allow overlay",
                    complete = overlayEnabled
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (serviceConnected) ComposeColor(0xFFE8F7EF) else ComposeColor(0xFFFFF0F1)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    val statusColor = if (serviceConnected) ComposeColor(0xFF238636) else ComposeColor(0xFFC43D4B)
                    Text(
                        if (serviceConnected) "●  Detector active" else "●  Detector not connected",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Text(
                    "Tip: add the Reel Counter widget to your home screen for a quick glance.",
                    color = ComposeColor(0xFF6F6B75),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                TextButton(onClick = { refreshKey++ }) {
                    Text("Refresh")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SetupCard(title: String, body: String, action: String, complete: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFFFEFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.border(1.dp, ComposeColor(0xFFE9E5F0), RoundedCornerShape(20.dp))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF211D2B))
            Text(body, color = ComposeColor(0xFF6F6B75), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Button(
                onClick = onClick,
                enabled = !complete,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(action)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF211D2B))
        Text(subtitle, color = ComposeColor(0xFF716D7B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@androidx.compose.runtime.Composable
private fun WeeklyChart(days: List<com.reelcounter.data.DayCount>) {
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val labels = days.map { it.date.takeLast(2) }
    Card(
        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFFFEFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val barWidth = size.width / (days.size * 1.8f)
                val gap = barWidth * .8f
                days.forEachIndexed { index, day ->
                    val height = size.height * day.count / max
                    val left = index * (barWidth + gap) + gap / 2
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(ComposeColor(0xFF9A82DB), ComposeColor(0xFF6750A4))),
                        topLeft = androidx.compose.ui.geometry.Offset(left, size.height - height),
                        size = androidx.compose.ui.geometry.Size(barWidth, height.coerceAtLeast(5f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, fontSize = 11.sp, color = ComposeColor(0xFF6F6B75)) }
            }
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any {
        ComponentName.unflattenFromString(it) == ComponentName(context, ReelAccessibilityService::class.java)
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
