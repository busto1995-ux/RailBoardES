package com.railboard.es.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.railboard.es.data.local.StationCatalog
import com.railboard.es.data.repository.RailRepository
import com.railboard.es.domain.model.BoardDirection
import com.railboard.es.domain.model.BoardSnapshot
import com.railboard.es.domain.model.Station
import com.railboard.es.location.NearestStationLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardUiState(
    val allStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val favoriteCodes: Set<String> = emptySet(),
    val selectedStation: Station? = null,
    val nearestDistanceKm: Double? = null,
    val direction: BoardDirection = BoardDirection.DEPARTURES,
    val snapshot: BoardSnapshot? = null,
    val searchQuery: String = "",
    val searchResults: List<Station> = emptyList(),
    val isLoadingBoard: Boolean = false,
    val isLocating: Boolean = false,
    val statusMessage: String? = null
)

class BoardViewModel(
    private val repository: RailRepository,
    private val locator: NearestStationLocator
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState(isLoadingBoard = true))
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        initialize()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.favoriteCodes.collect { codes ->
                _uiState.update { state ->
                    state.copy(
                        favoriteCodes = codes,
                        favoriteStations = state.allStations.filter { it.code in codes }
                    )
                }
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            val stations = repository.loadStations()
            val savedCode = repository.lastStationCode.first()
            val favCodes = repository.favoriteCodes.first()

            _uiState.update {
                it.copy(
                    allStations = stations,
                    favoriteCodes = favCodes,
                    favoriteStations = stations.filter { st -> st.code in favCodes },
                    searchResults = stations
                )
            }

            val initialStation = stations.find { it.code == savedCode } ?: stations.firstOrNull()
            if (locator.hasLocationPermission()) {
                detectNearestStation(fallbackStation = initialStation)
            } else if (initialStation != null) {
                selectStation(initialStation, distanceKm = null)
            }
        }
    }

    fun detectNearestStation(fallbackStation: Station? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, statusMessage = null) }
            val stations = _uiState.value.allStations.ifEmpty { repository.loadStations() }
            val result = locator.findNearest(stations)
            _uiState.update { it.copy(isLocating = false) }

            result.onSuccess { match ->
                selectStation(match.station, distanceKm = match.distanceKm)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = error.message ?: "No se pudo detectar la estación cercana")
                }
                if (_uiState.value.selectedStation == null && fallbackStation != null) {
                    selectStation(fallbackStation, distanceKm = null)
                }
            }
        }
    }

    fun selectStation(station: Station, distanceKm: Double? = null) {
        viewModelScope.launch {
            repository.saveLastStation(station.code)
            _uiState.update {
                it.copy(
                    selectedStation = station,
                    nearestDistanceKm = distanceKm,
                    statusMessage = null
                )
            }
            refreshBoard()
        }
    }

    fun setDirection(direction: BoardDirection) {
        if (_uiState.value.direction == direction) return
        _uiState.update { it.copy(direction = direction) }
        refreshBoard()
    }

    fun refreshBoard() {
        val station = _uiState.value.selectedStation ?: return
        val direction = _uiState.value.direction
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBoard = true) }
            val snapshot = repository.fetchBoard(station, direction)
            _uiState.update {
                it.copy(
                    snapshot = snapshot,
                    isLoadingBoard = false
                )
            }
        }
    }

    fun toggleFavorite(stationCode: String) {
        viewModelScope.launch {
            repository.toggleFavorite(stationCode)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                searchResults = StationCatalog.filter(state.allStations, query)
            )
        }
    }

    class Factory(
        private val repository: RailRepository,
        private val locator: NearestStationLocator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoardViewModel(repository, locator) as T
        }
    }
}
