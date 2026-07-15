package com.example.agriflow.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.serialization.MarshalFloat
import org.ksoap2.transport.HttpTransportSE
import java.util.Vector

/**
 * Domain model representing coordinate points for AgriFlow logistics.
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

/**
 * Repository handling SOAP transactions with the AgriFlow PHP Web Service.
 */
class SoapRepository {

    private val TAG = "SoapRepository"
    private val namespace = "http://localhost:8000/agriflow"

    /**
     * Executes the SOAP transaction for crop yield forecasting.
     */
    suspend fun calculateYieldForecast(
        endpointUrl: String,
        area: Double,
        temp: Double,
        ph: Double
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val methodName = "calculateYieldForecast"
            val soapAction = "$namespace#$methodName"

            // Construct SOAP request payload
            val request = SoapObject(namespace, methodName).apply {
                addProperty("area", area)
                addProperty("temp", temp)
                addProperty("ph", ph)
            }

            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
                dotNet = false
                setOutputSoapObject(request)
            }
            MarshalFloat().register(envelope)

            Log.d(TAG, "Yield Request: $request")
            val transport = HttpTransportSE(endpointUrl, 10000)
            transport.call(soapAction, envelope)

            val response = envelope.response
            Log.d(TAG, "Yield Response: $response")

            if (response != null) {
                val yield = response.toString().toDoubleOrNull()
                if (yield != null) {
                    Result.success(yield)
                } else {
                    Result.failure(Exception("Failed to parse double from response: $response"))
                }
            } else {
                Result.failure(Exception("Empty SOAP response received"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yield SOAP execution error", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the SOAP transaction for freight price quotes.
     */
    suspend fun calculateFreightPrice(
        endpointUrl: String,
        distance: Double,
        fuelPrice: Double,
        weight: Double
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val methodName = "calculateFreightPrice"
            val soapAction = "$namespace#$methodName"

            val request = SoapObject(namespace, methodName).apply {
                addProperty("distance", distance)
                addProperty("fuelPrice", fuelPrice)
                addProperty("weight", weight)
            }

            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
                dotNet = false
                setOutputSoapObject(request)
            }
            MarshalFloat().register(envelope)

            Log.d(TAG, "Freight Request: $request")
            val transport = HttpTransportSE(endpointUrl, 10000)
            transport.call(soapAction, envelope)

            val response = envelope.response
            Log.d(TAG, "Freight Response: $response")

            if (response != null) {
                val price = response.toString().toDoubleOrNull()
                if (price != null) {
                    Result.success(price)
                } else {
                    Result.failure(Exception("Failed to parse double from response: $response"))
                }
            } else {
                Result.failure(Exception("Empty SOAP response received"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Freight SOAP execution error", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the SOAP transaction for K-Means hub location clustering.
     */
    suspend fun calculateHubCluster(
        endpointUrl: String,
        latitudes: List<Double>,
        longitudes: List<Double>,
        k: Int
    ): Result<List<Coordinate>> = withContext(Dispatchers.IO) {
        try {
            val methodName = "calculateHubCluster"
            val soapAction = "$namespace#$methodName"

            val request = SoapObject(namespace, methodName)

            // Construct latitudes DoubleArray property
            val latsObj = SoapObject(namespace, "latitudes")
            latitudes.forEach { lat ->
                latsObj.addProperty("value", lat)
            }
            request.addProperty("latitudes", latsObj)

            // Construct longitudes DoubleArray property
            val lngsObj = SoapObject(namespace, "longitudes")
            longitudes.forEach { lng ->
                lngsObj.addProperty("value", lng)
            }
            request.addProperty("longitudes", lngsObj)

            request.addProperty("k", k)

            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
                dotNet = false
                setOutputSoapObject(request)
            }
            MarshalFloat().register(envelope)

            Log.d(TAG, "Hub Cluster Request: $request")
            val transport = HttpTransportSE(endpointUrl, 12000) // Longer timeout for clustering
            transport.call(soapAction, envelope)

            val response = envelope.response
            Log.d(TAG, "Hub Cluster Response: $response")

            val coordinates = mutableListOf<Coordinate>()
            if (response != null) {
                parseCoordinateResponse(response, coordinates)
                Result.success(coordinates)
            } else {
                Result.failure(Exception("Empty SOAP response received"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hub Cluster SOAP execution error", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the SOAP transaction for CO2 footprint estimation.
     */
    suspend fun calculateCarbonFootprint(
        endpointUrl: String,
        efficiency: Double,
        distance: Double,
        weight: Double
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val methodName = "calculateCarbonFootprint"
            val soapAction = "$namespace#$methodName"

            val request = SoapObject(namespace, methodName).apply {
                addProperty("efficiency", efficiency)
                addProperty("distance", distance)
                addProperty("weight", weight)
            }

            val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
                dotNet = false
                setOutputSoapObject(request)
            }
            MarshalFloat().register(envelope)

            Log.d(TAG, "Carbon Request: $request")
            val transport = HttpTransportSE(endpointUrl, 20000)
            transport.call(soapAction, envelope)

            val response = envelope.response
            Log.d(TAG, "Carbon Response: $response")

            if (response != null) {
                val carbon = response.toString().toDoubleOrNull()
                if (carbon != null) {
                    Result.success(carbon)
                } else {
                    Result.failure(Exception("Failed to parse double from response: $response"))
                }
            } else {
                Result.failure(Exception("Empty SOAP response received"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Carbon SOAP execution error", e)
            Result.failure(e)
        }
    }

    /**
     * Parses the Coordinate objects out of the SOAP Response payload recursively.
     */
    private fun parseCoordinateResponse(response: Any, outList: MutableList<Coordinate>) {
        when (response) {
            is SoapObject -> {
                // If this SoapObject represents a single coordinate object, extract its fields directly
                if (response.hasProperty("latitude") && response.hasProperty("longitude")) {
                    val lat = response.getPropertySafelyAsString("latitude").toDoubleOrNull() ?: 0.0
                    val lng = response.getPropertySafelyAsString("longitude").toDoubleOrNull() ?: 0.0
                    outList.add(Coordinate(lat, lng))
                } else {
                    // Otherwise, search nested properties for "coordinate" elements
                    for (i in 0 until response.propertyCount) {
                        val propInfo = org.ksoap2.serialization.PropertyInfo()
                        response.getPropertyInfo(i, propInfo)
                        
                        val propValue = response.getProperty(i)
                        if (propInfo.name == "coordinate" && propValue is SoapObject) {
                            val lat = propValue.getPropertySafelyAsString("latitude").toDoubleOrNull() ?: 0.0
                            val lng = propValue.getPropertySafelyAsString("longitude").toDoubleOrNull() ?: 0.0
                            outList.add(Coordinate(lat, lng))
                        } else if (propValue is SoapObject) {
                            // Recursively parse
                            parseCoordinateResponse(propValue, outList)
                        }
                    }
                }
            }
            is Vector<*> -> {
                for (item in response) {
                    if (item != null) {
                        parseCoordinateResponse(item, outList)
                    }
                }
            }
        }
    }

    private fun SoapObject.getPropertySafelyAsString(name: String): String {
        return if (hasProperty(name)) getProperty(name).toString() else ""
    }
}
