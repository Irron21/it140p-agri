package com.example.agriflow.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.agriflow.network.Coordinate
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgriFlowViewModel = viewModel { AgriFlowViewModel() }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val endpointUrl by viewModel.endpointUrl.collectAsStateWithLifecycle()
    val fuelApiUrl by viewModel.fuelApiUrl.collectAsStateWithLifecycle()

    Scaffold(
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
            
            // Server Settings Modal Dialog
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
                Spacer(modifier = Modifier.height(8.dp))
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
@Composable
fun YieldForecastTab(viewModel: AgriFlowViewModel) {
    var areaInput by remember { mutableStateOf("120.5") }
    var tempInput by remember { mutableFloatStateOf(25.0f) }
    var phInput by remember { mutableFloatStateOf(6.5f) }
    
    val yieldState by viewModel.yieldState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardHeader(
            title = "Yield Forecasting Model",
            subtitle = "Predict farm productivity based on land area and environmental inputs."
        )

        OutlinedTextField(
            value = areaInput,
            onValueChange = { areaInput = it },
            label = { Text("Cultivation Area (Hectares)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Temperature Slider + Label
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mean Temperature", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(String.format(Locale.US, "%.1f °C", tempInput), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = tempInput,
                onValueChange = { tempInput = it },
                valueRange = 0.0f..50.0f,
                steps = 50
            )
        }

        // pH Slider + Label
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Soil pH Level", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(String.format(Locale.US, "%.1f pH", phInput), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = phInput,
                onValueChange = { phInput = it },
                valueRange = 3.0f..10.0f,
                steps = 70
            )
        }

        Button(
            onClick = {
                val area = areaInput.toDoubleOrNull() ?: 0.0
                viewModel.runYieldForecast(area, tempInput.toDouble(), phInput.toDouble())
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "System Insights & Agronomy Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val insights = buildList {
                                val tempVal = tempInput.toDouble()
                                val phVal = phInput.toDouble()
                                if (tempVal > 30.0) {
                                    add("• Warning: Cultivation temperature exhibits high thermal stress, potentially reducing maximum yield.")
                                } else if (tempVal < 20.0) {
                                    add("• Warning: Low cultivation temperature exhibits thermal stress, which may delay crop growth stages.")
                                }
                                if (phVal < 6.0) {
                                    add("• Warning: Soil pH indicates high acidity, which may restrict root development and nutrient uptake.")
                                }
                                if (tempVal in 20.0..30.0 && phVal >= 6.0) {
                                    add("• System Insight: Soil pH and climate temperature are within optimal range for target crop cultivation.")
                                }
                            }
                            
                            insights.forEach { insight ->
                                Text(
                                    text = insight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Freight Price Screen Tab ---
@Composable
fun FreightPriceTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("380.0") }
    var fuelPriceInput by remember { mutableFloatStateOf(60.00f) }
    var weightInput by remember { mutableStateOf("18.5") }
    
    val freightState by viewModel.freightState.collectAsStateWithLifecycle()
    val fuelPriceState by viewModel.fuelPriceState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // When a live REST fuel price fetch succeeds, snap the slider to it (clamped to its range).
    LaunchedEffect(fuelPriceState) {
        val successState = fuelPriceState
        if (successState is SoapUiState.Success) {
            fuelPriceInput = successState.data.pricePerLiter.toFloat().coerceIn(40.00f, 100.00f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardHeader(
            title = "Freight Logistics Rate Calculator",
            subtitle = "Quote standard cargo transport rates depending on distance and weight."
        )

        OutlinedTextField(
            value = distanceInput,
            onValueChange = { distanceInput = it },
            label = { Text("Logistical Distance (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Freight Cargo Load Weight (Tons)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Fuel Price Slider
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fuel Market Price", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(String.format(Locale.US, "₱ %.2f / Liter", fuelPriceInput), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = fuelPriceInput,
                onValueChange = { fuelPriceInput = it },
                valueRange = 40.00f..100.00f,
                steps = 600
            )

            OutlinedButton(
                onClick = { viewModel.fetchLiveFuelPrice() },
                enabled = fuelPriceState !is SoapUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                if (fuelPriceState is SoapUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching from GasWatch PH…", fontSize = 13.sp)
                } else {
                    Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch Live Diesel Price (REST)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Live price fetch result / error caption
            when (val state = fuelPriceState) {
                is SoapUiState.Success -> {
                    val info = state.data
                    val statusLabel = when (info.cacheStatus) {
                        "live" -> "Live"
                        "cached" -> "Cached"
                        "stale-cache" -> "Cached (stale)"
                        "fallback" -> "Default (offline)"
                        else -> info.cacheStatus
                    }
                    Text(
                        text = buildString {
                            append("$statusLabel · GasWatch PH")
                            if (info.asOfText != null) append(" · as of ${info.asOfText}")
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is SoapUiState.Error -> {
                    Text(
                        text = "REST fetch failed: ${state.message}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                else -> Unit
            }
        }

        Button(
            onClick = {
                val distance = distanceInput.toDoubleOrNull() ?: 0.0
                val weight = weightInput.toDoubleOrNull() ?: 0.0
                viewModel.runFreightPrice(distance, fuelPriceInput.toDouble(), weight)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Compute SOAP Freight Cost", fontWeight = FontWeight.SemiBold)
        }

        StateDisplay(freightState) { data ->
            var isExpanded by remember { mutableStateOf(false) }
            val distance = distanceInput.toDoubleOrNull() ?: 0.0
            val weight = weightInput.toDoubleOrNull() ?: 0.0
            val fuelPrice = fuelPriceInput.toDouble()

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
                            Spacer(modifier = Modifier.height(8.dp))
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
}

// Helper to create custom circular markers for hubs
private fun createCircularSymbolMarker(context: android.content.Context, color: Int, symbol: String): android.graphics.drawable.BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).toInt() // 36dp
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
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
    
    // Draw symbol (Text)
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        setColor(android.graphics.Color.WHITE)
        textSize = 18 * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(symbol, size / 2f, yPos, textPaint)
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// --- 3. Hub Clustering Screen Tab ---
@Composable
fun HubClusterTab(viewModel: AgriFlowViewModel) {
    var farms by remember { mutableStateOf(listOf<GeoPoint>()) }
    var kInput by remember { mutableFloatStateOf(2.0f) }
    
    val hubState by viewModel.hubState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Prepare tinted standard osmdroid marker drawables
    val farmIcon = remember(context) {
        context.getDrawable(org.osmdroid.library.R.drawable.marker_default)?.mutate()?.apply {
            setTint(android.graphics.Color.BLUE)
        }
    }
    val hubIcon = remember(context) {
        createCircularSymbolMarker(context, android.graphics.Color.RED, "★")
    }

    // Initialize MapView with remember to survive recomposition
    val mapView = remember {
        MapView(context).apply {
            org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
            setMultiTouchControls(true)
            setBuiltInZoomControls(false) // Disable built-in zoom buttons
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(14.2778, 121.1250)) // Cabuyao City center
        }
    }

    // Handle MapView Lifecycle events to prevent memory leaks
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

    // Set up Map Tap Event Listener Overlay
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardHeader(
            title = "Logistics Hub Clustering (K-Means)",
            subtitle = "Tap on the map below to drop farm coordinates. Then run the clustering engine to calculate optimal logistics hubs (stars)."
        )

        // K Slider
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
                valueRange = 1.0f..5.0f,
                steps = 3
            )
        }

        // Map Section (Forces Map to fill remaining available space)
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

                    // Add Farm Markers (Blue)
                    farms.forEachIndexed { index, farm ->
                        val marker = Marker(map).apply {
                            position = farm
                            icon = farmIcon
                            infoWindow = null // Disable click info popup for farm pins
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(marker)
                    }

                    // Add Hub Centroid Markers (Red + Clickable InfoWindow)
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

            // Map Overlays Buttons (Top End)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear button
                FilledTonalButton(
                    onClick = {
                        farms = emptyList()
                        viewModel.resetStates()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Clear Map", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            // Bottom End Controls Column (Zoom & Calculate)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Zoom In
                SmallFloatingActionButton(
                    onClick = { mapView.controller.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { mapView.controller.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calculate Floating Action Button
                FloatingActionButton(
                    onClick = {
                        if (farms.isNotEmpty()) {
                            val lats = farms.map { it.latitude }
                            val lngs = farms.map { it.longitude }
                            viewModel.runHubCluster(lats, lngs, kInput.toInt())
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
}

// --- 4. Carbon Footprint Screen Tab ---
@Composable
fun CarbonFootprintTab(viewModel: AgriFlowViewModel) {
    var distanceInput by remember { mutableStateOf("150.0") }
    var weightInput by remember { mutableStateOf("8.4") }
    var efficiencyInput by remember { mutableStateOf("62.0") }
    
    val carbonState by viewModel.carbonState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Transport Types Dropdown Presets
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
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardHeader(
            title = "Carbon Footprint Assessment",
            subtitle = "Estimate CO₂ emission logs using weight and transport parameters."
        )

        OutlinedTextField(
            value = distanceInput,
            onValueChange = { distanceInput = it },
            label = { Text("Logistics Delivery Distance (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Cargo Weight (Tons)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Efficiency preset select dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedPresetName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Transport Mode Preset") },
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
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = efficiencyInput,
            onValueChange = { efficiencyInput = it },
            label = { Text("Efficiency Coefficient (gCO₂ / Ton-km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

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
            val treeOffsetCount = Math.max(1, Math.round(data / 21.0).toInt())

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
                            Spacer(modifier = Modifier.height(8.dp))
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
}

// --- Common UI Components ---

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
fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
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
