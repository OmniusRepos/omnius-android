package lol.omnius.android.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.FavSeries
import lol.omnius.android.data.FavoritesManager
import lol.omnius.android.data.model.Series
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.components.FavoriteDialog
import lol.omnius.android.ui.movies.FilterChip
import lol.omnius.android.ui.theme.OmniusRed
import lol.omnius.android.ui.theme.OmniusSurface

enum class SeriesSortOption(val label: String, val apiValue: String, val order: String = "desc") {
    TOP_RATED("Top Rated", "rating"),
    LATEST("Latest", "date_added"),
    ONGOING("Ongoing", "rating"),  // uses status filter
}

val SERIES_GENRES = listOf(
    "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime",
    "Documentary", "Drama", "Fantasy", "History", "Horror", "Mystery",
    "Romance", "Sci-Fi", "Thriller", "War",
)

data class SeriesBrowseState(
    val isLoading: Boolean = true,
    val series: List<Series> = emptyList(),
    val selectedGenre: String? = null,
    val selectedSort: SeriesSortOption = SeriesSortOption.TOP_RATED,
    val query: String = "",
    val page: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
)

class SeriesBrowseViewModel : ViewModel() {
    private val _state = MutableStateFlow(SeriesBrowseState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadSeries()
    }

    fun loadSeries(reset: Boolean = false) {
        viewModelScope.launch {
            val currentState = _state.value
            val page = if (reset) 1 else currentState.page

            _state.value = currentState.copy(
                isLoading = true,
                error = null,
                page = page,
                series = if (reset) emptyList() else currentState.series,
            )

            try {
                val sort = _state.value.selectedSort
                val response = ApiClient.getApi().listSeries(
                    limit = 20,
                    page = page,
                    genre = _state.value.selectedGenre,
                    sortBy = sort.apiValue,
                    orderBy = sort.order,
                    status = if (sort == SeriesSortOption.ONGOING) "Continuing" else null,
                    queryTerm = _state.value.query.trim().takeIf { it.isNotEmpty() },
                )
                val newSeries = response.data?.series ?: emptyList()

                _state.value = _state.value.copy(
                    isLoading = false,
                    series = if (reset) newSeries else _state.value.series + newSeries,
                    hasMore = newSeries.size >= 20,
                    page = page + 1,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun selectGenre(genre: String?) {
        _state.value = _state.value.copy(selectedGenre = genre)
        loadSeries(reset = true)
    }

    fun selectSort(sort: SeriesSortOption) {
        _state.value = _state.value.copy(selectedSort = sort)
        loadSeries(reset = true)
    }

    fun updateQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            loadSeries(reset = true)
        }
    }

    fun loadMore() {
        if (!_state.value.isLoading && _state.value.hasMore) {
            loadSeries()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesBrowseScreen(
    onSeriesClick: (Int) -> Unit,
    viewModel: SeriesBrowseViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val favSeries by FavoritesManager.series.collectAsState()
    var favDialogSeries by remember { mutableStateOf<FavSeries?>(null) }

    favDialogSeries?.let { s ->
        FavoriteDialog(
            title = s.title,
            isFavorite = FavoritesManager.isSeriesFav(s.id),
            onToggle = { FavoritesManager.toggleSeries(s) },
            onDismiss = { favDialogSeries = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TV Series",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        // Search input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(OmniusSurface, RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = state.query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                ),
                decorationBox = { innerTextField ->
                    if (state.query.isEmpty()) {
                        Text("Search TV series (EZTV)…", color = Color(0xFF666666), fontSize = 14.sp)
                    }
                    innerTextField()
                },
            )
        }

        // Sort chips
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(SeriesSortOption.entries.toList()) { sort ->
                FilterChip(
                    label = sort.label,
                    selected = state.selectedSort == sort,
                    onClick = { viewModel.selectSort(sort) },
                    color = Color(0xFF6366F1),
                )
            }
        }

        // Genre chips
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            item {
                FilterChip(
                    label = "All",
                    selected = state.selectedGenre == null,
                    onClick = { viewModel.selectGenre(null) },
                )
            }
            items(SERIES_GENRES) { genre ->
                FilterChip(
                    label = genre,
                    selected = state.selectedGenre == genre,
                    onClick = { viewModel.selectGenre(genre) },
                )
            }
        }

        // Series grid
        if (state.error != null && state.series.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = OmniusRed, fontSize = 14.sp)
            }
        } else if (state.isLoading && state.series.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.White)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.series, key = { it.id }) { show ->
                    ContentCard(
                        title = show.title,
                        imageUrl = show.posterImage,
                        rating = show.rating,
                        year = show.year,
                        onClick = { onSeriesClick(show.id) },
                        onLongClick = {
                            favDialogSeries = FavSeries(show.id, show.title, show.posterImage, show.year, show.rating)
                        },
                        isFavorite = FavoritesManager.isSeriesFav(show.id),
                    )
                }

                // Load more trigger
                if (state.hasMore && !state.isLoading) {
                    item {
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                    }
                }
            }
        }
    }
}
