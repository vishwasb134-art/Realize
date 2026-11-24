package com.realize.app.ui.screens.places

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.realize.app.data.AvoidZone
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val avoidZones by viewModel.avoidZonesFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBgLocationExplainer by remember { mutableStateOf(false) }

    // Fine location permission launcher
    val fineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            viewModel.checkPermissionsAndLoad()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !uiState.hasBackgroundLocation) {
                showBgLocationExplainer = true
            }
        }
    }

    // Background location launcher
    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissionsAndLoad()
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionsAndLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Avoid Zones & Near Shop Alerts",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PERMISSION WARNING CARDS
            if (!uiState.hasFineLocation) {
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
                                    text = "Location Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Realize needs location access to alert you when entering shopping temptation zones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    fineLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
            } else if (!uiState.hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Location Needed",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Select 'Allow all the time' so geofences trigger even when your phone is in your pocket.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showBgLocationExplainer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }

            // 2. SAVINGS GOAL SECTION
            item {
                SavingsGoalCard(
                    savingsGoal = uiState.savingsGoal,
                    onUpdateGoal = { viewModel.updateSavingsGoal(it) }
                )
            }

            // 3. QUICK PRESET: SHOP NEAR JAIN UNIVERSITY COLLEGE (10 METERS)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Shop Near Jain University College",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Trigger reminder at 10-meter proximity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Whenever you walk within 10 meters of the shop near your Jain University college, Realize will immediately send you a firm reminder to avoid spending and stay focused on your studies for conciousness.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (uiState.currentLat != null && uiState.currentLng != null) {
                                    viewModel.addAvoidZone(
                                        name = "Shop near Jain University College",
                                        latitude = uiState.currentLat!!,
                                        longitude = uiState.currentLng!!,
                                        radiusMeters = 10f
                                    )
                                } else {
                                    viewModel.fetchCurrentLocation { lat, lng ->
                                        viewModel.addAvoidZone(
                                            name = "Shop near Jain University College",
                                            latitude = lat,
                                            longitude = lng,
                                            radiusMeters = 10f
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Current Location as 10m Avoid Zone")
                        }
                    }
                }
            }

            // 4. ADD CUSTOM AVOID ZONE ON OPENSTREETMAP
            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PinDrop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Drop Pin on Map or Add Custom Avoid Zone")
                }
            }

            // 5. SAVED AVOID ZONES LIST
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Avoid Zones (${avoidZones.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supports up to 10+ zones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (avoidZones.isEmpty()) {
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
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Avoid Zones Added Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add shops, malls, or temptation zones where you want to be reminded not to buy anything for conciousness.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(avoidZones, key = { it.id }) { zone ->
                    AvoidZoneCard(
                        zone = zone,
                        onToggle = { enabled -> viewModel.toggleAvoidZone(zone, enabled) },
                        onDelete = { viewModel.deleteAvoidZone(zone) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // BACKGROUND LOCATION PERMISSION EXPLANATION DIALOG
    if (showBgLocationExplainer) {
        AlertDialog(
            onDismissRequest = { showBgLocationExplainer = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Background Location", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To alert you when you walk near shops even while your phone is locked or inside your pocket, Android requires selecting 'Allow all the time' in permission settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Your location is processed 100% locally on your device for geofencing and is never shared or uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showBgLocationExplainer = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) {
                    Text("Continue to Allow All the Time")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBgLocationExplainer = false
                    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(appSettingsIntent)
                }) {
                    Text("Open App Settings")
                }
            }
        )
    }

    // ADD AVOID ZONE MAP DIALOG (OSMDroid OpenStreetMap)
    if (showAddDialog) {
        AddAvoidZoneMapDialog(
            initialLat = uiState.currentLat ?: 12.9716, // Bangalore coordinates default
            initialLng = uiState.currentLng ?: 77.5946,
            onDismiss = { showAddDialog = false },
            onSave = { name, lat, lng, radius ->
                viewModel.addAvoidZone(name, lat, lng, radius)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SavingsGoalCard(
    savingsGoal: String,
    onUpdateGoal: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(savingsGoal) { mutableStateOf(savingsGoal) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Savings Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = {
                    if (isEditing) {
                        onUpdateGoal(textValue)
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                }) {
                    Text(if (isEditing) "Save" else "Edit")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = { Text("e.g. Save for semester tuition, books, exam fees") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                Text(
                    text = if (savingsGoal.isNotBlank()) "🎯 $savingsGoal" else "No specific savings goal set. Tap edit to set what you're saving for!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun AvoidZoneCard(
    zone: AvoidZone,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (zone.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (zone.isEnabled) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (zone.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (zone.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (zone.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Radius: ${zone.radiusMeters.roundToInt()} meters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(java.util.Locale.getDefault(), "%.5f, %.5f", zone.latitude, zone.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Switch(
                checked = zone.isEnabled,
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

@Composable
fun AddAvoidZoneMapDialog(
    initialLat: Double,
    initialLng: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, radius: Float) -> Unit
) {
    val context = LocalContext.current
    var zoneName by remember { mutableStateOf("Shop near Jain University College") }
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }
    var radiusMeters by remember { mutableFloatStateOf(10f) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Pin Avoid Zone Location", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name") },
                    placeholder = { Text("e.g. Shop near Jain University College") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Radius selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Alert Radius: ${radiusMeters.roundToInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 10f..300f,
                    steps = 28
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = radiusMeters == 10f,
                        onClick = { radiusMeters = 10f },
                        label = { Text("10m") }
                    )
                    FilterChip(
                        selected = radiusMeters == 25f,
                        onClick = { radiusMeters = 25f },
                        label = { Text("25m") }
                    )
                    FilterChip(
                        selected = radiusMeters == 50f,
                        onClick = { radiusMeters = 50f },
                        label = { Text("50m") }
                    )
                    FilterChip(
                        selected = radiusMeters == 100f,
                        onClick = { radiusMeters = 100f },
                        label = { Text("100m") }
                    )
                }

                Text(
                    text = "Tap on the map below to move the pin, or zoom into your location:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // OpenStreetMap (osmdroid) MapView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(17.5)
                                val initialPoint = GeoPoint(selectedLat, selectedLng)
                                controller.setCenter(initialPoint)

                                val marker = Marker(this).apply {
                                    position = initialPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = zoneName
                                }
                                overlays.add(marker)
                                markerRef = marker

                                val mapEventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        selectedLat = p.latitude
                                        selectedLng = p.longitude
                                        marker.position = p
                                        invalidate()
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint): Boolean {
                                        selectedLat = p.latitude
                                        selectedLng = p.longitude
                                        marker.position = p
                                        invalidate()
                                        return true
                                    }
                                }
                                overlays.add(0, MapEventsOverlay(mapEventsReceiver))

                                mapViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(zoneName.ifBlank { "Shop near Jain University College" }, selectedLat, selectedLng, radiusMeters)
                }
            ) {
                Text("Save Zone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
