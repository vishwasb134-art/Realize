package com.realize.app.ui.screens.schedule

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.realize.app.data.AppUsageInfo
import com.realize.app.data.StudyBlock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val studyBlocks by viewModel.studyBlocksFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
        viewModel.checkActiveSessionNow()
    }

    val isSessionActive = uiState.activeBlockNow != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Study Schedule & Lock",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { showAppPickerSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Manage Locked Apps",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Study Time", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ACTIVE STUDY STATUS BANNER
            item {
                ActiveStudyStatusCard(
                    activeBlock = uiState.activeBlockNow,
                    isLockerEnabled = uiState.isLockerEnabled
                )
            }

            // 2. USAGE ACCESS PERMISSION WARNING (IF NOT GRANTED)
            if (!uiState.isUsageAccessGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usage Access Required to Lock Apps",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Tap to grant permission so Realize can detect Instagram & games.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
            }

            // 3. MASTER LOCK CONTROLS (FROZEN DURING ACTIVE STUDY)
            item {
                MasterLockControlsCard(
                    isLockerEnabled = uiState.isLockerEnabled,
                    onToggleLocker = { viewModel.setLockerEnabled(it) },
                    lockAllGames = uiState.lockAllGames,
                    onToggleLockGames = { viewModel.setLockAllGames(it) },
                    onOpenAppPicker = { showAppPickerSheet = true },
                    lockedCount = uiState.lockedPackages.size,
                    gamesCount = uiState.detectedGamesCount,
                    isStudyActive = isSessionActive
                )
            }

            // 4. QUICK ADD TOMORROW'S SCHEDULE BANNER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plan Tomorrow's Study Hours",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Set your focus schedule today. Once study time starts, locks cannot be turned off until session finishes for conciousness.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("+ Plan")
                        }
                    }
                }
            }

            // 5. SCHEDULED STUDY SESSIONS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Study Sessions (${studyBlocks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSessionActive) {
                        Text(
                            text = "🔒 Non-deletable during active focus",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // 6. STUDY BLOCKS LIST
            if (studyBlocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Study Sessions Added Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add your daily study time or tomorrow's schedule below. During study time, Instagram and games will be locked automatically for conciousness!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("+ Add First Study Block")
                            }
                        }
                    }
                }
            } else {
                items(studyBlocks, key = { it.id }) { block ->
                    val isThisBlockActive = uiState.activeBlockNow?.id == block.id
                    StudyBlockItemCard(
                        block = block,
                        isActiveSession = isThisBlockActive,
                        onToggle = { enabled -> viewModel.toggleBlockEnabled(block, enabled) },
                        onDelete = { viewModel.deleteStudyBlock(block) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // STRICT LOCK ALERT DIALOG
    if (uiState.strictAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearStrictAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Strict Focus Mode", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(uiState.strictAlertMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearStrictAlert() }) {
                    Text("Understood")
                }
            }
        )
    }

    // ADD STUDY BLOCK DIALOG
    if (showAddDialog) {
        AddStudyBlockDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, startH, startM, endH, endM, isDaily, forTomorrow ->
                viewModel.addStudyBlock(title, startH, startM, endH, endM, isDaily, forTomorrow)
                showAddDialog = false
            }
        )
    }

    // MANAGE LOCKED APPS SHEET
    if (showAppPickerSheet) {
        LockedAppsDialog(
            apps = uiState.installedApps,
            lockedPackages = uiState.lockedPackages,
            lockAllGames = uiState.lockAllGames,
            isStudyActive = isSessionActive,
            onToggleLockGames = { viewModel.setLockAllGames(it) },
            onToggleApp = { viewModel.toggleAppLocked(it) },
            onDismiss = { showAppPickerSheet = false }
        )
    }
}

@Composable
fun ActiveStudyStatusCard(
    activeBlock: StudyBlock?,
    isLockerEnabled: Boolean
) {
    if (activeBlock != null && isLockerEnabled) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF991B1B))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔒 Study Time Active • Locks Frozen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${activeBlock.title} • Ends at ${activeBlock.formattedTimeRange.substringAfter('–').trim()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFEE2E2)
                    )
                    Text(
                        text = "Instagram and games are locked. You cannot delete this study time or remove locks until study time ends for conciousness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Guard Armed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "When your study schedule begins, Instagram and games will be locked automatically. Once active, locks cannot be turned off for conciousness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun MasterLockControlsCard(
    isLockerEnabled: Boolean,
    onToggleLocker: (Boolean) -> Unit,
    lockAllGames: Boolean,
    onToggleLockGames: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    lockedCount: Int,
    gamesCount: Int,
    isStudyActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Main App Locker Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lock Apps During Study Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isStudyActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isStudyActive) "🔒 Cannot be turned off during active study session" else if (isLockerEnabled) "Locker active during all scheduled sessions" else "Locker is currently paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isStudyActive) Color(0xFFDC2626) else MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = isLockerEnabled,
                    onCheckedChange = onToggleLocker,
                    enabled = !isStudyActive // FROZEN during study session!
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Games Lock Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lock All Games on Phone",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isStudyActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isStudyActive) "🔒 All games locked for study session" else "Auto-detects and blocks all installed games ($gamesCount found)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStudyActive) Color(0xFFDC2626) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(
                    checked = lockAllGames,
                    onCheckedChange = onToggleLockGames,
                    enabled = !isStudyActive // FROZEN during study session!
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App list picker shortcut
            OutlinedButton(
                onClick = onOpenAppPicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Apps to Lock ($lockedCount selected • Instagram included)")
            }
        }
    }
}

