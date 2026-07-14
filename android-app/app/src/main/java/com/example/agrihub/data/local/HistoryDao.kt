package com.example.agriflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for all computation-history tables. Kept as a single
 * DAO (rather than four separate ones) since the queries are trivial and
 * this keeps [AppDatabase] and [HistoryRepository] simple.
 *
 * Each table is capped to the most recent 20 rows per query -- this is a
 * "recent runs" convenience list, not a full audit log, so unbounded growth
 * isn't useful and would just slow down the history screens over time.
 */
@Dao
interface HistoryDao {

    @Insert
    suspend fun insertYield(entry: YieldForecastEntity)

    @Query("SELECT * FROM yield_forecast_history ORDER BY timestamp DESC LIMIT 20")
    fun getYieldHistory(): Flow<List<YieldForecastEntity>>

    @Insert
    suspend fun insertFreight(entry: FreightCostEntity)

    @Query("SELECT * FROM freight_cost_history ORDER BY timestamp DESC LIMIT 20")
    fun getFreightHistory(): Flow<List<FreightCostEntity>>

    @Insert
    suspend fun insertHubCluster(entry: HubClusterEntity)

    @Query("SELECT * FROM hub_cluster_history ORDER BY timestamp DESC LIMIT 20")
    fun getHubClusterHistory(): Flow<List<HubClusterEntity>>

    @Insert
    suspend fun insertCarbon(entry: CarbonFootprintEntity)

    @Query("SELECT * FROM carbon_footprint_history ORDER BY timestamp DESC LIMIT 20")
    fun getCarbonHistory(): Flow<List<CarbonFootprintEntity>>
}
