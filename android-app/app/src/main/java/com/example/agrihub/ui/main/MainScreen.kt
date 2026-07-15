package com.example.agriflow.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.agriflow.data.local.HistoryRepository
import com.example.agriflow.data.local.decodePoints
import com.example.agriflow.ui.components.GenericHistoryScreen
import com.example.agriflow.ui.components.HistoryFilterConfig
import com.example.agriflow.ui.components.HistoryModalDialog
import com.example.agriflow.network.Coordinate
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

/**
 * Builds the shared [AgriFlowViewModel], wiring in a [HistoryRepository]
 * backed by a Room database file scoped to the app's Application context.
 */
@Composable
fun rememberAgriFlowViewModel(): AgriFlowViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel {
        AgriFlowViewModel(historyRepository = HistoryRepository(appContext))
    }
}

@Suppress("UNUSED_PARAMETER", "AssignedValueIsNeverRead")
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgriFlowViewModel = rememberAgriFlowViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val endpointUrl by viewModel.endpointUrl.collectAsStateWithLifecycle()
    val fuelApiUrl by viewModel.fuelApiUrl.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            OptInTopAppBar(
                title = {
                    Text(
                        text = "Agricultural Logistics Hub",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Server Endpoint URL",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Agriculture, contentDescription = "Yield Forecast") },
                    label = { Text("Yield", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = "Freight Price") },
                    label = { Text("Freight", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Warehouse, contentDescription = "Hub Clustering") },
                    label = { Text("Hubs", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Eco, contentDescription = "Carbon Footprint") },
                    label = { Text("Carbon", fontSize = 11.sp) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> YieldForecastTab(viewModel)
                1 -> FreightPriceTab(viewModel)
                2 -> HubClusterTab(viewModel)
                3 -> CarbonFootprintTab(viewModel)
            }
            
            if (showSettingsDialog) {
                ServerSettingsDialog(
                    currentSoapUrl = endpointUrl,
                    currentFuelApiUrl = fuelApiUrl,
                    onSave = { newSoapUrl, newFuelApiUrl ->
                        viewModel.updateEndpointUrl(newSoapUrl)
                        viewModel.updateFuelApiUrl(newFuelApiUrl)
                        showSettingsDialog = false
                    },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptInTopAppBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors
) {
    TopAppBar(
        title = title,
        actions = actions,
        colors = colors
    )
}

@Composable
fun ServerSettingsDialog(
    currentSoapUrl: String,
    currentFuelApiUrl: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var soapUrlText by remember { mutableStateOf(currentSoapUrl) }
    var fuelApiUrlText by remember { mutableStateOf(currentFuelApiUrl) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Backend Connection Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure the backend target endpoints. For local emulators, use 10.0.2.2 instead of localhost.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SOAP (WSDL)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = soapUrlText,
                    onValueChange = { soapUrlText = it },
                    label = { Text("SOAP server.php Endpoint URL") },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "REST (JSON)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fuelApiUrlText,
                    onValueChange = { fuelApiUrlText = it },
                    label = { Text("Live Fuel Price REST API URL") },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(soapUrlText, fuelApiUrlText) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Links", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// --- 1. Yield Forecast Screen Tab ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YieldForecastTab(viewModel: AgriFlowViewModel) {
    var areaInput by remember { mutableStateOf("120.5") }
    var tempInput by remember { mutableStateOf("25.0") }
    var phInput by remember { mutableStateOf("6.5") }
    
    val yieldState by viewModel.yieldState.collectAsStateWithLifecycle()
    val yieldHistory by viewModel.yieldHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingYieldReuse.collectAsStateWithLifecycle()
    
    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Filter States
    var filterDays by remember { mutableStateOf<Int?>(null) }
    var minYieldFilter by remember { mutableStateOf<Double?>(null) }
    var thermalStressFilter by remember { mutableStateOf(false) }
    var sortByHighestYield by remember { mutableStateOf(false) }

    var tempRange by remember { mutableStateOf(10f..40f) }
    var phRange by remember { mutableStateOf(3.0f..10.0f) }

    val filteredHistory = remember(yieldHistory, filterDays, minYieldFilter, thermalStressFilter, sortByHighestYield, tempRange, phRange) {
        var list = yieldHistory
        if (filterDays != null) {
            val threshold = System.currentTimeMillis() - (filterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        }
        if (minYieldFilter != null) {
            list = list.filter { it.resultTons >= minYieldFilter!! }
        }
        if (thermalStressFilter) {
            list = list.filter { it.temperature > 30.0 }
        }
        list = list.filter { it.temperature.toFloat() in tempRange && it.ph.toFloat() in phRange }
        if (sortByHighestYield) {
            list = list.sortedByDescending { it.resultTons }
        }
        list
    }

    LaunchedEffect(pendingReuse) {
        pendingReuse?.let { entry ->
            areaInput = entry.area.toString()
            tempInput = entry.temperature.toString()
            phInput = entry.ph.toString()
            viewModel.clearYieldReuse()
        }
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
                title = "Yield Forecasting Model",
                subtitle = "Predict farm productivity."
            )
            IconButton(onClick = { showHistoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = areaInput,
                    onValueChange = { 
                        areaInput = it
                        if (yieldState != SoapUiState.Idle) viewModel.resetYieldState()
                    },
                    label = { Text("Area (ha)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("Total land area in hectares used for crop cultivation. Larger areas generally scale total yield output.")
                    }
                )

                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { 
                        tempInput = it
                        if (yieldState != SoapUiState.Idle) viewModel.resetYieldState()
                    },
                    label = { Text("Temp (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("The average environmental temperature. Optimal ranges for most crops are between 20°C and 30°C.")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phInput,
                    onValueChange = { 
                        phInput = it
                        if (yieldState != SoapUiState.Idle) viewModel.resetYieldState()
                    },
                    label = { Text("Soil pH") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("Measures soil acidity or alkalinity. Most crops thrive in slightly acidic to neutral soil (pH 6.0 - 7.5).")
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Button(
            onClick = {
                val area = areaInput.toDoubleOrNull() ?: 0.0
                val temp = tempInput.toDoubleOrNull() ?: 0.0
                val ph = phInput.toDoubleOrNull() ?: 0.0
                viewModel.runYieldForecast(area, temp, ph)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Yield Forecast", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(yieldState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Forecasted Yield Output:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f metric tons", data),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "View Analysis",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "System Insights & Agronomy Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val tempVal = tempInput.toDoubleOrNull() ?: 0.0
                            val phVal = phInput.toDoubleOrNull() ?: 0.0

                            val insightLines = buildList {
                                if (tempVal > 30.0) {
                                    add(buildAnnotatedInsight("Warning", ": Cultivation temperature exhibits high ", "thermal stress", ", potentially reducing maximum yield."))
                                } else if (tempVal < 20.0) {
                                    add(buildAnnotatedInsight("Warning", ": Low cultivation temperature exhibits ", "thermal stress", ", which may delay crop growth stages."))
                                }
                                if (phVal < 6.0) {
                                    add(buildAnnotatedInsight("Warning", ": Soil pH indicates high acidity, which may restrict root development."))
                                }
                                if (tempVal in 20.0..30.0 && phVal >= 6.0) {
                                    add(buildAnnotatedInsight("System Insight", ": Soil pH and climate temperature are within ", "optimal range", " for cultivation."))
                                }
                            }
                            
                            insightLines.forEach { annotatedString ->
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryModalDialog(
            title = "Yield Forecast History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Last 7 Days", "High Yield (> 500t)", "Thermal Stress (> 30°C)", "Sort: Highest Yield")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Last 7 Days" -> filterDays = if (isSelected) 7 else null
                        "High Yield (> 500t)" -> minYieldFilter = if (isSelected) 500.0 else null
                        "Thermal Stress (> 30°C)" -> thermalStressFilter = isSelected
                        "Sort: Highest Yield" -> sortByHighestYield = isSelected
                    }
                },
                onAdvancedFilterSave = { },
                advancedFilterContent = {
                    Text("Environmental Ranges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Temperature: ${tempRange.start.toInt()}°C - ${tempRange.endInclusive.toInt()}°C", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = tempRange,
                        onValueChange = { tempRange = it },
                        valueRange = 0f..50f,
                        steps = 50
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Soil pH: ${String.format("%.1f", phRange.start)} - ${String.format("%.1f", phRange.endInclusive)}", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = phRange,
                        onValueChange = { phRange = it },
                        valueRange = 0f..14f,
                        steps = 140
                    )
                },
                itemContent = { entry ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.selectYieldForReuse(entry)
                                    showHistoryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "%.1f ha · %.1f°C · pH %.1f", entry.area, entry.temperature, entry.ph),
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
                                text = String.format(Locale.US, "%.2f t", entry.resultTons),
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

@Composable
fun buildAnnotatedInsight(prefix: String, mid: String, highlight: String = "", suffix: String = ""): AnnotatedString {
    val warningColor = Color(0xFFE57373) // Gentle Red
    val optimalColor = Color(0xFF2E7D32) // Green
    val insightColor = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        val color = when (prefix) {
            "Warning" -> warningColor
            "System Insight" -> insightColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("• $prefix")
        }
        append(mid)
        if (highlight.isNotEmpty()) {
            val hColor = if (highlight.contains("optimal", ignoreCase = true)) optimalColor else color
            withStyle(style = SpanStyle(color = hColor, fontWeight = FontWeight.Bold)) {
                append(highlight)
            }
        }
        append(suffix)
    }
}

// --- 2. Freight Price Screen Tab ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FreightPriceTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("380.0") }
    var fuelPriceInput by remember { mutableStateOf("60.00") }
    var weightInput by remember { mutableStateOf("18.5") }
    
    val freightState by viewModel.freightState.collectAsStateWithLifecycle()
    val fuelPriceState by viewModel.fuelPriceState.collectAsStateWithLifecycle()
    val freightHistory by viewModel.freightHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingFreightReuse.collectAsStateWithLifecycle()
    
    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Filter States
    var freightFilterDays by remember { mutableStateOf<Int?>(null) }
    var minDistanceFilter by remember { mutableStateOf<Double?>(null) }
    var heavyLoadFilter by remember { mutableStateOf(false) }
    var sortByHighestCost by remember { mutableStateOf(false) }

    var distanceRange by remember { mutableStateOf(0f..1000f) }
    var weightRange by remember { mutableStateOf(0f..50f) }

    val filteredFreightHistory = remember(freightHistory, freightFilterDays, minDistanceFilter, heavyLoadFilter, sortByHighestCost, distanceRange, weightRange) {
        var list = freightHistory
        if (freightFilterDays != null) {
            val threshold = System.currentTimeMillis() - (freightFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        }
        if (minDistanceFilter != null) {
            list = list.filter { it.distance >= minDistanceFilter!! }
        }
        if (heavyLoadFilter) {
            list = list.filter { it.weight > 20.0 }
        }
        list = list.filter { it.distance.toFloat() in distanceRange && it.weight.toFloat() in weightRange }
        if (sortByHighestCost) {
            list = list.sortedByDescending { it.resultCost }
        }
        list
    }

    LaunchedEffect(pendingReuse) {
        pendingReuse?.let { entry ->
            distanceInput = entry.distance.toString()
            fuelPriceInput = entry.fuelPrice.toString()
            weightInput = entry.weight.toString()
            viewModel.clearFreightReuse()
        }
    }

    LaunchedEffect(fuelPriceState) {
        val successState = fuelPriceState
        if (successState is SoapUiState.Success) {
            fuelPriceInput = String.format(Locale.US, "%.2f", successState.data.pricePerLiter)
        }
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
                title = "Freight Logistics Rate Calculator",
                subtitle = "Quote standard cargo transport rates."
            )
            IconButton(onClick = { showHistoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { 
                        distanceInput = it
                        if (freightState != SoapUiState.Idle) viewModel.resetFreightState()
                    },
                    label = { Text("Dist. (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("The total travel distance in kilometers between origin and destination.")
                    }
                )

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { 
                        weightInput = it
                        if (freightState != SoapUiState.Idle) viewModel.resetFreightState()
                    },
                    label = { Text("Weight (Tons)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("The total weight of the freight cargo in metric tons.")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = fuelPriceInput,
                    onValueChange = { 
                        fuelPriceInput = it
                        if (freightState != SoapUiState.Idle) viewModel.resetFreightState()
                    },
                    label = { Text("Fuel (₱/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            InfoTooltip("Current price of diesel per liter. Affects the fuel surcharge cost.")
                            IconButton(
                                onClick = { viewModel.fetchLiveFuelPrice() },
                                enabled = fuelPriceState !is SoapUiState.Loading,
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (fuelPriceState is SoapUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh, 
                                        contentDescription = "Fetch Live Price",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                
                // Status tag beside the input
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    when (val state = fuelPriceState) {
                        is SoapUiState.Success -> {
                            val info = state.data
                            val statusLabel = when (info.cacheStatus) {
                                "live" -> "Live"
                                "cached" -> "Cached"
                                "stale-cache" -> "Cached (stale)"
                                "fallback" -> "Default"
                                else -> info.cacheStatus
                            }
                            Text(
                                text = buildString {
                                    append("$statusLabel · GasWatch PH")
                                    if (info.asOfText != null) append("\nas of ${info.asOfText}")
                                },
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        is SoapUiState.Error -> {
                            Text(
                                text = "Fetch failed:\n${state.message}",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        Button(
            onClick = {
                val distance = distanceInput.toDoubleOrNull() ?: 0.0
                val fuelPrice = fuelPriceInput.toDoubleOrNull() ?: 0.0
                val weight = weightInput.toDoubleOrNull() ?: 0.0
                viewModel.runFreightPrice(distance, fuelPrice, weight)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Freight Cost", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(freightState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            val distance = distanceInput.toDoubleOrNull() ?: 0.0
            val weight = weightInput.toDoubleOrNull() ?: 0.0
            val fuelPrice = fuelPriceInput.toDoubleOrNull() ?: 0.0

            val baseFare = 2500.00
            val fuelCost = (distance / 3.5) * fuelPrice
            val cargoFee = 1.50 * weight * distance

            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated Freight Quote:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "₱ %,.2f PHP", data),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { isExpanded = !isExpanded }) {
                            Text(
                                text = if (isExpanded) "Hide Cost" else "Cost Breakdown",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Itemized Logistics Receipt",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Base Dispatcher Fare:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "₱ %,.2f", baseFare), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Fuel Surcharge Cost:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "₱ %,.2f", fuelCost), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Cargo Weight Fee:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "₱ %,.2f", cargoFee), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Calculated Cost:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(String.format(Locale.US, "₱ %,.2f", baseFare + fuelCost + cargoFee), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryModalDialog(
            title = "Freight Quote History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredFreightHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Last 7 Days", "High Distance (> 500km)", "Heavy Load (> 20t)", "Sort: Highest Cost")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Last 7 Days" -> freightFilterDays = if (isSelected) 7 else null
                        "High Distance (> 500km)" -> minDistanceFilter = if (isSelected) 500.0 else null
                        "Heavy Load (> 20t)" -> heavyLoadFilter = isSelected
                        "Sort: Highest Cost" -> sortByHighestCost = isSelected
                    }
                },
                onAdvancedFilterSave = { },
                advancedFilterContent = {
                    Text("Logistics Ranges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Distance: ${distanceRange.start.toInt()}km - ${distanceRange.endInclusive.toInt()}km", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = distanceRange,
                        onValueChange = { distanceRange = it },
                        valueRange = 0f..1000f,
                        steps = 100
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Cargo Weight: ${weightRange.start.toInt()}t - ${weightRange.endInclusive.toInt()}t", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = weightRange,
                        onValueChange = { weightRange = it },
                        valueRange = 0f..50f,
                        steps = 50
                    )
                },
                itemContent = { entry ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.selectFreightForReuse(entry)
                                    showHistoryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "%.0f km · ₱%.2f/L · %.1f t", entry.distance, entry.fuelPrice, entry.weight),
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
                                text = String.format(Locale.US, "₱%,.2f", entry.resultCost),
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

// --- 3. Hub Clustering Screen Tab ---
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

// --- 4. Carbon Footprint Screen Tab ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CarbonFootprintTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("150.0") }
    var weightInput by remember { mutableStateOf("8.4") }
    var efficiencyInput by remember { mutableStateOf("62.0") }
    
    val carbonState by viewModel.carbonState.collectAsStateWithLifecycle()
    val carbonHistory by viewModel.carbonHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingCarbonReuse.collectAsStateWithLifecycle()
    
    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Filter States
    var carbonFilterDays by remember { mutableStateOf<Int?>(null) }
    var highEmissionFilter by remember { mutableStateOf(false) }
    var ecoFriendlyFilter by remember { mutableStateOf(false) }
    var sortByHighestCo2 by remember { mutableStateOf(false) }

    var co2Range by remember { mutableStateOf(0f..500f) }

    val filteredCarbonHistory = remember(carbonHistory, carbonFilterDays, highEmissionFilter, ecoFriendlyFilter, sortByHighestCo2, co2Range) {
        var list = carbonHistory
        if (carbonFilterDays != null) {
            val threshold = System.currentTimeMillis() - (carbonFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        }
        if (highEmissionFilter) {
            list = list.filter { it.resultCo2 > 100.0 }
        }
        if (ecoFriendlyFilter) {
            list = list.filter { it.resultCo2 < 10.0 }
        }
        list = list.filter { it.resultCo2.toFloat() in co2Range }
        if (sortByHighestCo2) {
            list = list.sortedByDescending { it.resultCo2 }
        }
        list
    }

    LaunchedEffect(pendingReuse) {
        pendingReuse?.let { entry ->
            distanceInput = entry.distance.toString()
            weightInput = entry.weight.toString()
            efficiencyInput = entry.efficiency.toString()
            viewModel.clearCarbonReuse()
        }
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    val transportPresets = listOf(
        Pair("Truck (Heavy Cargo)", "62.0"),
        Pair("Freight Train", "22.0"),
        Pair("Air Cargo plane", "560.0"),
        Pair("Cargo Container Ship", "8.0")
    )
    var selectedPresetName by remember { mutableStateOf(transportPresets[0].first) }

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
                title = "Carbon Footprint Assessment",
                subtitle = "Estimate CO₂ emission logs."
            )
            IconButton(onClick = { showHistoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { 
                        distanceInput = it
                        if (carbonState != SoapUiState.Idle) viewModel.resetCarbonState()
                    },
                    label = { Text("Dist. (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("The total travel distance in kilometers for the cargo delivery.")
                    }
                )

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { 
                        weightInput = it
                        if (carbonState != SoapUiState.Idle) viewModel.resetCarbonState()
                    },
                    label = { Text("Weight (Tons)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        InfoTooltip("Total weight of the cargo being transported in metric tons.")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Efficiency preset select dropdown
                Box(modifier = Modifier.weight(1.3f)) {
                    OutlinedTextField(
                        value = selectedPresetName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transport Preset") },
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        transportPresets.forEach { (name, coeff) ->
                            DropdownMenuItem(
                                text = { Text("$name ($coeff g/t-km)") },
                                onClick = {
                                    selectedPresetName = name
                                    efficiencyInput = coeff
                                    if (carbonState != SoapUiState.Idle) viewModel.resetCarbonState()
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = efficiencyInput,
                    onValueChange = { 
                        efficiencyInput = it
                        if (carbonState != SoapUiState.Idle) viewModel.resetCarbonState()
                    },
                    label = { Text("Coeff (g/t-km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.7f),
                    trailingIcon = {
                        InfoTooltip("Grams of CO₂ emitted per ton-kilometer. Depends heavily on transport mode.")
                    }
                )
            }
        }

        Button(
            onClick = {
                val efficiency = efficiencyInput.toDoubleOrNull() ?: 0.0
                val distance = distanceInput.toDoubleOrNull() ?: 0.0
                val weight = weightInput.toDoubleOrNull() ?: 0.0
                viewModel.runCarbonFootprint(efficiency, distance, weight)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Carbon Output", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(carbonState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            val treeOffsetCount = max(1, round(data / 21.0).toInt())

            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated CO₂ Footprint:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f kg CO₂", data),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "View Analysis",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Environmental Impact Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Dynamic Tree Absorption Equivalency:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "Equivalent to the annual carbon absorption of %d mature trees.", treeOffsetCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: A mature tree offsets approximately 21 kg of CO₂ per year. Minimizing transit distances directly preserves local forestry offset capacities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryModalDialog(
            title = "Carbon Footprint History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredCarbonHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Last 7 Days", "High Emission (> 100kg)", "Eco Friendly (< 10kg)", "Sort: Highest CO2")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Last 7 Days" -> carbonFilterDays = if (isSelected) 7 else null
                        "High Emission (> 100kg)" -> highEmissionFilter = isSelected
                        "Eco Friendly (< 10kg)" -> ecoFriendlyFilter = isSelected
                        "Sort: Highest CO2" -> sortByHighestCo2 = isSelected
                    }
                },
                onAdvancedFilterSave = { },
                advancedFilterContent = {
                    Text("Emission Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("CO₂ Impact: ${co2Range.start.toInt()}kg - ${co2Range.endInclusive.toInt()}kg", style = MaterialTheme.typography.bodySmall)
                    RangeSlider(
                        value = co2Range,
                        onValueChange = { co2Range = it },
                        valueRange = 0f..1000f,
                        steps = 100
                    )
                },
                itemContent = { entry ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.selectCarbonForReuse(entry)
                                    showHistoryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "%.0f km · %.1f t · %.1f gCO₂/t-km", entry.distance, entry.weight, entry.efficiency),
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
                                text = String.format(Locale.US, "%.2f kg", entry.resultCo2),
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

// --- Common UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(text: String) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(text = text, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
            }
        },
        state = rememberTooltipState()
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun CardHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun <T> StateDisplay(
    state: SoapUiState<T>,
    successContent: @Composable (T) -> Unit
) {
    AnimatedVisibility(
        visible = state != SoapUiState.Idle,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            when (state) {
                is SoapUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SoapUiState.Success -> {
                    successContent(state.data)
                }
                is SoapUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "SOAP Server Connection Failure",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    state.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                SoapUiState.Idle -> {
                    // Do nothing
                }
            }
        }
    }
}
