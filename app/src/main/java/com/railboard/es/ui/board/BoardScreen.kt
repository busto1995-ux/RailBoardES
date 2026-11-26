package com.railboard.es.ui.board

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railboard.es.domain.model.BoardDirection
import com.railboard.es.domain.model.DataOrigin
import com.railboard.es.domain.model.Station
import com.railboard.es.domain.model.TrainMovement
import com.railboard.es.domain.model.TrainStatus
import com.railboard.es.ui.theme.BoardingCyan
import com.railboard.es.ui.theme.CancelledRed
import com.railboard.es.ui.theme.DelayedAmber
import com.railboard.es.ui.theme.OnTimeGreen
import com.railboard.es.ui.theme.PlatformAccent
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(viewModel: BoardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchSheet by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectNearestStation()
        }
    }

    val selected = state.selectedStation
    val isFavorite = selected != null && selected.code in state.favoriteCodes

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column(
                        modifier = Modifier.clickable { showSearchSheet = true }
                    ) {
                        Text(
                            text = selected?.name ?: "Seleccionar estación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = buildString {
                            if (selected != null) {
                                append("Cód. ${selected.code}")
                                state.nearestDistanceKm?.let { km ->
                                    append(String.format(Locale.US, " · %.1f km", km))
                                }
                                state.snapshot?.updatedAt?.let { time ->
                                    append(" · Act. ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                                }
                            } else {
                                append("Pulsa para buscar estación")
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selected != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(selected.code) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorita",
                                tint = if (isFavorite) PlatformAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        if (state.isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Estación más cercana"
                            )
                        }
                    }
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar estación"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.favoriteStations.isNotEmpty()) {
                FavoritesBar(
                    favorites = state.favoriteStations,
                    selectedCode = selected?.code,
                    onSelect = { viewModel.selectStation(it) }
                )
            }

            DirectionSelector(
                selected = state.direction,
                onSelect = viewModel::setDirection
            )

            val bannerMessage = state.statusMessage ?: state.snapshot?.warningMessage
            if (bannerMessage != null) {
                OriginBanner(
                    message = bannerMessage,
                    origin = state.snapshot?.origin ?: DataOrigin.MOCK_DATA
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isLoadingBoard,
                onRefresh = viewModel::refreshBoard,
                modifier = Modifier.fillMaxSize()
            ) {
                val movements = state.snapshot?.movements.orEmpty()
                if (movements.isEmpty() && !state.isLoadingBoard) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin circulaciones disponibles",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(movements, key = { it.id }) { movement ->
                            TrainBoardCard(
                                movement = movement,
                                direction = state.direction
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSearchSheet) {
        StationSearchSheet(
            query = state.searchQuery,
            results = state.searchResults,
            favoriteCodes = state.favoriteCodes,
            onQueryChange = viewModel::onSearchQueryChange,
            onToggleFavorite = viewModel::toggleFavorite,
            onSelectStation = { station ->
                viewModel.selectStation(station)
                viewModel.onSearchQueryChange("")
                showSearchSheet = false
            },
            onDismiss = { showSearchSheet = false }
        )
    }
}

@Composable
private fun FavoritesBar(
    favorites: List<Station>,
    selectedCode: String?,
    onSelect: (Station) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(favorites, key = { it.code }) { station ->
            FilterChip(
                selected = station.code == selectedCode,
                onClick = { onSelect(station) },
                label = {
                    Text(
                        text = station.name.substringBefore("-"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PlatformAccent
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSelector(
    selected: BoardDirection,
    onSelect: (BoardDirection) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BoardDirection.entries.forEachIndexed { index, dir ->
            SegmentedButton(
                selected = dir == selected,
                onClick = { onSelect(dir) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BoardDirection.entries.size
                )
            ) {
                Text(
                    text = dir.label.uppercase(),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun OriginBanner(
    message: String,
    origin: DataOrigin
) {
    val accent = when (origin) {
        DataOrigin.LIVE_API -> OnTimeGreen
        DataOrigin.CACHED_FALLBACK -> DelayedAmber
        DataOrigin.MOCK_DATA -> BoardingCyan
    }
    Surface(
        color = accent.copy(alpha = 0.14f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrainBoardCard(
    movement: TrainMovement,
    direction: BoardDirection
) {
    val isCancelled = movement.status == TrainStatus.CANCELLED
    val isDelayed = movement.delayMinutes > 0 && !isCancelled

    val statusColor = when (movement.status) {
        TrainStatus.ON_TIME -> OnTimeGreen
        TrainStatus.DELAYED -> DelayedAmber
        TrainStatus.BOARDING -> BoardingCyan
        TrainStatus.DEPARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        TrainStatus.CANCELLED -> CancelledRed
    }

    val statusText = when (movement.status) {
        TrainStatus.ON_TIME -> "A TIEMPO"
        TrainStatus.DELAYED -> "+${movement.delayMinutes} MIN"
        TrainStatus.BOARDING -> "EMBARCANDO"
        TrainStatus.DEPARTED -> "SALIDO"
        TrainStatus.CANCELLED -> "SUPRIMIDO"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(74.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = movement.formattedScheduled(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                        textDecoration = if (isDelayed || isCancelled) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    ),
                    color = if (isDelayed || isCancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (isDelayed) {
                    Text(
                        text = movement.formattedEstimated(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = DelayedAmber
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (direction == BoardDirection.DEPARTURES) "DESTINO" else "ORIGEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = movement.originOrDestination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${movement.serviceType} · ${movement.operatorName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "#${movement.trainNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    movement.notes?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            PlatformBadge(
                platform = movement.platform,
                isCancelled = isCancelled
            )
        }
    }
}

@Composable
private fun PlatformBadge(
    platform: String?,
    isCancelled: Boolean
) {
    val bgColor = when {
        isCancelled -> CancelledRed.copy(alpha = 0.2f)
        platform != null -> PlatformAccent
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fgColor = when {
        isCancelled -> CancelledRed
        platform != null -> Color(0xFF111111)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(56.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VÍA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fgColor.copy(alpha = 0.85f)
            )
            Text(
                text = if (isCancelled) "X" else (platform ?: "--"),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum"
                ),
                color = fgColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationSearchSheet(
    query: String,
    results: List<Station>,
    favoriteCodes: Set<String>,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectStation: (Station) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Buscar estación",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Nombre, provincia o código (ej. 11014)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.height(380.dp)
            ) {
                items(results, key = { it.code }) { station ->
                    val isFav = station.code in favoriteCodes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStation(station) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${station.province} · Código ${station.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(station.code) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Guardar favorita",
                                tint = if (isFav) PlatformAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
