package com.railboard.es.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RailApiService {

    @GET("v1/stations")
    suspend fun getStations(): List<StationDto>

    @GET("v1/stations/{stationCode}/board")
    suspend fun getStationBoard(
        @Path("stationCode") stationCode: String,
        @Query("direction") direction: String
    ): BoardResponseDto
}

data class StationDto(
    val code: String,
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
)

data class BoardResponseDto(
    val stationCode: String,
    val movements: List<TrainMovementDto>
)

data class TrainMovementDto(
    val id: String,
    val trainNumber: String,
    val serviceType: String,
    val operatorName: String,
    val originOrDestination: String,
    val scheduledTime: String,
    val delayMinutes: Int,
    val status: String,
    val platform: String?,
    val notes: String?
)
