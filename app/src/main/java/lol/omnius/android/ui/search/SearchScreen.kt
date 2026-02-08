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
import lol.omnius.android.ui.theme.OmniusSurface

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _results = MutableStateFlow<List<Movie>>(emptyList())
    val results = _results.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
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

        Spacer(Modifier.height(16.dp))

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
