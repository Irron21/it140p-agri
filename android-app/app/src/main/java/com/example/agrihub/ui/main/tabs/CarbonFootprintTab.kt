package com.example.agriflow.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
import kotlin.math.max
import kotlin.math.round

@Composable
fun TreeVisual(treeCount: Double) {
    val fullTrees = treeCount.toInt()
    val remainder = (treeCount - fullTrees).toFloat()
    
    // Limit display to a reasonable amount (e.g., 20) to avoid UI lag, 
    // but mention the total count if it's very high.
    val displayTrees = fullTrees.coerceAtMost(20)
    
    Column {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center
        ) {
            repeat(displayTrees) {
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp).padding(2.dp)
                )
            }
            if (remainder >= 0.1 && displayTrees < 20) {
                Box(modifier = Modifier.size(28.dp).padding(2.dp)) {
                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                clip = true
                                shape = object : Shape {
                                    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                                        return Outline.Rectangle(Rect(0f, 0f, size.width * remainder, size.height))
                                    }
                                }
                            }
                    )
                    // Dimmed background of the tree to show it's partial
                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32).copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        if (fullTrees > 20) {
            Text(
                text = String.format(Locale.US, "+ %d more trees needed", fullTrees - 20),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CarbonFootprintTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("150.0") }
    var weightInput by remember { mutableStateOf("8.4") }
    var efficiencyInput by remember { mutableStateOf("62.0") }
    
    val carbonState by viewModel.carbonState.collectAsStateWithLifecycle()
    val carbonHistory by viewModel.carbonHistory.collectAsStateWithLifecycle()
    val pendingReuse by viewModel.pendingCarbonReuse.collectAsStateWithLifecycle()
    
    val scrollState = rememberScrollState()

    // Dialog State
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Filter States
    var carbonFilterDays by remember { mutableStateOf<Int?>(null) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    var highEmissionFilter by remember { mutableStateOf(false) }
    var ecoFriendlyFilter by remember { mutableStateOf(false) }
    var sortByHighestCo2 by remember { mutableStateOf(false) }

    var co2Range by remember { mutableStateOf(0f..500f) }

    val filteredCarbonHistory = remember(carbonHistory, carbonFilterDays, startDateMillis, endDateMillis, highEmissionFilter, ecoFriendlyFilter, sortByHighestCo2, co2Range) {
        var list = carbonHistory
        if (carbonFilterDays != null) {
            val threshold = System.currentTimeMillis() - (carbonFilterDays!! * 24 * 60 * 60 * 1000L)
            list = list.filter { it.timestamp >= threshold }
        } else if (startDateMillis != null) {
            list = list.filter { it.timestamp >= startDateMillis!! && (endDateMillis == null || it.timestamp <= endDateMillis!! + 86400000L) }
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
        Pair("Cargo Container Ship", "8.0")
    )
    var selectedPresetName by remember { mutableStateOf(transportPresets[0].first) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky Header Row
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp),
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
                val efficiency = efficiencyInput.toDoubleOrNull() ?: -1.0
                val distance = distanceInput.toDoubleOrNull() ?: -1.0
                val weight = weightInput.toDoubleOrNull() ?: -1.0
                
                if (efficiency <= 0 || efficiency > 2000) {
                    viewModel.setCarbonError("Efficiency must be between 0.1 and 2,000 g/t-km")
                } else if (distance <= 0 || distance > 20000) {
                    viewModel.setCarbonError("Distance must be between 0.1 and 20,000 km")
                } else if (weight <= 0 || weight > 500) {
                    viewModel.setCarbonError("Weight must be between 0.1 and 500 Tons")
                } else {
                    viewModel.runCarbonFootprint(efficiency, distance, weight)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Carbon Output", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(carbonState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            val treesNeeded = data / 21.0
            
            val esgInfo = when {
                data < 50.0 -> Triple("Category A (Excellent)", "Highly ESG Compliant", Color(0xFF2E7D32))
                data < 100.0 -> Triple("Category B (Good)", "ESG Compliant", Color(0xFF4CAF50))
                data < 250.0 -> Triple("Category C (Fair)", "ESG Review Recommended", Color(0xFFFFA000))
                else -> Triple("Category D (Poor)", "Non-ESG Compliant", Color(0xFFD32F2F))
            }

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
                            Text("Estimated CO₂ Footprint:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f kg CO₂", data),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Surface(
                                color = esgInfo.third.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (data < 250.0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = esgInfo.third,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = esgInfo.first,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = esgInfo.third
                                    )
                                }
                            }
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
                                text = "Environmental & ESG Compliance Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Tree Absorption Equivalency:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            TreeVisual(treesNeeded)
                            
                            Text(
                                text = String.format(Locale.US, "This emission requires %.2f mature trees to offset annually.", treesNeeded),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "ESG Compliance Status:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = esgInfo.second,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = esgInfo.third,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Text(
                                text = when {
                                    data < 100.0 -> "Your logistics efficiency is within sustainable limits. Minimal environmental remediation required."
                                    data < 250.0 -> "Emissions are approaching high levels. Consider route optimization or fuel-efficient transport alternatives."
                                    else -> "Immediate ESG intervention recommended. High carbon density detected for this freight volume."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Note: A mature tree offsets approximately 21 kg of CO₂ per year. ESG categories are based on carbon density per freight ton-kilometer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                lineHeight = 14.sp
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
            title = "Carbon Footprint History",
            onDismiss = { showHistoryDialog = false }
        ) {
            GenericHistoryScreen(
                historyItems = filteredCarbonHistory,
                filterConfig = HistoryFilterConfig(
                    quickFilters = listOf("Today", "Last 7 Days", "Last 30 Days", "High Emission (> 100kg)", "Eco Friendly (< 10kg)", "Sort: Highest CO2")
                ),
                onFilterToggled = { label, isSelected ->
                    when (label) {
                        "Today" -> {
                            carbonFilterDays = if (isSelected) 1 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 7 Days" -> {
                            carbonFilterDays = if (isSelected) 7 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "Last 30 Days" -> {
                            carbonFilterDays = if (isSelected) 30 else null
                            startDateMillis = null
                            endDateMillis = null
                        }
                        "High Emission (> 100kg)" -> highEmissionFilter = isSelected
                        "Eco Friendly (< 10kg)" -> ecoFriendlyFilter = isSelected
                        "Sort: Highest CO2" -> sortByHighestCo2 = isSelected
                    }
                },
                onDateRangeSelected = { start, end ->
                    startDateMillis = start
                    endDateMillis = end
                    if (start != null) carbonFilterDays = null
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
