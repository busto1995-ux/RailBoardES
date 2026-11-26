package com.railboard.es.data.remote

import com.railboard.es.data.local.StationCatalog
import com.railboard.es.domain.model.BoardDirection
import com.railboard.es.domain.model.Station
import com.railboard.es.domain.model.TrainMovement
import com.railboard.es.domain.model.TrainStatus
import kotlinx.coroutines.delay
import java.time.LocalTime

class MockRailDataSource : RailDataSource {

    private data class RouteTemplate(
        val trainNumber: String,
        val serviceType: String,
        val operatorName: String,
        val counterpart: String,
        val offsetMinutes: Long,
        val delayMinutes: Int,
        val status: TrainStatus,
        val platform: String?,
        val notes: String? = null
    )

    override suspend fun fetchStations(): List<Station> = StationCatalog.seedStations

    override suspend fun fetchBoard(
        station: Station,
        direction: BoardDirection
    ): List<TrainMovement> {
        delay(350)
        val now = LocalTime.now().withSecond(0).withNano(0)
        val seed = station.code.hashCode() and 0x7fffffff
        val peers = StationCatalog.seedStations.filter { it.code != station.code }

        val templates = listOf(
            RouteTemplate("04122", "AVE", "Renfe", peers[seed % peers.size].name, 4, 0, TrainStatus.BOARDING, "3"),
            RouteTemplate("18060", "MD", "Renfe", peers[(seed + 1) % peers.size].name, 14, 7, TrainStatus.DELAYED, "2", "Retraso por regulación de tráfico"),
            RouteTemplate("06311", "AVLO", "Renfe", peers[(seed + 2) % peers.size].name, 26, 0, TrainStatus.ON_TIME, "5"),
            RouteTemplate("00438", "Alvia", "Renfe", peers[(seed + 3) % peers.size].name, 41, 12, TrainStatus.DELAYED, "1"),
            RouteTemplate("06145", "Alta Velocidad", "Iryo", peers[(seed + 4) % peers.size].name, 55, 0, TrainStatus.ON_TIME, null, "Vía pendiente de asignación"),
            RouteTemplate("16004", "Regional Exprés", "Renfe", peers[(seed + 5) % peers.size].name, 73, 0, TrainStatus.ON_TIME, "4"),
            RouteTemplate("06580", "Alta Velocidad", "Ouigo", peers[(seed + 6) % peers.size].name, 92, 0, TrainStatus.CANCELLED, null, "Servicio suprimido")
        )

        return templates.mapIndexed { idx, item ->
            val adjustedStatus = if (direction == BoardDirection.ARRIVALS && item.status == TrainStatus.BOARDING) {
                TrainStatus.ON_TIME
            } else {
                item.status
            }
            TrainMovement(
                id = "${station.code}-${direction.apiValue}-${item.trainNumber}-$idx",
                trainNumber = item.trainNumber,
                serviceType = item.serviceType,
                operatorName = item.operatorName,
                originOrDestination = item.counterpart,
                scheduledTime = now.plusMinutes(item.offsetMinutes),
                delayMinutes = item.delayMinutes,
                status = adjustedStatus,
                platform = item.platform,
                notes = item.notes
            )
        }.sortedBy { it.scheduledTime }
    }
}
