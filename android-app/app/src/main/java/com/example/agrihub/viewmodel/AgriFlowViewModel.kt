package com.example.agriflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agriflow.data.local.CarbonFootprintEntity
import com.example.agriflow.data.local.FreightCostEntity
import com.example.agriflow.data.local.HistoryRepository
import com.example.agriflow.data.local.HubClusterEntity
import com.example.agriflow.data.local.YieldForecastEntity
import com.example.agriflow.data.local.decodePoints
import com.example.agriflow.data.local.encodePoints
import com.example.agriflow.network.Coordinate
import com.example.agriflow.network.FuelPriceInfo
import com.example.agriflow.network.FuelPriceRepository
import com.example.agriflow.network.SoapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sealed UI State class representing standard SOAP transaction states.
 */
sealed interface SoapUiState<out T> {
    object Idle : SoapUiState<Nothing>
    object Loading : SoapUiState<Nothing>
    data class Success<out T>(val data: T) : SoapUiState<T>
    data class Error(val message: String) : SoapUiState<Nothing>
}

/**
 * Shared ViewModel handling all operations for AgriFlow calculations.
 */
class AgriFlowViewModel(
    private val repository: SoapRepository = SoapRepository(),
    private val fuelPriceRepository: FuelPriceRepository = FuelPriceRepository(),
    private val historyRepository: HistoryRepository
) : ViewModel() {

    // SOAP Endpoint URL (Configurable by user in-app)
    private val _endpointUrl = MutableStateFlow("http://10.0.2.2:8000/server.php")
    val endpointUrl: StateFlow<String> = _endpointUrl.asStateFlow()

    // REST Fuel Price Endpoint URL (Configurable by user in-app)
    private val _fuelApiUrl = MutableStateFlow("http://10.0.2.2:8000/fuel-price-api.php")
    val fuelApiUrl: StateFlow<String> = _fuelApiUrl.asStateFlow()

    // --- 5. Live Fuel Price (REST) State ---
    private val _fuelPriceState = MutableStateFlow<SoapUiState<FuelPriceInfo>>(SoapUiState.Idle)
    val fuelPriceState: StateFlow<SoapUiState<FuelPriceInfo>> = _fuelPriceState.asStateFlow()

    // --- 1. Yield Forecast State ---
    private val _yieldState = MutableStateFlow<SoapUiState<Double>>(SoapUiState.Idle)
    val yieldState: StateFlow<SoapUiState<Double>> = _yieldState.asStateFlow()
    val yieldHistory: StateFlow<List<YieldForecastEntity>> =
        historyRepository.yieldHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _pendingYieldReuse = MutableStateFlow<YieldForecastEntity?>(null)
    val pendingYieldReuse: StateFlow<YieldForecastEntity?> = _pendingYieldReuse.asStateFlow()

    // --- 2. Freight Price State ---
    private val _freightState = MutableStateFlow<SoapUiState<Double>>(SoapUiState.Idle)
    val freightState: StateFlow<SoapUiState<Double>> = _freightState.asStateFlow()
    val freightHistory: StateFlow<List<FreightCostEntity>> =
        historyRepository.freightHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _pendingFreightReuse = MutableStateFlow<FreightCostEntity?>(null)
    val pendingFreightReuse: StateFlow<FreightCostEntity?> = _pendingFreightReuse.asStateFlow()

    // --- 3. Hub Clustering State ---
    private val _hubState = MutableStateFlow<SoapUiState<List<Coordinate>>>(SoapUiState.Idle)
    val hubState: StateFlow<SoapUiState<List<Coordinate>>> = _hubState.asStateFlow()
    val hubClusterHistory: StateFlow<List<HubClusterEntity>> =
        historyRepository.hubClusterHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _pendingHubReuse = MutableStateFlow<HubClusterEntity?>(null)
    val pendingHubReuse: StateFlow<HubClusterEntity?> = _pendingHubReuse.asStateFlow()

    // --- 4. Carbon Footprint State ---
    private val _carbonState = MutableStateFlow<SoapUiState<Double>>(SoapUiState.Idle)
    val carbonState: StateFlow<SoapUiState<Double>> = _carbonState.asStateFlow()
    val carbonHistory: StateFlow<List<CarbonFootprintEntity>> =
        historyRepository.carbonHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _pendingCarbonReuse = MutableStateFlow<CarbonFootprintEntity?>(null)
    val pendingCarbonReuse: StateFlow<CarbonFootprintEntity?> = _pendingCarbonReuse.asStateFlow()

    /**
     * Updates the target SOAP web service endpoint URL.
     */
    fun updateEndpointUrl(newUrl: String) {
        _endpointUrl.value = newUrl.trim()
    }

    /**
     * Updates the target REST fuel-price-api.php endpoint URL.
     */
    fun updateFuelApiUrl(newUrl: String) {
        _fuelApiUrl.value = newUrl.trim()
    }

    /**
     * Runs the REST transaction to fetch the current live diesel price.
     * A plain GET/JSON call -- deliberately lighter weight than the SOAP
     * transactions below, since fuel prices change often and don't need a
     * strict WSDL contract.
     */
    fun fetchLiveFuelPrice() {
        viewModelScope.launch {
            _fuelPriceState.value = SoapUiState.Loading
            fuelPriceRepository.fetchDieselPrice(_fuelApiUrl.value)
                .onSuccess { info ->
                    _fuelPriceState.value = SoapUiState.Success(info)
                }
                .onFailure { error ->
                    _fuelPriceState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown REST error")
                }
        }
    }

    /**
     * Runs the Yield Forecast SOAP transaction.
     */
    fun runYieldForecast(area: Double, temp: Double, ph: Double) {
        viewModelScope.launch {
            _yieldState.value = SoapUiState.Loading
            repository.calculateYieldForecast(_endpointUrl.value, area, temp, ph)
                .onSuccess { yieldVal ->
                    _yieldState.value = SoapUiState.Success(yieldVal)
                    historyRepository.saveYield(
                        YieldForecastEntity(
                            timestamp = System.currentTimeMillis(),
                            area = area,
                            temperature = temp,
                            ph = ph,
                            resultTons = yieldVal
                        )
                    )
                }
                .onFailure { error ->
                    _yieldState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown network error")
                }
        }
    }

    /**
     * Runs the Freight Price Quote SOAP transaction.
     */
    fun runFreightPrice(distance: Double, fuelPrice: Double, weight: Double) {
        viewModelScope.launch {
            _freightState.value = SoapUiState.Loading
            repository.calculateFreightPrice(_endpointUrl.value, distance, fuelPrice, weight)
                .onSuccess { priceVal ->
                    _freightState.value = SoapUiState.Success(priceVal)
                    historyRepository.saveFreight(
                        FreightCostEntity(
                            timestamp = System.currentTimeMillis(),
                            distance = distance,
                            fuelPrice = fuelPrice,
                            weight = weight,
                            resultCost = priceVal
                        )
                    )
                }
                .onFailure { error ->
                    _freightState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown network error")
                }
        }
    }

    /**
     * Runs the Hub Location Clustering SOAP transaction.
     */
    fun runHubCluster(latitudes: List<Double>, longitudes: List<Double>, k: Int) {
        viewModelScope.launch {
            _hubState.value = SoapUiState.Loading
            
            if (latitudes.size != longitudes.size) {
                _hubState.value = SoapUiState.Error("Latitude and Longitude counts do not match.")
                return@launch
            }
            if (latitudes.isEmpty() || k <= 0) {
                _hubState.value = SoapUiState.Error("Requires at least one coordinate and K > 0.")
                return@launch
            }

            repository.calculateHubCluster(_endpointUrl.value, latitudes, longitudes, k)
                .onSuccess { centroids ->
                    _hubState.value = SoapUiState.Success(centroids)
                    historyRepository.saveHubCluster(
                        HubClusterEntity(
                            timestamp = System.currentTimeMillis(),
                            farmPoints = encodePoints(latitudes.zip(longitudes)),
                            k = k,
                            resultHubs = encodePoints(centroids.map { it.latitude to it.longitude })
                        )
                    )
                }
                .onFailure { error ->
                    _hubState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown network error")
                }
        }
    }

    /**
     * Runs the Carbon Footprint SOAP transaction.
     */
    fun runCarbonFootprint(efficiency: Double, distance: Double, weight: Double) {
        viewModelScope.launch {
            _carbonState.value = SoapUiState.Loading
            repository.calculateCarbonFootprint(_endpointUrl.value, efficiency, distance, weight)
                .onSuccess { carbonVal ->
                    _carbonState.value = SoapUiState.Success(carbonVal)
                    historyRepository.saveCarbon(
                        CarbonFootprintEntity(
                            timestamp = System.currentTimeMillis(),
                            efficiency = efficiency,
                            distance = distance,
                            weight = weight,
                            resultCo2 = carbonVal
                        )
                    )
                }
                .onFailure { error ->
                    _carbonState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown network error")
                }
        }
    }

    /**
     * Tap-to-reuse: called when the user taps a history row. The tab
     * composable observes the corresponding `pendingXReuse` StateFlow via
     * LaunchedEffect, applies the values to its local input fields, then
     * calls the matching `clearXReuse()` so it isn't re-applied on the next
     * recomposition.
     */
    fun selectYieldForReuse(entry: YieldForecastEntity) { _pendingYieldReuse.value = entry }
    fun clearYieldReuse() { _pendingYieldReuse.value = null }

    fun selectFreightForReuse(entry: FreightCostEntity) { _pendingFreightReuse.value = entry }
    fun clearFreightReuse() { _pendingFreightReuse.value = null }

    fun selectHubForReuse(entry: HubClusterEntity) { _pendingHubReuse.value = entry }
    fun clearHubReuse() { _pendingHubReuse.value = null }

    fun selectCarbonForReuse(entry: CarbonFootprintEntity) { _pendingCarbonReuse.value = entry }
    fun clearCarbonReuse() { _pendingCarbonReuse.value = null }

    /**
     * Resets transaction states.
     */
    fun resetStates() {
        resetYieldState()
        resetFreightState()
        resetHubState()
        resetCarbonState()
        resetFuelPriceState()
    }

    fun resetYieldState() {
        _yieldState.value = SoapUiState.Idle
    }

    fun resetFreightState() {
        _freightState.value = SoapUiState.Idle
    }

    fun resetHubState() {
        _hubState.value = SoapUiState.Idle
    }

    fun resetCarbonState() {
        _carbonState.value = SoapUiState.Idle
    }

    fun resetFuelPriceState() {
        _fuelPriceState.value = SoapUiState.Idle
    }

    fun setYieldError(message: String) {
        _yieldState.value = SoapUiState.Error(message)
    }

    fun setFreightError(message: String) {
        _freightState.value = SoapUiState.Error(message)
    }

    fun setCarbonError(message: String) {
        _carbonState.value = SoapUiState.Error(message)
    }
}
