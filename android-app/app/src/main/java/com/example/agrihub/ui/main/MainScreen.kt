package com.example.agriflow.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.agriflow.data.local.RoomHistoryRepository
import com.example.agriflow.ui.main.components.OptInTopAppBar
import com.example.agriflow.ui.main.components.ServerSettingsDialog
import com.example.agriflow.ui.main.tabs.CarbonFootprintTab
import com.example.agriflow.ui.main.tabs.FreightPriceTab
import com.example.agriflow.ui.main.tabs.HubClusterTab
import com.example.agriflow.ui.main.tabs.YieldForecastTab
import com.example.agriflow.viewmodel.AgriFlowViewModel

/**
 * Builds the shared [AgriFlowViewModel], wiring in a [HistoryRepository]
 * backed by a Room database file scoped to the app's Application context.
 */
@Composable
fun rememberAgriFlowViewModel(): AgriFlowViewModel {
    val appContext = LocalContext.current.applicationContext
    return viewModel {
        AgriFlowViewModel(historyRepository = RoomHistoryRepository(appContext))
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
