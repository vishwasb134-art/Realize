package com.realize.app.ui.screens.usage

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.realize.app.data.AppUsageInfo
import com.realize.app.data.DailyUsageBar
import com.realize.app.ui.theme.AmberWarning
import com.realize.app.ui.theme.EmeraldSecondary
import com.realize.app.ui.theme.IndigoPrimary
import com.realize.app.ui.theme.RedDanger

/**
 * Usage Screen showing today's social media time, a 7-day bar chart,
 * 30-minute limit settings, and an installed app selector.
 */
@Composable
fun UsageScreen(
    viewModel: UsageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh when returning from Android Settings screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var showLimitPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HeaderSection(isServiceActive = uiState.isServiceActive)
        }

        // 1. Permission Warning Banner (if usage stats access not yet granted)
        if (!uiState.hasPermission) {
            item {
                PermissionRequestCard(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        } else {
            // 2. Today's Overview & Daily Limit Progress
            item {
                TodaySummaryCard(
                    totalMinutes = uiState.totalTodayMinutes,
                    limitMinutes = uiState.limitMinutes,
                    onEditLimit = { showLimitPicker = true }
                )
            }

            // 3. 7-Day Bar Chart
            item {
                WeeklyUsageChartCard(
                    weeklyBars = uiState.weeklyBars,
                    limitMinutes = uiState.limitMinutes
                )
            }

            // 4. Per-App Today Breakdown Header & Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monitored Apps Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.loadInstalledApps()
                            showAppPicker = true
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Customize Apps", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 5. Per-App Usage Items
            if (uiState.todayApps.isEmpty()) {
                item {
                    Text(
                        text = "No usage recorded yet today for your selected social apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.todayApps, key = { it.packageName }) { appInfo ->
                    AppUsageCard(
                        appInfo = appInfo,
                        limitMinutes = uiState.limitMinutes
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // App Picker Dialog
    if (showAppPicker) {
        AppPickerDialog(
            installedApps = uiState.installedApps,
            onToggleApp = { pkg, isSocial -> viewModel.toggleAppSocial(pkg, isSocial) },
            onDismiss = { showAppPicker = false }
        )
    }

    // Daily Limit Picker Dialog
    if (showLimitPicker) {
        LimitPickerDialog(
            currentLimit = uiState.limitMinutes,
            onSaveLimit = {
                viewModel.updateLimit(it)
                showLimitPicker = false
            },
            onDismiss = { showLimitPicker = false }
        )
    }
}

@Composable
private fun HeaderSection(isServiceActive: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Social Usage",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Live Service Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isServiceActive) EmeraldSecondary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) EmeraldSecondary else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceActive) "Guard Active" else "Guard Standby",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isServiceActive) EmeraldSecondary else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Foreground screen time & 30-min break alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRequestCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Usage Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android requires \"Usage Access\" permission to measure foreground screen time on social apps. Tap below to find Realize in Settings and turn it on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
            ) {
                Text("Grant Usage Access", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(
    totalMinutes: Int,
    limitMinutes: Int,
    onEditLimit: () -> Unit
) {
    val progress = if (limitMinutes > 0) (totalMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f) else 0f
    val isOverLimit = totalMinutes >= limitMinutes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverLimit) RedDanger.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S SOCIAL TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${totalMinutes}m",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOverLimit) RedDanger else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "/ ${limitMinutes}m limit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onEditLimit() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Change Limit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    isOverLimit -> RedDanger
                    progress > 0.8f -> AmberWarning
                    else -> EmeraldSecondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOverLimit) "🚨 Limit reached! Nudges are active with your top pending assignments."
                else "✅ Under limit. You have ${maxOf(0, limitMinutes - totalMinutes)} mins remaining today.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverLimit) RedDanger else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyUsageChartCard(
    weeklyBars: List<DailyUsageBar>,
    limitMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Last 7 Days Usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daily social media foreground minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            val maxMinutes = (weeklyBars.maxOfOrNull { it.totalMinutes } ?: 60).coerceAtLeast(limitMinutes)

            // Simple 7-day bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyBars.forEach { bar ->
                    val barHeightFraction = if (maxMinutes > 0) {
                        (bar.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
                    } else 0.04f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${bar.totalMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    when {
                                        bar.totalMinutes >= limitMinutes -> RedDanger
                                        bar.isToday -> IndigoPrimary
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.isToday) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    appInfo: AppUsageInfo,
    limitMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val iconBitmap = remember(appInfo.icon) {
                appInfo.icon?.let { drawableToBitmap(it) }
            }

            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = appInfo.appName,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))

                val appProgress = if (limitMinutes > 0) {
                    (appInfo.totalMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { appProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (appInfo.totalMinutes >= limitMinutes) RedDanger else IndigoPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = appInfo.formattedTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (appInfo.totalMinutes >= limitMinutes) RedDanger else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppPickerDialog(
    installedApps: List<AppUsageInfo>,
    onToggleApp: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pick Monitored Social Apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select which installed apps count towards your study break limit. (WhatsApp is excluded by default).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = app.isSelectedSocial,
                                onCheckedChange = { checked ->
                                    onToggleApp(app.packageName, checked)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun LimitPickerDialog(
    currentLimit: Int,
    onSaveLimit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLimit by remember { mutableIntStateOf(currentLimit) }
    val options = listOf(15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Social Screen Time Limit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "When continuous or total daily usage reaches this limit, Realize will alert you with your pending assignments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(3).forEach { min ->
                        FilterChip(
                            selected = selectedLimit == min,
                            onClick = { selectedLimit = min },
                            label = { Text("${min}m") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.drop(3).forEach { min ->
                        FilterChip(
                            selected = selectedLimit == min,
                            onClick = { selectedLimit = min },
                            label = { Text("${min}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveLimit(selectedLimit) }) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
