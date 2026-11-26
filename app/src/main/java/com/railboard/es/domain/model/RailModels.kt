package com.railboard.es.domain.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Station(
    val code: String,
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
)

enum class BoardDirection(val apiValue: String, val label: String) {
    DEPARTURES("departures", "Salidas"),
    ARRIVALS("arrivals", "Llegadas")
}

enum class TrainStatus {
    ON_TIME,
    DELAYED,
    BOARDING,
    DEPARTED,
    CANCELLED
}

data class TrainMovement(
    val id: String,
    val trainNumber: String,
    val serviceType: String,
    val operatorName: String,
    val originOrDestination: String,
    val scheduledTime: LocalTime,
    val delayMinutes: Int,
    val status: TrainStatus,
    val platform: String?,
    val notes: String? = null
) {
    val estimatedTime: LocalTime
        get() = scheduledTime.plusMinutes(delayMinutes.toLong())

    fun formattedScheduled(): String = scheduledTime.format(TIME_FORMATTER)
    fun formattedEstimated(): String = estimatedTime.format(TIME_FORMATTER)

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

enum class DataOrigin {
    LIVE_API,
    CACHED_FALLBACK,
    MOCK_DATA
}

data class BoardSnapshot(
    val station: Station,
    val direction: BoardDirection,
    val movements: List<TrainMovement>,
    val origin: DataOrigin,
    val updatedAt: LocalTime,
    val warningMessage: String? = null
)
