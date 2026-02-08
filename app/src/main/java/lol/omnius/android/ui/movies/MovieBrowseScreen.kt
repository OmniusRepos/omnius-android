package lol.omnius.android.ui.movies

import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.theme.OmniusRed
import lol.omnius.android.ui.theme.OmniusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieBrowseScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: MovieBrowseViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Movies",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        // Genre chips
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            item {
                GenreChip(
                    label = "All",
                    selected = state.selectedGenre == null,
                    onClick = { viewModel.selectGenre(null) },
                )
            }
            items(MOVIE_GENRES) { genre ->
                GenreChip(
                    label = genre,
                    selected = state.selectedGenre == genre,
                    onClick = { viewModel.selectGenre(genre) },
                )
            }
        }

        // Movie grid
        if (state.error != null && state.movies.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = OmniusRed, fontSize = 14.sp)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.movies, key = { it.id }) { movie ->
                    ContentCard(
                        title = movie.title,
                        imageUrl = movie.mediumCoverImage,
                        rating = movie.rating,
                        year = movie.year,
                        onClick = { onMovieClick(movie.id) },
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GenreChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var chipFocused by remember { mutableStateOf(false) }
    val chipShape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { chipFocused = it.isFocused }
            .then(
                if (chipFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), chipShape)
                else Modifier
            ),
        shape = ClickableSurfaceDefaults.shape(shape = chipShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) OmniusRed else OmniusSurface,
            focusedContainerColor = if (selected) OmniusRed else Color(0xFF333333),
        ),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
