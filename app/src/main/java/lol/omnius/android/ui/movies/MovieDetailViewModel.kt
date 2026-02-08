package lol.omnius.android.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Movie
import lol.omnius.android.data.model.MovieDetails
import lol.omnius.android.torrent.TorrentStreamManager
import lol.omnius.android.torrent.TorrentStreamState

data class MovieDetailState(
    val isLoading: Boolean = true,
    val movie: MovieDetails? = null,
    val error: String? = null,
    // Related content
    val franchiseMovies: List<Movie> = emptyList(),
    val suggestions: List<Movie> = emptyList(),
    // Streaming (from TorrentStreamManager)
    val selectedHash: String? = null,
    val torrentState: TorrentStreamState = TorrentStreamState(),
)

class MovieDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(MovieDetailState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            TorrentStreamManager.state.collect { torrentState ->
                _state.value = _state.value.copy(torrentState = torrentState)
            }
        }
    }

    fun loadMovie(movieId: Int) {
        viewModelScope.launch {
            try {
                _state.value = MovieDetailState(isLoading = true)
                val api = ApiClient.getApi()
                val response = api.getMovieDetails(movieId)
                _state.value = MovieDetailState(
                    isLoading = false,
                    movie = response.data?.movie,
                )

                // Load related content in background
                launch {
                    try {
                        val franchise = api.getFranchiseMovies(movieId)
                        _state.value = _state.value.copy(
                            franchiseMovies = franchise.data?.movies ?: emptyList(),
                        )
                    } catch (_: Exception) {}
                }
                launch {
                    try {
                        val suggestions = api.getMovieSuggestions(movieId)
                        _state.value = _state.value.copy(
                            suggestions = suggestions.data?.movies ?: emptyList(),
                        )
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                _state.value = MovieDetailState(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun startStream(torrentHash: String, title: String) {
        _state.value = _state.value.copy(selectedHash = torrentHash)
        TorrentStreamManager.startStream(torrentHash, title)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
