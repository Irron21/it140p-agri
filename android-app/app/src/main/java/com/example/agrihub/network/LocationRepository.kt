package com.example.agriflow.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Domain model representing a GPS coordinate.
 */
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val provider: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

/**
 * Repository handling device location retrieval using the standard Android LocationManager.
 * This avoids external Play Services dependencies while maintaining a similar
 * "fetch" pattern as the REST repositories.
 */
class LocationRepository(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<LocationInfo> = suspendCancellableCoroutine { continuation ->
        try {
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else {
                null
            }

            if (provider == null) {
                continuation.resume(Result.failure(Exception("No location providers enabled. Please enable GPS.")))
                return@suspendCancellableCoroutine
            }

            // Fallback: try to get the last known location immediately
            val lastKnown = locationManager.getLastKnownLocation(provider)
            if (lastKnown != null) {
                Log.d("LocationRepo", "Using last known location: ${lastKnown.latitude}, ${lastKnown.longitude}")
                // We resume with this but also keep the listener for a fresh update if possible?
                // Actually, if we resume here, we can't resume again.
                // For a "My Location" button, last known is often enough and much faster.
                continuation.resume(Result.success(LocationInfo(lastKnown.latitude, lastKnown.longitude, provider)))
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        Log.d("LocationRepo", "Fresh location received: ${location.latitude}, ${location.longitude}")
                        continuation.resume(Result.success(LocationInfo(location.latitude, location.longitude, location.provider ?: "GPS")))
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Location provider $provider disabled")))
                    }
                }
            }

            locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
            
            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        } catch (e: Exception) {
            Log.e("LocationRepo", "Error getting location", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}
