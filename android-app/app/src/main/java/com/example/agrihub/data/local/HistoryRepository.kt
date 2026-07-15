package com.example.agriflow.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the persistence operations for SOAP transaction history.
 * Allows for easy mocking in unit tests.
 */
interface HistoryRepository {
    val yieldHistory: Flow<List<YieldForecastEntity>>
    suspend fun saveYield(entry: YieldForecastEntity)

    val freightHistory: Flow<List<FreightCostEntity>>
    suspend fun saveFreight(entry: FreightCostEntity)

    val hubClusterHistory: Flow<List<HubClusterEntity>>
    suspend fun saveHubCluster(entry: HubClusterEntity)

    val carbonHistory: Flow<List<CarbonFootprintEntity>>
    suspend fun saveCarbon(entry: CarbonFootprintEntity)
}

/**
 * Room-backed implementation of [HistoryRepository].
 * Takes an Android [Context] to access the local [AppDatabase].
 */
class RoomHistoryRepository(context: Context) : HistoryRepository {

    private val dao = AppDatabase.getInstance(context).historyDao()

    override val yieldHistory: Flow<List<YieldForecastEntity>> = dao.getYieldHistory()
    override suspend fun saveYield(entry: YieldForecastEntity) = dao.insertYield(entry)

    override val freightHistory: Flow<List<FreightCostEntity>> = dao.getFreightHistory()
    override suspend fun saveFreight(entry: FreightCostEntity) = dao.insertFreight(entry)

    override val hubClusterHistory: Flow<List<HubClusterEntity>> = dao.getHubClusterHistory()
    override suspend fun saveHubCluster(entry: HubClusterEntity) = dao.insertHubCluster(entry)

    override val carbonHistory: Flow<List<CarbonFootprintEntity>> = dao.getCarbonHistory()
    override suspend fun saveCarbon(entry: CarbonFootprintEntity) = dao.insertCarbon(entry)
}
