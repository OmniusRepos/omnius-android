package lol.omnius.android.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.tv.material3.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Movie
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.theme.OmniusCard
import lol.omnius.android.ui.theme.OmniusRed
import lol.omnius.android.ui.theme.OmniusSurface

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _results = MutableStateFlow<List<Movie>>(emptyList())
    val results = _results.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    private val _isOnlineSearching = MutableStateFlow(false)
    val isOnlineSearching = _isOnlineSearching.asStateFlow()
    private val _onlineMessage = MutableStateFlow<String?>(null)
    val onlineMessage = _onlineMessage.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        _onlineMessage.value = null
        searchJob?.cancel()
        if (q.length < 2) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            try {
                _isSearching.value = true
                val response = ApiClient.getApi().listMovies(queryTerm = q, limit = 30)
                _results.value = response.data?.movies ?: emptyList()
            } catch (_: Exception) {}
            _isSearching.value = false
        }
    }

    fun searchOnline() {
        val q = _query.value.trim()
        if (q.length < 2 || _isOnlineSearching.value) return
        viewModelScope.launch {
            try {
                _isOnlineSearching.value = true
                _onlineMessage.value = null
                val resp = ApiClient.getApi().searchOnline(queryTerm = q, limit = 5)
                val movies = resp.data?.movies ?: emptyList()
                _results.value = movies
                _onlineMessage.value = if (movies.isEmpty()) "No online results" else "Imported ${movies.size} result(s)"
            } catch (_: Exception) {
                _onlineMessage.value = "Online search failed"
            }
            _isOnlineSearching.value = false
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
    val isOnlineSearching by viewModel.isOnlineSearching.collectAsState()
    val onlineMessage by viewModel.onlineMessage.collectAsState()

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

        Spacer(Modifier.height(12.dp))

        if (query.length >= 2) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var focused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(8.dp)
                Surface(
                    onClick = { viewModel.searchOnline() },
                    modifier = Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .then(
                            if (focused) Modifier.border(BorderStroke(2.dp, OmniusRed), shape)
                            else Modifier
                        ),
                    shape = ClickableSurfaceDefaults.shape(shape = shape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = OmniusCard,
                        focusedContainerColor = OmniusRed.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        text = if (isOnlineSearching) "Searching online..." else "Search online (YTS)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                onlineMessage?.let { msg ->
                    Spacer(Modifier.width(12.dp))
                    Text(msg, color = Color(0xFF888888), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (isSearching) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Searching...", color = Color(0xFF888888), fontSize = 14.sp)
            }
        }

        if (results.isEmpty() && query.length >= 2 && !isSearching) {
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
                items(results, key = { it.id }) { movie ->
                    ContentCard(
                        title = movie.title,
                        imageUrl = movie.mediumCoverImage,
                        rating = movie.rating,
                        year = movie.year,
                        onClick = { onMovieClick(movie.id) },
                    )
                }
            }
        }
    }
}
