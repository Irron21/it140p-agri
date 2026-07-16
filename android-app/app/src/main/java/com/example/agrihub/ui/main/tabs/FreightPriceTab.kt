package com.example.agriflow.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FreightPriceTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("380.0") }
    var fuelPriceInput by remember { mutableStateOf("60.00") }
    var weightInput by remember { mutableStateOf("18.5") }

    val freightState by viewModel.freightState.collectAsStateWithLifecycle()
    val baseFare by viewModel.baseFreightFare.collectAsStateWithLifecycle()
    val fuelPriceState by viewModel.fuelPriceState.collectAsStateWithLifecycle()
    val freightHistory by viewModel.freightHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingFreightReuse.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showBaseFareDialog by remember { mutableStateOf(false) }

    // Filter States
    var freightFilterDays by remember { mutableStateOf<Int?>(null) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    var minDistanceFilter by remember { mutableStateOf<Double?>(null) }
    var heavyLoadFilter by remember { mutableStateOf(false) }
    var sortByHighestCost by remember { mutableStateOf(false) }

    var distanceRange by remember { mutableStateOf(0f..1000f) }
    var weightRange by remember { mutableStateOf(0f..50f) }

    val filteredFreightHistory = remember(freightHistory, freightFilterDays, startDateMillis, endDateMillis, minDistanceFilter, heavyLoadFilter, sortByHighestCost, distanceRange, weightRange) {
        var list = freightHistory
        if (freightFilterDays != null) {
            val threshold = System.currentTimeMillis() - (freightFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        } else if (startDateMillis != null) {
            list = list.filter { it.timestamp >= startDateMillis!! && (endDateMillis == null || it.timestamp <= endDateMillis!! + 86400000L) }
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
            Box(modifier = Modifier.weight(1f)) {
                CardHeader(
                    title = "Freight Logistics Rate Calculator",
                    subtitle = "Quote standard cargo transport rates."
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showBaseFareDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit Base Fare",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showHistoryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Scrollable Body Section
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                val distance = distanceInput.toDoubleOrNull() ?: -1.0
                val fuelPrice = fuelPriceInput.toDoubleOrNull() ?: -1.0
                val weight = weightInput.toDoubleOrNull() ?: -1.0

                if (distance <= 0 || distance > 5000) {
                    viewModel.setFreightError("Distance must be between 0.1 and 5,000 km")
                } else if (fuelPrice <= 0 || fuelPrice > 200) {
                    viewModel.setFreightError("Fuel price must be between 0.1 and 200 ₱/L")
                } else if (weight <= 0 || weight > 100) {
                    viewModel.setFreightError("Weight must be between 0.1 and 100 Tons")
                } else {
                    viewModel.runFreightPrice(distance, fuelPrice, weight)
                }
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

            val fuelCost = (distance / 3.5) * fuelPrice
            val cargoFee = 1.50 * weight * distance

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
                            Text("Estimated Freight Quote:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "₱ %,.2f PHP", data),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "View Breakdown",
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
                                text = "Itemized Logistics Receipt",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Base Dispatcher Fare:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "PHP %,.2f", baseFare), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Fuel Surcharge Cost:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "PHP %,.2f", fuelCost), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• Cargo Weight Fee:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "PHP %,.2f", cargoFee), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Calculated Cost:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(String.format(Locale.US, "PHP %,.2f", baseFare + fuelCost + cargoFee), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showBaseFareDialog) {
        var tempBaseFare by remember { mutableStateOf(baseFare.toString()) }
        AlertDialog(
            onDismissRequest = { showBaseFareDialog = false },
            title = { Text("Configure Base Dispatcher Fare") },
            text = {
                Column {
                    Text("This amount is the flat starting rate applied to every freight quote before fuel and weight surcharges.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempBaseFare,
                        onValueChange = { tempBaseFare = it },
                        label = { Text("Base Fare (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    tempBaseFare.toDoubleOrNull()?.let {
                        viewModel.updateBaseFreightFare(it)
                    }
                    showBaseFareDialog = false
                }) {
                    Text("Update Fare")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBaseFareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHistoryDialog) {
        HistoryModalDialog(
            title = "Freight Quote History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredFreightHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Today", "Last 7 Days", "Last 30 Days", "High Distance (> 500km)", "Heavy Load (> 20t)", "Sort: Highest Cost")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Today" -> {
                            freightFilterDays = if (isSelected) 1 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 7 Days" -> {
                            freightFilterDays = if (isSelected) 7 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 30 Days" -> {
                            freightFilterDays = if (isSelected) 30 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "High Distance (> 500km)" -> minDistanceFilter = if (isSelected) 500.0 else null
                        "Heavy Load (> 20t)" -> heavyLoadFilter = isSelected
                        "Sort: Highest Cost" -> sortByHighestCost = isSelected
                    }
                },
                onDateRangeSelected = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                    if (start != null) freightFilterDays = null
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
