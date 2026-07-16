package com.example.agriflow.ui.main.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agriflow.ui.components.GenericHistoryScreen
import com.example.agriflow.ui.components.HistoryFilterConfig
import com.example.agriflow.ui.components.HistoryModalDialog
import com.example.agriflow.ui.main.components.CardHeader
import com.example.agriflow.ui.main.components.InfoTooltip
import com.example.agriflow.ui.main.components.StateDisplay
import com.example.agriflow.ui.main.components.buildAnnotatedInsight
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YieldForecastTab(viewModel: AgriFlowViewModel) {
    var areaInput by remember { mutableStateOf("120.5") }
    var tempInput by remember { mutableStateOf("25.0") }
    var phInput by remember { mutableStateOf("6.5") }
    
    val yieldState by viewModel.yieldState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val yieldHistory by viewModel.yieldHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingYieldReuse.collectAsStateWithLifecycle()
    
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

    // Filter States
    var filterDays by remember { mutableStateOf<Int?>(null) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    var minYieldFilter by remember { mutableStateOf<Double?>(null) }
    var thermalStressFilter by remember { mutableStateOf(false) }
    var sortByHighestYield by remember { mutableStateOf(false) }

    var tempRange by remember { mutableStateOf(10f..40f) }
    var phRange by remember { mutableStateOf(3.0f..10.0f) }

    val filteredHistory = remember(yieldHistory, filterDays, startDateMillis, endDateMillis, minYieldFilter, thermalStressFilter, sortByHighestYield, tempRange, phRange) {
        var list = yieldHistory
        if (filterDays != null) {
            val threshold = System.currentTimeMillis() - (filterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        } else if (startDateMillis != null) {
            list = list.filter { it.timestamp >= startDateMillis!! && (endDateMillis == null || it.timestamp <= endDateMillis!! + 86400000L) }
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

    LaunchedEffect(weatherState) {
        if (weatherState is SoapUiState.Success) {
            tempInput = String.format(Locale.US, "%.1f", (weatherState as SoapUiState.Success).data.temperature)
        }
    }

    // Sequence: Fetch Location -> Fetch Weather
    LaunchedEffect(locationState) {
        if (locationState is SoapUiState.Success) {
            val loc = (locationState as SoapUiState.Success).data
            viewModel.fetchLiveWeather(loc.latitude, loc.longitude)
            viewModel.resetLocationState()
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = areaInput,
                    onValueChange = { 
                        areaInput = it
                        if (yieldState != SoapUiState.Idle) viewModel.resetYieldState()
                    },
                    label = { Text("Area (ha)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    trailingIcon = {
                        InfoTooltip("Total land area in hectares used for crop cultivation. Larger areas generally scale total yield output.")
                    }
                )

                OutlinedTextField(
                    value = phInput,
                    onValueChange = { 
                        phInput = it
                        if (yieldState != SoapUiState.Idle) viewModel.resetYieldState()
                    },
                    label = { Text("Soil pH") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    trailingIcon = {
                        InfoTooltip("Measures soil acidity or alkalinity. Most crops thrive in slightly acidic to neutral soil (pH 6.0 - 7.5).")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            InfoTooltip("The average environmental temperature. Optimal ranges for most crops are between 20°C and 30°C.")
                            IconButton(
                                onClick = { 
                                    locationPermissionLauncher.launch(arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ))
                                },
                                enabled = weatherState !is SoapUiState.Loading && locationState !is SoapUiState.Loading,
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (weatherState is SoapUiState.Loading || locationState is SoapUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh, 
                                        contentDescription = "Fetch Live Weather",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                )

                // Weather status tag beside the input (matching FreightPriceTab layout)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    when (val state = weatherState) {
                        is SoapUiState.Success -> {
                            val info = state.data
                            Text(
                                text = buildString {
                                    append("${info.condition} · ${info.humidity}% Humidity")
                                    append("\nLat: ${String.format(Locale.US, "%.2f", info.lat)}, Lon: ${String.format(Locale.US, "%.2f", info.lon)}")
                                },
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        is SoapUiState.Error -> {
                            Text(
                                text = "Weather fetch failed:\n${state.message}",
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
                val area = areaInput.toDoubleOrNull() ?: -1.0
                val temp = tempInput.toDoubleOrNull() ?: -100.0
                val ph = phInput.toDoubleOrNull() ?: -1.0
                
                if (area <= 0 || area > 10000) {
                    viewModel.setYieldError("Area must be between 0.1 and 10,000 ha")
                } else if (temp < -10 || temp > 60) {
                    viewModel.setYieldError("Temperature must be between -10°C and 60°C")
                } else if (ph < 0 || ph > 14) {
                    viewModel.setYieldError("Soil pH must be between 0 and 14")
                } else {
                    viewModel.runYieldForecast(area, temp, ph)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Yield Forecast", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(yieldState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            val tempVal = tempInput.toDoubleOrNull() ?: 0.0
            val phVal = phInput.toDoubleOrNull() ?: 0.0
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                color = if (tempVal > 30.0 || tempVal < 20.0 || phVal < 6.0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "System Insights & Agronomy Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val tempVal = tempInput.toDoubleOrNull() ?: 0.0
                            val phVal = phInput.toDoubleOrNull() ?: 0.0

                            val insightLines = buildList {
                                if (tempVal > 30.0) {
                                    add(Pair("Warning", buildAnnotatedInsight("", "Cultivation temperature exhibits high ", "thermal stress", ", potentially reducing maximum yield.")))
                                } else if (tempVal < 20.0) {
                                    add(Pair("Warning", buildAnnotatedInsight("", "Low cultivation temperature exhibits ", "thermal stress", ", which may delay crop growth stages.")))
                                }
                                if (phVal < 6.0) {
                                    add(Pair("Warning", buildAnnotatedInsight("", "Soil pH indicates high acidity, which may restrict root development.")))
                                }
                                if (tempVal in 20.0..30.0 && phVal >= 6.0) {
                                    add(Pair("Optimal", buildAnnotatedInsight("", "Soil pH and climate temperature are within ", "optimal range", " for cultivation.")))
                                }
                            }
                            
                            insightLines.forEach { (type, annotatedString) ->
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = annotatedString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                    quickFilters = listOf("Today", "Last 7 Days", "Last 30 Days", "High Yield (> 500t)", "Thermal Stress (> 30°C)", "Sort: Highest Yield")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Today" -> {
                            filterDays = if (isSelected) 1 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 7 Days" -> {
                            filterDays = if (isSelected) 7 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 30 Days" -> {
                            filterDays = if (isSelected) 30 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "High Yield (> 500t)" -> minYieldFilter = if (isSelected) 500.0 else null
                        "Thermal Stress (> 30°C)" -> thermalStressFilter = isSelected
                        "Sort: Highest Yield" -> sortByHighestYield = isSelected
                    }
                },
                onDateRangeSelected = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                    if (start != null) filterDays = null
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
