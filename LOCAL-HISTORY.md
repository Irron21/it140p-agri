# Local Calculation History (Room Database)

## Summary
Adds on-device persistence for every calculation the user runs (yield
forecast, freight cost, hub clustering, carbon footprint) using
[Room](https://developer.android.com/training/data-storage/room) (Android's
SQLite wrapper). Each tab now has a "Recent Runs" list that survives app
restarts and lets the user tap an old entry to reload those inputs.

This is purely local storage — it does not talk to the SOAP or REST
backends. It only remembers what was calculated *on this device*.

## Files touched
| File | Role |
|---|---|
| `data/local/HistoryEntities.kt` | New. The 4 Room `@Entity` table definitions + point-list encode/decode helpers. |
| `data/local/HistoryDao.kt` | New. `@Dao` interface — insert + query per table. |
| `data/local/AppDatabase.kt` | New. `@Database` class, singleton accessor. |
| `data/local/HistoryRepository.kt` | New. Thin wrapper the ViewModel talks to instead of touching Room directly. |
| `viewmodel/AgriFlowViewModel.kt` | Saves a row after every successful calculation; exposes each history list as `StateFlow`. |
| `ui/main/MainScreen.kt` | Collapsible "Recent Runs" section per tab; tapping a row reuses its inputs. |

## Schema

Four tables, one per calculator — not a single generic table — because the
input shapes genuinely differ (hub clustering stores a *list* of tapped
coordinates and a *list* of result centroids; the others are flat numeric
inputs). Each table follows the same shape: an auto-increment id, a
timestamp, the inputs used, and the result.

```kotlin
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

@Entity(tableName = "hub_cluster_history")
data class HubClusterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val farmPoints: String,  // encoded "lat,lng;lat,lng;..."
    val k: Int,
    val resultHubs: String   // encoded "lat,lng;lat,lng;..."
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
```

**Coordinate list encoding:** rather than add a JSON library for the two
list-typed fields on `HubClusterEntity`, points are encoded as a plain
`"lat,lng;lat,lng;..."` string via `encodePoints()` / `decodePoints()`.
`decodePoints()` skips malformed segments instead of throwing, since it only
backs a "tap to reuse" convenience feature, not a critical data path.

## Data access

`HistoryDao` is a single `@Dao` interface covering all four tables (kept as
one DAO rather than four, since the queries are trivial). Each table has:

- An `@Insert suspend fun insertX(entry: XEntity)`
- A capped, most-recent-first read:
  ```kotlin
  @Query("SELECT * FROM yield_forecast_history ORDER BY timestamp DESC LIMIT 20")
  fun getYieldHistory(): Flow<List<YieldForecastEntity>>
  ```
  The `LIMIT 20` is deliberate — this is a "recent runs" convenience list,
  not a full audit log, so the table can grow unbounded on disk while the
  UI only ever reads the most recent 20 rows.

## Database setup

`AppDatabase` is a standard Room singleton, built once per process:

```kotlin
@Database(
    entities = [YieldForecastEntity::class, FreightCostEntity::class,
                HubClusterEntity::class, CarbonFootprintEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "agriflow_history.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

`version = 1` with no migrations defined yet — schema changes later will
need a `Migration` or `fallbackToDestructiveMigration()`.

`HistoryRepository` wraps the DAO so the ViewModel never imports Room types
directly; it just exposes each history list as a `Flow` and a `saveX(entry)`
suspend function.

## How a row gets saved

In `AgriFlowViewModel`, every "run" function calls the backend, and on
success both updates the UI state **and** writes a history row in the same
coroutine:

```kotlin
fun runYieldForecast(area: Double, temp: Double, ph: Double) {
    viewModelScope.launch {
        _yieldState.value = SoapUiState.Loading
        repository.calculateYieldForecast(_endpointUrl.value, area, temp, ph)
            .onSuccess { yieldVal ->
                _yieldState.value = SoapUiState.Success(yieldVal)
                historyRepository.saveYield(
                    YieldForecastEntity(
                        timestamp = System.currentTimeMillis(),
                        area = area, temperature = temp, ph = ph,
                        resultTons = yieldVal
                    )
                )
            }
            .onFailure { error -> _yieldState.value = SoapUiState.Error(...) }
    }
}
```

Failed calculations are **not** saved — only successful runs get a history
row.

## How the UI stays in sync

Each history list is turned into a `StateFlow` in the ViewModel:

```kotlin
val yieldHistory: StateFlow<List<YieldForecastEntity>> =
    historyRepository.yieldHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

Because Room's `Flow` re-emits automatically whenever the underlying table
changes, saving a new row after a calculation causes the "Recent Runs"
section on that tab to update on its own — no manual refresh call needed.

Tapping a history row calls back into the ViewModel (e.g.
`selectYieldForReuse(entry)`), which repopulates the tab's input fields with
that entry's saved values.

## Not in scope
- No delete/edit of individual history rows — only insert and capped read.
- No manual migration path yet (`version = 1`, `exportSchema = false`); a
  schema change later needs a `Migration` added before bumping the version.
- History is local-only and per-device; it is not synced to any backend or
  shared between installs.
