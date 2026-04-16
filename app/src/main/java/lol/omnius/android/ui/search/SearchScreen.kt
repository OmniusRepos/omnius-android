package lol.omnius.android.ui.search

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
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.movies.FilterChip
import lol.omnius.android.ui.theme.OmniusSurface

enum class SearchTab(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("TV Series"),
}

data class SearchResult(
    val id: Int?,
    val imdbId: String,
    val title: String,
    val imageUrl: String?,
    val year: Int,
    val rating: Double,
    val type: String, // "movie" or "tvSeries"
    val isLocal: Boolean, // already in our library
)

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results = _results.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    private val _tab = MutableStateFlow(SearchTab.ALL)
    val tab = _tab.asStateFlow()
    private val _syncingId = MutableStateFlow<String?>(null)
    val syncingId = _syncingId.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.length < 2) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            try {
                _isSearching.value = true
                val api = ApiClient.getApi()

                // Search local library and IMDB in parallel
                val localJob = launch {
                    try {
                        val response = api.unifiedSearch(query = q, limit = 30)
                        val localMovies = (response.data?.movies ?: emptyList()).map { m ->
                            SearchResult(m.id, m.imdbCode ?: "", m.title, m.mediumCoverImage, m.year, m.rating ?: 0.0, "movie", true)
                        }
                        val localSeries = (response.data?.series ?: emptyList()).map { s ->
                            SearchResult(s.id, s.imdbCode ?: "", s.title, s.posterImage, s.year, s.rating?.toDouble() ?: 0.0, "tvSeries", true)
                        }
                        val combined = localMovies + localSeries
                        _results.value = combined
                    } catch (_: Exception) {}
                }

                val imdbJob = launch {
                    try {
                        val imdbResponse = api.searchIMDB(query = q)
                        val localResults = _results.value
                        val localImdbIds = localResults.map { it.imdbId }.toSet()

                        val imdbOnly = imdbResponse.titles
                            .filter { it.id !in localImdbIds && (it.type == "movie" || it.type == "tvSeries" || it.type == "tvMiniSeries") }
                            .map { t ->
                                SearchResult(
                                    id = null,
                                    imdbId = t.id,
                                    title = t.primaryTitle,
                                    imageUrl = t.primaryImage?.url,
                                    year = t.startYear,
                                    rating = t.rating?.aggregateRating ?: 0.0,
                                    type = t.type,
                                    isLocal = false,
                                )
                            }

                        _results.value = localResults + imdbOnly
                    } catch (_: Exception) {}
                }

                localJob.join()
                imdbJob.join()
            } catch (_: Exception) {}
            _isSearching.value = false
        }
    }

    fun selectTab(t: SearchTab) {
        _tab.value = t
    }

    fun syncAndOpen(result: SearchResult, onMovieReady: (Int) -> Unit, onSeriesReady: (Int) -> Unit) {
        viewModelScope.launch {
            _syncingId.value = result.imdbId
            try {
                val api = ApiClient.getApi()
                if (result.type == "movie") {
                    val response = api.syncMovie(mapOf("imdb_code" to result.imdbId))
                    val movieId = response.data?.id ?: response.data?.movieId
                    if (movieId != null) {
                        try { api.refreshMovie(mapOf("movie_id" to movieId)) } catch (_: Exception) {}
                        onMovieReady(movieId)
                    }
                } else {
                    val response = api.syncSeries(mapOf("imdb_code" to result.imdbId))
                    val seriesId = response.data?.id ?: response.data?.seriesId
                    if (seriesId != null) {
                        try { api.refreshSeries(mapOf("series_id" to seriesId)) } catch (_: Exception) {}
                        onSeriesReady(seriesId)
                    }
                }
            } catch (_: Exception) {}
            _syncingId.value = null
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val syncingId by viewModel.syncingId.collectAsState()

    val movieResults = results.filter { it.type == "movie" }
    val seriesResults = results.filter { it.type != "movie" }
    val hasResults = results.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp),
        )

        // Search input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(OmniusSurface, RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Search movies, series...", color = Color(0xFF666666), fontSize = 16.sp)
                    }
                    innerTextField()
                },
            )
        }

        if (hasResults) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchTab.entries.forEach { t ->
                    val count = when (t) {
                        SearchTab.ALL -> results.size
                        SearchTab.MOVIES -> movieResults.size
                        SearchTab.SERIES -> seriesResults.size
                    }
                    if (count > 0 || t == SearchTab.ALL) {
                        FilterChip(
                            label = "${t.label} ($count)",
                            selected = tab == t,
                            onClick = { viewModel.selectTab(t) },
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }

        if (isSearching) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Searching...", color = Color(0xFF888888), fontSize = 14.sp)
            }
        }

        if (!hasResults && query.length >= 2 && !isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = Color(0xFF888888), fontSize = 14.sp)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Section headers in ALL tab
                if (tab == SearchTab.ALL && movieResults.isNotEmpty() && seriesResults.isNotEmpty()) {
                    item(span = { TvGridItemSpan(maxLineSpan) }) {
                        Text("Movies", color = Color(0xFF888888), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                if (tab == SearchTab.ALL || tab == SearchTab.MOVIES) {
                    items(movieResults, key = { "m_${it.imdbId}" }) { result ->
                        val isSyncing = syncingId == result.imdbId
                        ContentCard(
                            title = if (isSyncing) "Adding..." else result.title,
                            imageUrl = result.imageUrl,
                            rating = result.rating,
                            year = result.year,
                            badge = if (!result.isLocal) "IMDB" else null,
                            onClick = {
                                if (result.isLocal && result.id != null) {
                                    onMovieClick(result.id)
                                } else if (!isSyncing) {
                                    viewModel.syncAndOpen(result, onMovieReady = onMovieClick, onSeriesReady = onSeriesClick)
                                }
                            },
                        )
                    }
                }

                if (tab == SearchTab.ALL && movieResults.isNotEmpty() && seriesResults.isNotEmpty()) {
                    item(span = { TvGridItemSpan(maxLineSpan) }) {
                        Text("TV Series", color = Color(0xFF888888), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                if (tab == SearchTab.ALL || tab == SearchTab.SERIES) {
                    items(seriesResults, key = { "s_${it.imdbId}" }) { result ->
                        val isSyncing = syncingId == result.imdbId
                        ContentCard(
                            title = if (isSyncing) "Adding..." else result.title,
                            imageUrl = result.imageUrl,
                            rating = result.rating,
                            year = result.year,
                            badge = if (!result.isLocal) "IMDB" else null,
                            onClick = {
                                if (result.isLocal && result.id != null) {
                                    onSeriesClick(result.id)
                                } else if (!isSyncing) {
                                    viewModel.syncAndOpen(result, onMovieReady = onMovieClick, onSeriesReady = onSeriesClick)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
