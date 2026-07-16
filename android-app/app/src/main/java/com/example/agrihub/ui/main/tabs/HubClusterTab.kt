package com.example.agriflow.ui.main.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.agriflow.data.local.HubClusterEntity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agriflow.data.local.decodePoints
import com.example.agriflow.network.Coordinate
import com.example.agriflow.ui.components.GenericHistoryScreen
import com.example.agriflow.ui.components.HistoryFilterConfig
import com.example.agriflow.ui.components.HistoryModalDialog
import com.example.agriflow.ui.main.components.CardHeader
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.math.max

// Helper to create the hub centroid marker (Warehouse Icon)
private fun createHubMarker(context: android.content.Context): android.graphics.drawable.BitmapDrawable {
    val color = android.graphics.Color.parseColor("#E57373") // Gentle Red
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw shadow
    val shadowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(android.graphics.Color.argb(50, 0, 0, 0))
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f + (2 * density), size / 2f - (2 * density), shadowPaint)
    
    // Circle background
    val bgPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(color)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - (2 * density), bgPaint)
    
    // Draw white border
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(android.graphics.Color.WHITE)
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - (3 * density), borderPaint)
    
    // Draw Hub Icon (using Factory/Warehouse Unicode emoji)
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(android.graphics.Color.WHITE)
        textSize = 18 * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    
    return bitmap.toDrawable(context.resources)
}