@Composable
fun StudyBlockItemCard(
    block: StudyBlock,
    isActiveSession: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val tomorrowEpochDay = LocalDate.now().plusDays(1).toEpochDay()
    val todayEpochDay = LocalDate.now().toEpochDay()

    val badgeLabel = when {
        isActiveSession -> "🔥 ACTIVE NOW"
        block.isDailyRecurring -> "Daily Recurring"
        block.targetDateEpochDay == tomorrowEpochDay -> "Tomorrow"
        block.targetDateEpochDay == todayEpochDay -> "Today"
        else -> "Scheduled"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveSession) Color(0xFF1E293B) else if (block.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActiveSession) 4.dp else if (block.isEnabled) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveSession) Color.White else if (block.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActiveSession) Color(0xFFDC2626) else if (block.isDailyRecurring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveSession) Color.White else if (block.isDailyRecurring) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isActiveSession) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = block.formattedTimeRange,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActiveSession) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isActiveSession) {
                // Non-deletable & non-toggleable during active focus!
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626).copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Session Locked",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            } else {
                Switch(
                    checked = block.isEnabled,
                    onCheckedChange = onToggle
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyBlockDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, startH: Int, startM: Int, endH: Int, endM: Int, isDaily: Boolean, forTomorrow: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }

    var scheduleMode by remember { mutableIntStateOf(0) }

    var startHour by remember { mutableIntStateOf(18) }
    var startMinute by remember { mutableIntStateOf(0) }

    var endHour by remember { mutableIntStateOf(21) }
    var endMinute by remember { mutableIntStateOf(0) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val tomorrowDateStr = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Study Time Block", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Study Subject or Goal") },
                    placeholder = { Text("e.g. Maths Study, DSA Coding, Exam Prep") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "When does this study block apply?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { scheduleMode = 0 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = scheduleMode == 0, onClick = { scheduleMode = 0 })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Every Day (Daily Recurring)", fontWeight = FontWeight.SemiBold)
                        Text("Instagram & games locked at this time every day for conciousness", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { scheduleMode = 1 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = scheduleMode == 1, onClick = { scheduleMode = 1 })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Tomorrow's Schedule ($tomorrowDateStr)", fontWeight = FontWeight.SemiBold)
                        Text("Plan today for tomorrow's focus hours for conciousness", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { scheduleMode = 2 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = scheduleMode == 2, onClick = { scheduleMode = 2 })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Today Only", fontWeight = FontWeight.SemiBold)
                        Text("Focus session for today for conciousness", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Starts At", style = MaterialTheme.typography.labelSmall)
                            Text(formatTime(startHour, startMinute), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ends At", style = MaterialTheme.typography.labelSmall)
                            Text(formatTime(endHour, endMinute), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = startHour == 18 && endHour == 21,
                        onClick = {
                            startHour = 18; startMinute = 0; endHour = 21; endMinute = 0
                        },
                        label = { Text("6-9 PM") }
                    )
                    FilterChip(
                        selected = startHour == 9 && endHour == 12,
                        onClick = {
                            startHour = 9; startMinute = 0; endHour = 12; endMinute = 0
                        },
                        label = { Text("9-12 AM") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val cal = Calendar.getInstance()
                            startHour = cal.get(Calendar.HOUR_OF_DAY)
                            startMinute = cal.get(Calendar.MINUTE)
                            endHour = (startHour + 2) % 24
                            endMinute = startMinute
                        },
                        label = { Text("+2 Hours") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "Study Session" }
                    val isDaily = scheduleMode == 0
                    val forTomorrow = scheduleMode == 1
                    onSave(finalTitle, startHour, startMinute, endHour, endMinute, isDaily, forTomorrow)
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    startHour = timePickerState.hour
                    startMinute = timePickerState.minute
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    endHour = timePickerState.hour
                    endMinute = timePickerState.minute
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LockedAppsDialog(
    apps: List<AppUsageInfo>,
    lockedPackages: Set<String>,
    lockAllGames: Boolean,
    isStudyActive: Boolean,
    onToggleLockGames: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Apps to Lock", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isStudyActive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF991B1B).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🔒 Study time is in progress. You cannot unlock apps or games until study time finishes for conciousness.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Master Games Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Gamepad, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lock All Games Automatically", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Any game installed is blocked during study hours", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(
                            checked = lockAllGames,
                            onCheckedChange = onToggleLockGames,
                            enabled = !isStudyActive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Apps to Lock During Study Sessions:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        val isLocked = lockedPackages.contains(app.packageName)
                        val isInstagram = app.packageName == "com.instagram.android"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!(isStudyActive && isLocked)) {
                                        onToggleApp(app.packageName)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(app.appName.take(1))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.appName,
                                            fontWeight = if (isInstagram) FontWeight.ExtraBold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (isInstagram) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFE1306C)
                                            ) {
                                                Text(
                                                    text = "INSTAGRAM",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (app.isGame) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = "GAME",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Switch(
                                checked = isLocked,
                                onCheckedChange = { onToggleApp(app.packageName) },
                                enabled = !(isStudyActive && isLocked) // Cannot UNLOCK during active study!
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

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
}
