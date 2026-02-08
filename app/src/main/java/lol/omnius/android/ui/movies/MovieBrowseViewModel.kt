package lol.omnius.android.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.Movie

data class MovieBrowseState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val selectedGenre: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
)

val MOVIE_GENRES = listOf(
    "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Fantasy", "History", "Horror",
    "Music", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western",
)

class MovieBrowseViewModel : ViewModel() {
    private val _state = MutableStateFlow(MovieBrowseState())
    val state = _state.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies(reset: Boolean = false) {
        viewModelScope.launch {
            val currentState = _state.value
            val page = if (reset) 1 else currentState.page

            _state.value = currentState.copy(
                isLoading = true,
                error = null,
                page = page,
                movies = if (reset) emptyList() else currentState.movies,
            )

            try {
                val response = ApiClient.getApi().listMovies(
                    limit = 20,
                    page = page,
                    genre = currentState.selectedGenre,
                    sortBy = "download_count",
                )
                val newMovies = response.data?.movies ?: emptyList()

                _state.value = _state.value.copy(
                    isLoading = false,
                    movies = if (reset) newMovies else _state.value.movies + newMovies,
                    hasMore = newMovies.size >= 20,
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
        loadMovies(reset = true)
    }

    fun loadMore() {
        if (!_state.value.isLoading && _state.value.hasMore) {
            loadMovies()
        }
    }
}
