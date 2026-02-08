package lol.omnius.android.ui.movies

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import lol.omnius.android.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    onPlay: (title: String, streamUrl: String, imdbCode: String) -> Unit,
    viewModel: MovieDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
    }

    // When stream is ready, launch player
    LaunchedEffect(state.streamReady) {
        if (state.streamReady) {
            val url = viewModel.getStreamUrl() ?: return@LaunchedEffect
            val movie = state.movie ?: return@LaunchedEffect
            onPlay(movie.title, url, movie.imdbCode)
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
        return
    }

    val movie = state.movie
    if (movie == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: "Movie not found", color = OmniusRed)
        }
        return
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Backdrop + Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            ) {
                AsyncImage(
                    model = movie.backgroundImageOriginal.ifEmpty { movie.backgroundImage },
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OmniusDark.copy(alpha = 0.8f),
                                    OmniusDark,
                                ),
                            )
                        ),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 24.dp, end = 24.dp),
                ) {
                    // Poster
                    AsyncImage(
                        model = movie.largeCoverImage.ifEmpty { movie.mediumCoverImage },
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(140.dp)
                            .aspectRatio(2f / 3f),
                    )

                    Spacer(Modifier.width(24.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))

                        val meta = buildList {
                            add(movie.year.toString())
                            if (movie.runtime > 0) add("${movie.runtime}min")
                            if (movie.mpaRating != null) add(movie.mpaRating)
                        }
                        Text(
                            text = meta.joinToString(" • "),
                            color = OmniusTextSecondary,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = movie.genres.joinToString(", "),
                            color = OmniusGold,
                            fontSize = 13.sp,
                        )

                        // Ratings
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val imdb = movie.imdbRating ?: movie.rating
                            if (imdb > 0) {
                                Text("IMDB $imdb", color = OmniusGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            movie.rottenTomatoes?.let {
                                Text("RT $it%", color = OmniusGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Synopsis
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = movie.synopsis.ifEmpty { movie.summary },
                            color = Color(0xFFBBBBBB),
                            fontSize = 13.sp,
                            maxLines = 4,
                        )
                    }
                }
            }
        }

        // Quality selector / Play buttons
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = "Available Qualities",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                if (state.isStreaming && !state.streamReady) {
                    val stats = state.streamStats
                    val progress = stats?.progress?.let { String.format("%.1f%%", it) } ?: "0%"
                    val speed = stats?.downloadSpeed?.let { formatBytes(it) + "/s" } ?: "0 B/s"
                    val peers = stats?.peers ?: 0
                    Text(
                        text = "Buffering $progress at $speed • $peers peers",
                        color = OmniusGold,
                        fontSize = 14.sp,
                    )
                    if (state.isStuck) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Low peer count — this may take longer",
                            color = Color(0xFFFF8800),
                            fontSize = 12.sp,
                        )
                    }
                }

                if (state.streamError != null) {
                    Text(
                        text = state.streamError!!,
                        color = OmniusRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(movie.torrents, key = { it.hash }) { torrent ->
                        val isSelected = state.selectedHash == torrent.hash
                        Surface(
                            onClick = {
                                if (!state.isStreaming) {
                                    viewModel.startStream(torrent.hash)
                                }
                            },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) OmniusGold else OmniusRed,
                                focusedContainerColor = if (isSelected) OmniusGold.copy(alpha = 0.8f) else OmniusRed.copy(alpha = 0.8f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = torrent.quality,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = "${torrent.size} • ${torrent.seeds} seeds",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cast
        if (movie.cast.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Cast",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(movie.cast) { castMember ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(80.dp),
                            ) {
                                AsyncImage(
                                    model = castMember.urlSmallImage,
                                    contentDescription = castMember.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = castMember.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                )
                                Text(
                                    text = castMember.characterName,
                                    color = OmniusTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
