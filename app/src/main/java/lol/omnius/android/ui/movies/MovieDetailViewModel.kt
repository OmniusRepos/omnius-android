package lol.omnius.android.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.MovieDetails
import lol.omnius.android.data.model.StreamInfo
import lol.omnius.android.data.model.StreamStartRequest
import lol.omnius.android.data.model.StreamStats
import lol.omnius.android.data.model.StreamStopRequest

data class MovieDetailState(
    val isLoading: Boolean = true,
    val movie: MovieDetails? = null,
    val error: String? = null,
    // Streaming
    val isStreaming: Boolean = false,
    val streamInfo: StreamInfo? = null,
    val streamStats: StreamStats? = null,
    val streamReady: Boolean = false,
    val streamError: String? = null,
    val isStuck: Boolean = false,
    val selectedHash: String? = null,
)

class MovieDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(MovieDetailState())
    val state = _state.asStateFlow()
    private var pollJob: Job? = null
    private var requestHash: String? = null

    fun loadMovie(movieId: Int) {
        viewModelScope.launch {
            try {
                _state.value = MovieDetailState(isLoading = true)
                val response = ApiClient.getApi().getMovieDetails(movieId)
                _state.value = MovieDetailState(
                    isLoading = false,
                    movie = response.data?.movie,
                )
            } catch (e: Exception) {
                _state.value = MovieDetailState(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun startStream(torrentHash: String, fileIndex: Int? = null) {
        pollJob?.cancel()
        requestHash = torrentHash

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isStreaming = true,
                    streamError = null,
                    streamReady = false,
                    isStuck = false,
                    selectedHash = torrentHash,
                )

                val api = ApiClient.getApi()
                val info = api.startStream(StreamStartRequest(hash = torrentHash, fileIndex = fileIndex))
                _state.value = _state.value.copy(streamInfo = info)

                // Poll for readiness — use request hash since response doesn't include info_hash
                val pollHash = torrentHash
                var stuckTimer = 0
                pollJob = viewModelScope.launch {
                    while (_state.value.isStreaming && !_state.value.streamReady) {
                        delay(1000)
                        try {
                            val stats = api.getStreamStatus(pollHash)
                            _state.value = _state.value.copy(streamStats = stats)

                            // progress is 0-100 from server
                            val progress = stats.progress
                            val speed = stats.downloadSpeed
                            val peers = stats.peers

                            // Buffer readiness:
                            // 1. Progress >= 1% with speed > 0.5 MB/s
                            // 2. Progress >= 0.3% with speed > 2 MB/s and peers > 5
                            // 3. Progress >= 2% regardless
                            val ready = (progress >= 1.0 && speed > 500_000) ||
                                (progress >= 0.3 && speed > 2_000_000 && peers > 5) ||
                                (progress >= 2.0)

                            if (ready) {
                                _state.value = _state.value.copy(streamReady = true)
                                break
                            }

                            // Stuck detection: 15s with no progress and < 2 peers
                            stuckTimer++
                            if (stuckTimer >= 15 && progress == 0.0 && peers < 2) {
                                _state.value = _state.value.copy(isStuck = true)
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isStreaming = false,
                    streamError = e.message,
                )
            }
        }
    }

    fun stopStream() {
        pollJob?.cancel()
        viewModelScope.launch {
            val hash = requestHash ?: return@launch
            _state.value = _state.value.copy(
                isStreaming = false,
                streamReady = false,
                isStuck = false,
                selectedHash = null,
            )
            requestHash = null
            try {
                ApiClient.getApi().stopStream(StreamStopRequest(hash))
            } catch (_: Exception) {}
        }
    }

    fun getStreamUrl(): String? {
        val info = _state.value.streamInfo
        // stream_url from server is relative like /stream/hash/0
        val apiUrl = info?.streamUrl ?: ""
        if (apiUrl.isNotEmpty()) {
            if (apiUrl.startsWith("http://") || apiUrl.startsWith("https://")) {
                return apiUrl
            }
            // Relative path — prepend base URL
            val base = ApiClient.getBaseUrl().trimEnd('/')
            return "$base${if (apiUrl.startsWith("/")) apiUrl else "/$apiUrl"}"
        }
        // Fallback: construct from hash
        val hash = requestHash ?: return null
        return ApiClient.streamUrl(hash, info?.fileIndex ?: 0)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        val hash = requestHash
        if (hash != null && _state.value.isStreaming) {
            viewModelScope.launch {
                try { ApiClient.getApi().stopStream(StreamStopRequest(hash)) } catch (_: Exception) {}
            }
        }
    }
}
