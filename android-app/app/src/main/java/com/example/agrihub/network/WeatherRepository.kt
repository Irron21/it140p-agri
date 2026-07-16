package com.example.agriflow.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Domain model representing current weather data fetched from the REST API.
 */
data class WeatherInfo(
    val temperature: Double,
    val unit: String,
    val humidity: Int,
    val condition: String,
    val location: String,
    val fetchedAt: String,
    val lat: Double,
    val lon: Double
)

/**
 * Repository handling REST transactions against the AgriFlow weather-api.php endpoint.
 */
class WeatherRepository {

    private val TAG = "WeatherRepository"

    /**
     * Fetches the current live temperature from the REST endpoint, optionally
     * passing coordinates to get location-specific data.
     */
    suspend fun fetchCurrentWeather(
        endpointUrl: String,
        lat: Double? = null,
        lon: Double? = null
    ): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val fullUrl = if (lat != null && lon != null) {
                "$endpointUrl?lat=$lat&lon=$lon"
            } else {
                endpointUrl
            }
            
            val url = URL(fullUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use(BufferedReader::readText)

            if (status !in 200..299) {
                Log.e(TAG, "REST fetch failed: HTTP $status - $body")
                return@withContext Result.failure(Exception("Server returned HTTP $status"))
            }

            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("API reported failure"))
            }

            val info = WeatherInfo(
                temperature = json.getDouble("temperature"),
                unit = json.optString("unit", "Celsius"),
                humidity = json.optInt("humidity", 0),
                condition = json.optString("condition", "Unknown"),
                location = json.optString("location", "Unknown"),
                fetchedAt = json.optString("fetchedAt", ""),
                lat = json.optDouble("lat", 0.0),
                lon = json.optDouble("lon", 0.0)
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "REST weather fetch error", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
