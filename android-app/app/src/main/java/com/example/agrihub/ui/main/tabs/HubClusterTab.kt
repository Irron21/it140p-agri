package com.example.agriflow.ui.main.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val hubClusterHistory by viewModel.hubClusterHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingHubReuse.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    
    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Filter States
    var hubFilterDays by remember { mutableStateOf<Int?>(null) }
    var multiHubFilter by remember { mutableStateOf(false) }
    var largeFarmFilter by remember { mutableStateOf(false) }

    var kRange by remember { mutableStateOf(1f..5f) }

    val filteredHubHistory = remember(hubClusterHistory, hubFilterDays, multiHubFilter, largeFarmFilter, kRange) {
        var list = hubClusterHistory
        if (hubFilterDays != null) {
            val threshold = System.currentTimeMillis() - (hubFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
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

    LaunchedEffect(farms.size) {
        if (kInput > farms.size && farms.isNotEmpty()) {
            kInput = farms.size.toFloat()
        }
    }

    val farmIcon = remember(context) {
        createFarmMarker(context)
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
                            icon = farmIcon
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
                        Icon(Icons.Default.PlayArrow, contentDescription = "Compute")
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
                    quickFilters = listOf("Last 7 Days", "Multi-Hub (K > 2)", "Large Farm Count (> 5)")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Last 7 Days" -> hubFilterDays = if (isSelected) 7 else null
                        "Multi-Hub (K > 2)" -> multiHubFilter = isSelected
                        "Large Farm Count (> 5)" -> largeFarmFilter = isSelected
                    }
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
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.selectHubForReuse(entry)
                                    showHistoryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${decodePoints(entry.farmPoints).size} farms · K=${entry.k}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
                                        .format(java.util.Date(entry.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "${decodePoints(entry.resultHubs).size} hubs",
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
}
