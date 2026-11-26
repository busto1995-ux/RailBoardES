package com.railboard.es.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.railboard.es.domain.model.Station
import kotlinx.coroutines.tasks.await
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NearestStationMatch(
    val station: Station,
    val distanceKm: Double
)

class NearestStationLocator(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun findNearest(stations: List<Station>): Result<NearestStationMatch> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Permiso de ubicación no concedido"))
        }
        if (stations.isEmpty()) {
            return Result.failure(IllegalStateException("Catálogo de estaciones vacío"))
        }

        return runCatching {
            val cts = CancellationTokenSource()
            val location = fusedClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .await()
                ?: fusedClient.lastLocation.await()
                ?: error("No se pudo obtener la ubicación GPS actual")

            stations
                .map { station ->
                    NearestStationMatch(
                        station = station,
                        distanceKm = haversineKm(
                            location.latitude,
                            location.longitude,
                            station.latitude,
                            station.longitude
                        )
                    )
                }
                .minBy { it.distanceKm }
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthRadiusKm * asin(sqrt(a))
    }
}
