package com.realize.app.ui.screens.schedule

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realize.app.data.Assignment
import com.realize.app.data.RealizeDatabase
import com.realize.app.data.UsageStatsHelper
import com.realize.app.ui.theme.RealizeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * Fullscreen Lock Activity displayed whenever the student opens Instagram, games,
 * or distracting apps during a scheduled study block.
 * Overrides back navigation to return directly to the Android Home launcher.
 */
class LockScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val EXTRA_STUDY_TITLE = "extra_study_title"
        const val EXTRA_END_HOUR = "extra_end_hour"
        const val EXTRA_END_MINUTE = "extra_end_minute"

        fun createIntent(
            context: Context,
            packageName: String,
            studyTitle: String,
            endHour: Int,
            endMinute: Int
        ): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                putExtra(EXTRA_STUDY_TITLE, studyTitle)
                putExtra(EXTRA_END_HOUR, endHour)
                putExtra(EXTRA_END_MINUTE, endMinute)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Allow showing over lockscreen if screen is locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: "com.instagram.android"
        val studyTitle = intent.getStringExtra(EXTRA_STUDY_TITLE) ?: "Scheduled Study Session"
        val endHour = intent.getIntExtra(EXTRA_END_HOUR, 22)
        val endMinute = intent.getIntExtra(EXTRA_END_MINUTE, 0)

        val (appName, _) = UsageStatsHelper.getAppDetails(packageManager, lockedPackage)

        // Intercept back button: Always send user to Android home so they cannot re-enter the locked app!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToHomeScreen()
            }
        })

        setContent {
            RealizeTheme(darkTheme = true) {
                LockScreenContent(
                    appName = appName,
                    studyTitle = studyTitle,
                    endHour = endHour,
                    endMinute = endMinute,
                    onBackToStudiesClicked = { exitToHomeScreen() }
                )
            }
        }
    }

    private fun exitToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun LockScreenContent(
    appName: String,
    studyTitle: String,
    endHour: Int,
    endMinute: Int,
    onBackToStudiesClicked: () -> Unit
) {
    var nearestAssignments by remember { mutableStateOf<List<Assignment>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = RealizeDatabase.getDatabase(context)
            nearestAssignments = db.assignmentDao().getTop2NearestPending()
        }
    }

    // Format end time
    val endAmPm = if (endHour < 12) "AM" else "PM"
    val displayEndHour = when {
        endHour == 0 -> 12
        endHour > 12 -> endHour - 12
        else -> endHour
    }
    val formattedEndTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayEndHour, endMinute, endAmPm)

    // Calculate remaining minutes
    val nowCalendar = Calendar.getInstance()
    val currentMinuteOfDay = nowCalendar.get(Calendar.HOUR_OF_DAY) * 60 + nowCalendar.get(Calendar.MINUTE)
    val endMinuteOfDay = endHour * 60 + endMinute
    val remainingMinutes = if (endMinuteOfDay >= currentMinuteOfDay) {
        endMinuteOfDay - currentMinuteOfDay
    } else {
        (24 * 60 - currentMinuteOfDay) + endMinuteOfDay
    }
    val remainingHours = remainingMinutes / 60
    val remainingMinsOnly = remainingMinutes % 60
    val remainingText = if (remainingHours > 0) {
        "${remainingHours}h ${remainingMinsOnly}m remaining"
    } else {
        "${remainingMinsOnly}m remaining"
    }

    // Subtle pulsing animation for lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Deep slate background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Animated Lock Icon Shield
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEF4444).copy(alpha = 0.35f),
                                Color(0xFFDC2626).copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFDC2626), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Subtitle
            Text(
                text = "Study Time Active",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$appName is locked until $formattedEndTime",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFCA5A5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Study Session Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = studyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "⏳ $remainingText until this study block finishes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "“Focus now so you don't have to stress later. Instagram and games will still be there after you finish.”",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Pending Work Section
            if (nearestAssignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pending Work You Should Be Doing:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        nearestAssignments.forEach { assignment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = assignment.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = assignment.subject,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = assignment.daysLeftText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Big Exit / Back to studies Button
            Button(
                onClick = onBackToStudiesClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Back to Studies (Close $appName)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
