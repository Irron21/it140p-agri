package com.example.agriflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * App-local SQLite database (via Room) holding computation history for the
 * four SOAP tools. This is purely local persistence -- it doesn't talk to
 * either the SOAP or REST backends, it just remembers what the user ran on
 * this device so results survive navigating away or restarting the app.
 */
@Database(
    entities = [
        YieldForecastEntity::class,
        FreightCostEntity::class,
        HubClusterEntity::class,
        CarbonFootprintEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agriflow_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
