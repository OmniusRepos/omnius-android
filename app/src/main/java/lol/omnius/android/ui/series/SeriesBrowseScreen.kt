package lol.omnius.android.ui.series

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Series
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.theme.OmniusRed

class SeriesBrowseViewModel : ViewModel() {
    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series = _series.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.getApi().listSeries(limit = 40, sortBy = "rating")
                _series.value = response.data?.series ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesBrowseScreen(
    onSeriesClick: (Int) -> Unit,
    viewModel: SeriesBrowseViewModel = viewModel(),
) {
    val series by viewModel.series.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TV Series",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        )

        if (isLoading) {
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
                items(series, key = { it.id }) { show ->
                    ContentCard(
                        title = show.title,
                        imageUrl = show.posterImage,
                        rating = show.rating,
                        year = show.year,
                        onClick = { onSeriesClick(show.id) },
                    )
                }
            }
        }
    }
}
