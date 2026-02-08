package lol.omnius.android.ui.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Episode
import lol.omnius.android.data.model.Series
import lol.omnius.android.torrent.TorrentStreamManager
import lol.omnius.android.torrent.TorrentStreamState
import lol.omnius.android.ui.theme.*

class SeriesDetailViewModel : ViewModel() {
    private val _series = MutableStateFlow<Series?>(null)
    val series = _series.asStateFlow()
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()
    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason = _selectedSeason.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Torrent streaming state
    private val _torrentState = MutableStateFlow(TorrentStreamState())
    val torrentState = _torrentState.asStateFlow()

    init {
        viewModelScope.launch {
            TorrentStreamManager.state.collect { state ->
                _torrentState.value = state
            }
        }
    }

    fun load(seriesId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.getApi().getSeriesDetails(seriesId)
                val show = response.data?.series
                _series.value = show
                val eps = response.data?.episodes
                if (!eps.isNullOrEmpty()) {
                    _episodes.value = eps
                } else if (show != null && show.totalSeasons > 0) {
                    loadSeason(seriesId, 1)
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun selectSeason(seriesId: Int, season: Int) {
        _selectedSeason.value = season
        loadSeason(seriesId, season)
    }

    private fun loadSeason(seriesId: Int, season: Int) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApi().getSeasonEpisodes(seriesId, season)
                _episodes.value = response.data ?: emptyList()
            } catch (_: Exception) {
                _episodes.value = emptyList()
            }
        }
    }

    fun startEpisodeStream(hash: String, fileIndex: Int?, title: String) {
        TorrentStreamManager.startStream(hash, title, fileIndex)
    }

    override fun onCleared() {
        super.onCleared()
        if (_torrentState.value.isStreaming) {
            TorrentStreamManager.stopStream()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    onBack: () -> Unit,
    onPlay: (title: String, streamUrl: String, imdbCode: String, isTorrent: Boolean) -> Unit,
    viewModel: SeriesDetailViewModel = viewModel(),
) {
    val show by viewModel.series.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val torrentState by viewModel.torrentState.collectAsState()

    LaunchedEffect(seriesId) { viewModel.load(seriesId) }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
        return
    }

    val s = show ?: return

    TvLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Backdrop
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
            ) {
                AsyncImage(
                    model = s.backgroundImage,
                    contentDescription = s.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, OmniusDark.copy(alpha = 0.8f), OmniusDark),
                            )
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                ) {
                    Text(s.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${s.year}${s.endYear?.let { " - $it" } ?: ""} • ${s.totalSeasons} Seasons • ${s.status}",
                        color = OmniusTextSecondary, fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(s.genres.joinToString(", "), color = OmniusGold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(s.summary, color = Color(0xFFBBBBBB), fontSize = 13.sp, maxLines = 3)
                }
            }
        }

        // Season tabs
        item {
            TvLazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                items((1..s.totalSeasons).toList()) { season ->
                    var seasonFocused by remember { mutableStateOf(false) }
                    val seasonShape = RoundedCornerShape(8.dp)
                    Surface(
                        onClick = { viewModel.selectSeason(seriesId, season) },
                        modifier = Modifier
                            .onFocusChanged { seasonFocused = it.isFocused }
                            .then(
                                if (seasonFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), seasonShape)
                                else Modifier
                            ),
                        shape = ClickableSurfaceDefaults.shape(shape = seasonShape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (season == selectedSeason) OmniusRed else OmniusSurface,
                        ),
                    ) {
                        Text(
                            "Season $season",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // Episodes
        items(episodes, key = { it.id }) { episode ->
            EpisodeRow(
                episode = episode,
                isStreaming = torrentState.isStreaming,
                onPlay = { hash, fileIndex ->
                    if (!torrentState.isStreaming) {
                        val title = "${s.title} S${episode.seasonNumber}E${episode.episodeNumber}"
                        viewModel.startEpisodeStream(hash, fileIndex, title)
                        onPlay(title, "", s.imdbCode, true)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRow(
    episode: Episode,
    isStreaming: Boolean,
    onPlay: (hash: String, fileIndex: Int?) -> Unit,
) {
    var episodeFocused by remember { mutableStateOf(false) }
    val episodeShape = RoundedCornerShape(8.dp)
    Surface(
        onClick = {
            val torrent = episode.torrents?.maxByOrNull { it.seeds }
            if (torrent != null) onPlay(torrent.hash, torrent.fileIndex)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .onFocusChanged { episodeFocused = it.isFocused }
            .then(
                if (episodeFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), episodeShape)
                else Modifier
            ),
        shape = ClickableSurfaceDefaults.shape(shape = episodeShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = OmniusSurface,
            focusedContainerColor = OmniusCard,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (episode.stillImage != null) {
                AsyncImage(
                    model = episode.stillImage,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(68.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNumber} - ${episode.title}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (episode.summary != null) {
                    Text(
                        text = episode.summary,
                        color = OmniusTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                    )
                }
                val best = episode.torrents?.maxByOrNull { it.seeds }
                if (best != null) {
                    Text(
                        text = "${best.quality} • ${best.size} • ${best.seeds} seeds",
                        color = OmniusGold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

