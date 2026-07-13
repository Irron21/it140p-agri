package com.example.agriflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agriflow.network.Coordinate
import com.example.agriflow.network.FuelPriceInfo
import com.example.agriflow.network.FuelPriceRepository
import com.example.agriflow.network.SoapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val fuelPriceRepository: FuelPriceRepository = FuelPriceRepository()
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

    // --- 2. Freight Price State ---
    private val _freightState = MutableStateFlow<SoapUiState<Double>>(SoapUiState.Idle)
    val freightState: StateFlow<SoapUiState<Double>> = _freightState.asStateFlow()

    // --- 3. Hub Clustering State ---
    private val _hubState = MutableStateFlow<SoapUiState<List<Coordinate>>>(SoapUiState.Idle)
    val hubState: StateFlow<SoapUiState<List<Coordinate>>> = _hubState.asStateFlow()

    // --- 4. Carbon Footprint State ---
    private val _carbonState = MutableStateFlow<SoapUiState<Double>>(SoapUiState.Idle)
    val carbonState: StateFlow<SoapUiState<Double>> = _carbonState.asStateFlow()

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
                }
                .onFailure { error ->
                    _carbonState.value = SoapUiState.Error(error.localizedMessage ?: "Unknown network error")
                }
        }
    }

    /**
     * Resets transaction states.
     */
    fun resetStates() {
        _yieldState.value = SoapUiState.Idle
        _freightState.value = SoapUiState.Idle
        _hubState.value = SoapUiState.Idle
        _carbonState.value = SoapUiState.Idle
        _fuelPriceState.value = SoapUiState.Idle
    }
}
