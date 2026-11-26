package com.railboard.es.data.remote

import com.railboard.es.domain.model.BoardDirection
import com.railboard.es.domain.model.Station
import com.railboard.es.domain.model.TrainMovement
import com.railboard.es.domain.model.TrainStatus
import java.time.LocalTime

interface RailDataSource {
    suspend fun fetchStations(): List<Station>
    suspend fun fetchBoard(station: Station, direction: BoardDirection): List<TrainMovement>
}

class RetrofitRailDataSource(
    private val api: RailApiService
) : RailDataSource {

    override suspend fun fetchStations(): List<Station> {
        return api.getStations().map { dto ->
            Station(
                code = dto.code,
                name = dto.name,
                province = dto.province,
                latitude = dto.latitude,
                longitude = dto.longitude
            )
        }
    }

    override suspend fun fetchBoard(
        station: Station,
        direction: BoardDirection
    ): List<TrainMovement> {
        val response = api.getStationBoard(station.code, direction.apiValue)
        return response.movements.map { dto ->
            TrainMovement(
                id = dto.id,
                trainNumber = dto.trainNumber,
                serviceType = dto.serviceType,
                operatorName = dto.operatorName,
                originOrDestination = dto.originOrDestination,
                scheduledTime = runCatching { LocalTime.parse(dto.scheduledTime) }
                    .getOrDefault(LocalTime.now()),
                delayMinutes = dto.delayMinutes.coerceAtLeast(0),
                status = runCatching { TrainStatus.valueOf(dto.status.uppercase()) }
                    .getOrDefault(if (dto.delayMinutes > 0) TrainStatus.DELAYED else TrainStatus.ON_TIME),
                platform = dto.platform?.takeIf { it.isNotBlank() },
                notes = dto.notes
            )
        }
    }
}
