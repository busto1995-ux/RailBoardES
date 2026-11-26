package com.railboard.es.data.local

import com.railboard.es.domain.model.Station
import java.text.Normalizer

object StationCatalog {
    val seedStations: List<Station> = listOf(
        Station("60000", "Madrid-Puerta de Atocha-Almudena Grandes", "Madrid", 40.4065, -3.6908),
        Station("17000", "Madrid-Chamartín-Clara Campoamor", "Madrid", 40.4721, -3.6826),
        Station("71801", "Barcelona-Sants", "Barcelona", 41.3790, 2.1401),
        Station("51003", "Sevilla-Santa Justa", "Sevilla", 37.3914, -5.9756),
        Station("03216", "València-Joaquín Sorolla", "Valencia", 39.4596, -0.3821),
        Station("04040", "Zaragoza-Delicias", "Zaragoza", 41.6587, -0.9116),
        Station("54413", "Málaga-María Zambrano", "Málaga", 36.7116, -4.4317),
        Station("10600", "Valladolid-Campo Grande", "Valladolid", 41.6416, -4.7276),
        Station("11014", "Burgos-Rosa Manzano", "Burgos", 42.3712, -3.6665),
        Station("11200", "Miranda de Ebro", "Burgos", 42.6815, -2.9488),
        Station("11208", "Vitoria/Gasteiz", "Álava", 42.8414, -2.6725),
        Station("13200", "Bilbao-Abando Indalecio Prieto", "Vizcaya", 43.2602, -2.9272),
        Station("11511", "Donostia-San Sebastián", "Guipúzcoa", 43.3176, -1.9768),
        Station("81100", "Pamplona/Iruña", "Navarra", 42.8253, -1.6615)
    )

    fun filter(stations: List<Station>, query: String): List<Station> {
        val clean = normalize(query)
        if (clean.isBlank()) return stations
        return stations.filter { station ->
            station.code.contains(clean) ||
                normalize(station.name).contains(clean) ||
                normalize(station.province).contains(clean)
        }.sortedByDescending { station ->
            station.code.startsWith(clean) || normalize(station.name).startsWith(clean)
        }
    }

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
