package com.railboard.es.data.repository

import com.railboard.es.data.local.StationCatalog
import com.railboard.es.data.local.UserPreferencesDataStore
import com.railboard.es.data.remote.MockRailDataSource
import com.railboard.es.data.remote.RailDataSource
import com.railboard.es.domain.model.BoardDirection
import com.railboard.es.domain.model.BoardSnapshot
import com.railboard.es.domain.model.DataOrigin
import com.railboard.es.domain.model.Station
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class RailRepository(
    private val remoteDataSource: RailDataSource?,
    private val mockDataSource: MockRailDataSource,
    private val prefs: UserPreferencesDataStore
) {
    private val boardCache = mutableMapOf<Pair<String, BoardDirection>, BoardSnapshot>()

    val favoriteCodes: Flow<Set<String>> = prefs.favoriteStationCodes
    val lastStationCode: Flow<String?> = prefs.lastStationCode

    suspend fun loadStations(forceRemote: Boolean = false): List<Station> {
        val localStations = prefs.getCachedStations().orEmpty().ifEmpty { StationCatalog.seedStations }
        if (remoteDataSource == null || (!forceRemote && localStations.isNotEmpty())) {
            return localStations
        }
        return runCatching {
            remoteDataSource.fetchStations().also { fetched ->
                if (fetched.isNotEmpty()) prefs.saveCachedStations(fetched)
            }
        }.getOrElse { localStations }
    }

    suspend fun fetchBoard(
        station: Station,
        direction: BoardDirection
    ): BoardSnapshot {
        val cacheKey = station.code to direction

        if (remoteDataSource != null) {
            val remoteResult = runCatching { remoteDataSource.fetchBoard(station, direction) }
            remoteResult.onSuccess { movements ->
                val snapshot = BoardSnapshot(
                    station = station,
                    direction = direction,
                    movements = movements.sortedBy { it.scheduledTime },
                    origin = DataOrigin.LIVE_API,
                    updatedAt = LocalTime.now()
                )
                boardCache[cacheKey] = snapshot
                return snapshot
            }.onFailure { error ->
                boardCache[cacheKey]?.let { stale ->
                    return stale.copy(
                        origin = DataOrigin.CACHED_FALLBACK,
                        warningMessage = "Sin conexión con el servidor (${error.message ?: "error de red"}). Mostrando últimos datos guardados."
                    )
                }
                val mockMovements = mockDataSource.fetchBoard(station, direction)
                return BoardSnapshot(
                    station = station,
                    direction = direction,
                    movements = mockMovements,
                    origin = DataOrigin.MOCK_DATA,
                    updatedAt = LocalTime.now(),
                    warningMessage = "Error de red al consultar el servidor. Mostrando datos simulados."
                )
            }
        }

        val mockMovements = mockDataSource.fetchBoard(station, direction)
        return BoardSnapshot(
            station = station,
            direction = direction,
            movements = mockMovements,
            origin = DataOrigin.MOCK_DATA,
            updatedAt = LocalTime.now(),
            warningMessage = "Modo demostración activo: configura RAIL_BFF_BASE_URL para conectar tu feed real."
        )
    }

    suspend fun toggleFavorite(stationCode: String) = prefs.toggleFavorite(stationCode)

    suspend fun saveLastStation(stationCode: String) = prefs.saveLastStationCode(stationCode)
}
