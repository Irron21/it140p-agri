package com.example.agriflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities that persist a run's inputs + result so it survives app
 * restarts and can be shown in a "Recent Runs" list per tool. Each SOAP tool
 * in the app gets its own strongly-typed table rather than one generic blob
 * table, since the input shapes differ enough (a lat/lng list vs plain
 * numbers) that typed columns are clearer to query and to read back.
 */

@Entity(tableName = "yield_forecast_history")
data class YieldForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val area: Double,
    val temperature: Double,
    val ph: Double,
    val resultTons: Double
)

@Entity(tableName = "freight_cost_history")
data class FreightCostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val distance: Double,
    val fuelPrice: Double,
    val weight: Double,
    val resultCost: Double
)

/**
 * Hub clustering has variable-length inputs (a list of tapped farm
 * coordinates) and a variable-length result (a list of computed hub
 * centroids). Rather than pull in a JSON library for two fields, they're
 * encoded as a simple "lat,lng;lat,lng;..." string -- see [encodePoints] /
 * [decodePoints] below.
 */
@Entity(tableName = "hub_cluster_history")
data class HubClusterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val farmPoints: String,
    val k: Int,
    val resultHubs: String
)

@Entity(tableName = "carbon_footprint_history")
data class CarbonFootprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val efficiency: Double,
    val distance: Double,
    val weight: Double,
    val resultCo2: Double
)

/**
 * Encodes a list of (latitude, longitude) pairs as "lat,lng;lat,lng;...".
 */
fun encodePoints(points: List<Pair<Double, Double>>): String =
    points.joinToString(separator = ";") { (lat, lng) -> "$lat,$lng" }

/**
 * Decodes a string produced by [encodePoints] back into a point list.
 * Malformed segments are skipped rather than throwing, since this only
 * backs a "tap to reuse" convenience feature, not a critical data path.
 */
fun decodePoints(encoded: String): List<Pair<Double, Double>> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(";").mapNotNull { segment ->
        val parts = segment.split(",")
        if (parts.size != 2) return@mapNotNull null
        val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
        val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
        lat to lng
    }
}
