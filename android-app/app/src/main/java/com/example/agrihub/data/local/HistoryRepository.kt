package com.example.agriflow.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper around [HistoryDao] so the ViewModel doesn't touch Room
 * directly. Takes an Android [Context] (needed to open/create the on-device
 * database file) -- see MainScreen.kt for how this gets constructed with the
 * Application context via the ViewModel factory.
 */
class HistoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).historyDao()

    val yieldHistory: Flow<List<YieldForecastEntity>> = dao.getYieldHistory()
    suspend fun saveYield(entry: YieldForecastEntity) = dao.insertYield(entry)

    val freightHistory: Flow<List<FreightCostEntity>> = dao.getFreightHistory()
    suspend fun saveFreight(entry: FreightCostEntity) = dao.insertFreight(entry)

    val hubClusterHistory: Flow<List<HubClusterEntity>> = dao.getHubClusterHistory()
    suspend fun saveHubCluster(entry: HubClusterEntity) = dao.insertHubCluster(entry)

    val carbonHistory: Flow<List<CarbonFootprintEntity>> = dao.getCarbonHistory()
    suspend fun saveCarbon(entry: CarbonFootprintEntity) = dao.insertCarbon(entry)
}
