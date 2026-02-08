package lol.omnius.android.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Episode
import lol.omnius.android.data.model.Series
import lol.omnius.android.data.model.StreamStartRequest
import lol.omnius.android.data.model.StreamStopRequest
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

    // Streaming state
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()
    private val _streamReady = MutableStateFlow(false)
    val streamReady = _streamReady.asStateFlow()
    private val _streamProgress = MutableStateFlow("")
    val streamProgress = _streamProgress.asStateFlow()
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl = _streamUrl.asStateFlow()
    private val _streamTitle = MutableStateFlow("")
    val streamTitle = _streamTitle.asStateFlow()
    private var pollJob: Job? = null
    private var currentInfoHash: String? = null

    fun load(seriesId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.getApi().getSeriesDetails(seriesId)
                val show = response.data?.series
                _series.value = show
                // If episodes came with the detail response, use them
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
        pollJob?.cancel()
        viewModelScope.launch {
            try {
                _isStreaming.value = true
                _streamReady.value = false
                _streamTitle.value = title
                _streamProgress.value = "Starting stream..."

                val api = ApiClient.getApi()
                val info = api.startStream(StreamStartRequest(hash = hash, fileIndex = fileIndex))
                currentInfoHash = hash

                // Build stream URL - server returns relative path like /stream/hash/0
                val streamPath = info.streamUrl
                val url = if (streamPath.startsWith("http://") || streamPath.startsWith("https://")) {
                    streamPath
                } else if (streamPath.isNotEmpty()) {
                    val base = ApiClient.getBaseUrl().trimEnd('/')
                    "$base${if (streamPath.startsWith("/")) streamPath else "/$streamPath"}"
                } else {
                    ApiClient.streamUrl(hash, info.fileIndex)
                }
                _streamUrl.value = url

                // Poll for readiness
                pollJob = viewModelScope.launch {
                    while (_isStreaming.value && !_streamReady.value) {
                        delay(1000)
                        try {
                            val stats = api.getStreamStatus(hash)
                            val progress = stats.progress // 0-100
                            val speed = stats.downloadSpeed
                            val peers = stats.peers
                            val pct = String.format("%.1f%%", progress)
                            val spd = formatSpeed(speed)
                            _streamProgress.value = "Buffering $pct at $spd • $peers peers"

                            val ready = (progress >= 1.0 && speed > 500_000) ||
                                (progress >= 0.3 && speed > 2_000_000 && peers > 5) ||
                                (progress >= 2.0)

                            if (ready) {
                                _streamReady.value = true
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                _streamProgress.value = e.message ?: "Stream failed"
                _isStreaming.value = false
            }
        }
    }

    fun clearStream() {
        pollJob?.cancel()
        val hash = currentInfoHash
        _isStreaming.value = false
        _streamReady.value = false
        _streamUrl.value = null
        _streamTitle.value = ""
        currentInfoHash = null
        if (hash != null) {
            viewModelScope.launch {
                try { ApiClient.getApi().stopStream(StreamStopRequest(hash)) } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        val hash = currentInfoHash
        if (hash != null) {
            viewModelScope.launch {
                try { ApiClient.getApi().stopStream(StreamStopRequest(hash)) } catch (_: Exception) {}
            }
        }
    }

    private fun formatSpeed(bytes: Long): String = when {
        bytes >= 1_000_000 -> String.format("%.1f MB/s", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.0f KB/s", bytes / 1_000.0)
        else -> "$bytes B/s"
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    onBack: () -> Unit,
    onPlay: (title: String, streamUrl: String, imdbCode: String) -> Unit,
    viewModel: SeriesDetailViewModel = viewModel(),
) {
    val show by viewModel.series.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamReady by viewModel.streamReady.collectAsState()
    val streamProgress by viewModel.streamProgress.collectAsState()
    val streamUrl by viewModel.streamUrl.collectAsState()
    val streamTitle by viewModel.streamTitle.collectAsState()

    LaunchedEffect(seriesId) { viewModel.load(seriesId) }

    // Launch player when stream is ready
    LaunchedEffect(streamReady) {
        if (streamReady) {
            val url = streamUrl ?: return@LaunchedEffect
            val s = show ?: return@LaunchedEffect
            onPlay(streamTitle, url, s.imdbCode)
            viewModel.clearStream()
        }
    }

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

        // Buffering indicator
        if (isStreaming && !streamReady) {
            item {
                Text(
                    text = streamProgress,
                    color = OmniusGold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
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
                    Surface(
                        onClick = { viewModel.selectSeason(seriesId, season) },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
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
                isStreaming = isStreaming,
                onPlay = { hash, fileIndex ->
                    if (!isStreaming) {
                        val title = "${s.title} S${episode.seasonNumber}E${episode.episodeNumber}"
                        viewModel.startEpisodeStream(hash, fileIndex, title)
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
    Surface(
        onClick = {
            val torrent = episode.torrents?.maxByOrNull { it.seeds }
            if (torrent != null) onPlay(torrent.hash, torrent.fileIndex)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
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