// Helper to create the farm marker (Green + Agriculture Icon)
private fun createFarmMarker(context: android.content.Context): android.graphics.drawable.BitmapDrawable {
    val color = android.graphics.Color.parseColor("#2E7D32") // Forest Green
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Pin background
    val bgPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(color)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - (2 * density), bgPaint)
    
    // Icon
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(android.graphics.Color.WHITE)
        textSize = 16 * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    
    return bitmap.toDrawable(context.resources)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubClusterTab(viewModel: AgriFlowViewModel) {
    var farms by remember { mutableStateOf(listOf<GeoPoint>()) }
    var kInput by remember { mutableFloatStateOf(2.0f) }

    val hubState by viewModel.hubState.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val hubClusterHistory by viewModel.hubClusterHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingHubReuse.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    
    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.fetchCurrentLocation()
        }
    }

    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedHistoryItem by remember { mutableStateOf<HubClusterEntity?>(null) }

    // Filter States
    var hubFilterDays by remember { mutableStateOf<Int?>(null) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    var multiHubFilter by remember { mutableStateOf(false) }
    var largeFarmFilter by remember { mutableStateOf(false) }

    var kRange by remember { mutableStateOf(1f..5f) }

    val filteredHubHistory = remember(hubClusterHistory, hubFilterDays, startDateMillis, endDateMillis, multiHubFilter, largeFarmFilter, kRange) {
        var list = hubClusterHistory
        if (hubFilterDays != null) {
            val threshold = System.currentTimeMillis() - (hubFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        } else if (startDateMillis != null) {
            list = list.filter { it.timestamp >= startDateMillis!! && (endDateMillis == null || it.timestamp <= endDateMillis!! + 86400000L) }
        }
        if (multiHubFilter) {
            list = list.filter { it.k > 2 }
        }
        if (largeFarmFilter) {
            list = list.filter { decodePoints(it.farmPoints).size > 5 }
        }
        list = list.filter { it.k.toFloat() in kRange }
        list
    }

    LaunchedEffect(pendingReuse) {
        pendingReuse?.let { entry ->
            farms = decodePoints(entry.farmPoints).map { (lat, lng) -> GeoPoint(lat, lng) }
            kInput = entry.k.toFloat()
            viewModel.clearHubReuse()
        }
    }

    val hubIcon = remember(context) {
        createHubMarker(context)
    }

    val mapView = remember {
        MapView(context).apply {
            org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(14.2778, 121.1250))
        }
    }

    LaunchedEffect(locationState) {
        if (locationState is SoapUiState.Success) {
            val loc = (locationState as SoapUiState.Success).data
            val newPoint = GeoPoint(loc.latitude, loc.longitude)
            if (!farms.any { it.latitude == newPoint.latitude && it.longitude == newPoint.longitude }) {
                farms = farms + newPoint
            }
            // Center the map on the new location
            mapView.controller.animateTo(newPoint)
            viewModel.resetLocationState()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mapEventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                farms = farms + p
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardHeader(
                title = "Logistics Hub Clustering (K-Means)",
                subtitle = "Drop farm coordinates on the map."
            )
            IconButton(onClick = { showHistoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Target Hubs (K)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(String.format(Locale.US, "%d Clusters", kInput.toInt()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = kInput,
                onValueChange = { kInput = it },
                valueRange = 1.0f..max(1.1f, farms.size.toFloat()),
                steps = if (farms.size > 1) farms.size - 2 else 0,
                enabled = farms.size > 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(map)
                    map.overlays.clear()
                    map.overlays.add(mapEventsOverlay)

                    farms.forEach { farm ->
                        val marker = Marker(map).apply {
                            position = farm
                            infoWindow = null
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(marker)
                    }

                    if (hubState is SoapUiState.Success) {
                        val hubs = (hubState as SoapUiState.Success<List<Coordinate>>).data
                        hubs.forEachIndexed { index, hub ->
                            val marker = Marker(map).apply {
                                position = GeoPoint(hub.latitude, hub.longitude)
                                title = "Hub #${index + 1}"
                                snippet = String.format(Locale.US, "%.3f, %.3f", hub.latitude, hub.longitude)
                                icon = hubIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                            map.overlays.add(marker)
                        }
                    }

                    map.invalidate()
                }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { 
                        locationPermissionLauncher.launch(arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)),
                    modifier = Modifier.height(36.dp),
                    enabled = locationState !is SoapUiState.Loading
                ) {
                    if (locationState is SoapUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("My Location", fontSize = 11.sp)
                    }
                }

                FilledTonalButton(
                    onClick = {
                        farms = emptyList()
                        viewModel.resetHubState()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Clear Map", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { mapView.controller.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }

                SmallFloatingActionButton(
                    onClick = { mapView.controller.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                FloatingActionButton(
                    onClick = {
                        if (farms.isNotEmpty()) {
                            val lats = farms.map { it.latitude }
                            val lngs = farms.map { it.longitude }
                            val k = kInput.toInt()
                            // Frontend validation: cap K at N if user requests more hubs than farms
                            val finalK = if (k > farms.size) farms.size else k
                            viewModel.runHubCluster(lats, lngs, finalK)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compute Hubs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryModalDialog(
            title = "Hub Clustering History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredHubHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Today", "Last 7 Days", "Last 30 Days", "Multi-Hub (K > 2)", "Large Farm Count (> 5)")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Today" -> {
                            hubFilterDays = if (isSelected) 1 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 7 Days" -> {
                            hubFilterDays = if (isSelected) 7 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 30 Days" -> {
                            hubFilterDays = if (isSelected) 30 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Multi-Hub (K > 2)" -> multiHubFilter = isSelected
                        "Large Farm Count (> 5)" -> largeFarmFilter = isSelected
                    }
                },
                onDateRangeSelected = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                    if (start != null) hubFilterDays = null
                },
                onAdvancedFilterSave = { },
                advancedFilterContent = {
                    Text("Clustering Parameters", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Cluster Count (K): ${kRange.start.toInt()} - ${kRange.endInclusive.toInt()}", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = kRange,
                        onValueChange = { kRange = it },
                        valueRange = 1f..10f,
                        steps = 9
                    )
                },
                itemContent = { entry ->
                    val farmPoints = remember(entry.farmPoints) { decodePoints(entry.farmPoints) }
                    val hubPoints = remember(entry.resultHubs) { decodePoints(entry.resultHubs) }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedHistoryItem = entry }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${farmPoints.size} farms · K=${entry.k}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lat/Lng: " + farmPoints.take(2).joinToString { String.format(Locale.US, "%.2f,%.2f", it.first, it.second) } + if (farmPoints.size > 2) "..." else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
                                        .format(java.util.Date(entry.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "${hubPoints.size} hubs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            )
        }
    }

    if (selectedHistoryItem != null) {
        val item = selectedHistoryItem!!
        val farmPointsList = remember(item.farmPoints) { decodePoints(item.farmPoints).map { GeoPoint(it.first, it.second) } }
        val hubPointsList = remember(item.resultHubs) { decodePoints(item.resultHubs).map { GeoPoint(it.first, it.second) } }

        AlertDialog(
            onDismissRequest = { selectedHistoryItem = null },
            confirmButton = {
                TextButton(onClick = { selectedHistoryItem = null }) { Text("Close") }
            },
            title = { Text("Hub Clustering Result Snapshot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Timestamp: ${java.text.SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(java.util.Date(item.timestamp))}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Configuration: ${farmPointsList.size} Farms -> ${item.k} Clusters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        AndroidView(
                            factory = { context ->
                                MapView(context).apply {
                                    org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
                                    setMultiTouchControls(true)
                                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                }
                            },
                            update = { map ->
                                map.overlays.clear()
                                farmPointsList.forEach { p ->
                                    map.overlays.add(Marker(map).apply {
                                        position = p
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    })
                                }
                                hubPointsList.forEach { p ->
                                    map.overlays.add(Marker(map).apply {
                                        position = p
                                        icon = hubIcon
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    })
                                }
                                if (farmPointsList.isNotEmpty()) {
                                    map.controller.setZoom(13.0)
                                    map.controller.setCenter(farmPointsList.first())
                                }
                                map.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Text(
                        text = "Calculated Hub Coordinates:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    hubPointsList.forEachIndexed { idx, p ->
                        Text(
                            text = String.format(Locale.US, "Hub #%d: %.5f, %.5f", idx + 1, p.latitude, p.longitude),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        )
    }
}
