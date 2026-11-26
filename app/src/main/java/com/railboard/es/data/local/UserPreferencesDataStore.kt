package com.railboard.es.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.railboard.es.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.railDataStore: DataStore<Preferences> by preferencesDataStore(name = "railboard_prefs")

class UserPreferencesDataStore(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    private val favoriteCodesKey = stringSetPreferencesKey("favorite_station_codes")
    private val lastStationCodeKey = stringPreferencesKey("last_station_code")
    private val cachedStationsJsonKey = stringPreferencesKey("cached_stations_json")

    val favoriteStationCodes: Flow<Set<String>> = context.railDataStore.data.map { prefs ->
        prefs[favoriteCodesKey] ?: emptySet()
    }

    val lastStationCode: Flow<String?> = context.railDataStore.data.map { prefs ->
        prefs[lastStationCodeKey]
    }

    suspend fun toggleFavorite(stationCode: String) {
        context.railDataStore.edit { prefs ->
            val current = prefs[favoriteCodesKey]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(stationCode)) {
                current.remove(stationCode)
            }
            prefs[favoriteCodesKey] = current
        }
    }

    suspend fun saveLastStationCode(stationCode: String) {
        context.railDataStore.edit { prefs ->
            prefs[lastStationCodeKey] = stationCode
        }
    }

    suspend fun getCachedStations(): List<Station>? {
        val json = context.railDataStore.data.first()[cachedStationsJsonKey] ?: return null
        return runCatching {
            val type = object : TypeToken<List<Station>>() {}.type
            gson.fromJson<List<Station>>(json, type)
        }.getOrNull()
    }

    suspend fun saveCachedStations(stations: List<Station>) {
        if (stations.isEmpty()) return
        context.railDataStore.edit { prefs ->
            prefs[cachedStationsJsonKey] = gson.toJson(stations)
        }
    }
}
