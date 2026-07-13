package com.example.agriflow.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Domain model representing a fuel price quote fetched from the REST API.
 */
data class FuelPriceInfo(
    val pricePerLiter: Double,
    val fuelType: String,
    val currency: String,
    val sourceUrl: String,
    val asOfText: String?,
    val note: String?,
    val cacheStatus: String,
    val fetchedAt: String?
)

/**
 * Repository handling plain REST/JSON transactions against the AgriFlow
 * fuel-price-api.php endpoint.
 *
 * Unlike [SoapRepository], this talks to the backend with a simple HTTP GET
 * and a small JSON body -- no WSDL contract, no XML envelope construction.
 * It exists specifically to demonstrate a lightweight REST integration next
 * to the heavier SOAP transactions used elsewhere in the app.
 */
class FuelPriceRepository {

    private val TAG = "FuelPriceRepository"

    /**
     * Fetches the current live (or cached/fallback) diesel price in PHP per
     * liter from the REST endpoint.
     *
     * @param endpointUrl Full URL to fuel-price-api.php, e.g.
     *   "http://10.0.2.2:8000/fuel-price-api.php" for the Android emulator.
     */
    suspend fun fetchDieselPrice(endpointUrl: String): Result<FuelPriceInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
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

            Log.d(TAG, "Fuel price REST response: $body")
            val json = JSONObject(body)

            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("API reported failure"))
            }

            val info = FuelPriceInfo(
                pricePerLiter = json.getDouble("pricePerLiter"),
                fuelType = json.optString("fuelType", "diesel"),
                currency = json.optString("currency", "PHP"),
                sourceUrl = json.optString("sourceUrl", ""),
                asOfText = if (json.isNull("asOfText") || !json.has("asOfText")) null else json.optString("asOfText"),
                note = if (json.isNull("note") || !json.has("note")) null else json.optString("note"),
                cacheStatus = json.optString("cacheStatus", "unknown"),
                fetchedAt = if (json.isNull("fetchedAt") || !json.has("fetchedAt")) null else json.optString("fetchedAt")
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "REST fuel price fetch error", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
