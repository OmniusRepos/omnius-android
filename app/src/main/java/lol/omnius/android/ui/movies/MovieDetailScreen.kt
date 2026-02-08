package lol.omnius.android.ui.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import lol.omnius.android.ui.components.ContentCard
import lol.omnius.android.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    onPlay: (title: String, streamUrl: String, imdbCode: String, isTorrent: Boolean) -> Unit,
    onMovieClick: ((Int) -> Unit)? = null,
    viewModel: MovieDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error ?: "Movie not found", color = OmniusRed, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                var retryFocused by remember { mutableStateOf(false) }
                val retryShape = RoundedCornerShape(8.dp)
                Surface(
                    onClick = { viewModel.loadMovie(movieId) },
                    modifier = Modifier
                        .onFocusChanged { retryFocused = it.isFocused }
                        .then(
                            if (retryFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), retryShape)
                            else Modifier
                        ),
                    shape = ClickableSurfaceDefaults.shape(shape = retryShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                    colors = ClickableSurfaceDefaults.colors(containerColor = OmniusSurface),
                ) {
                    Text(
                        "Retry",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed hero section: backdrop + poster + info (always visible)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            // Backdrop
            val bgImage = movie.backgroundImageOriginal.ifEmpty {
                movie.backgroundImage.ifEmpty {
                    movie.largeCoverImage.ifEmpty { movie.mediumCoverImage }
                }
            }
            AsyncImage(
                model = bgImage,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Horizontal gradient (left dark for text readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                OmniusDark.copy(alpha = 0.95f),
                                OmniusDark.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                            startX = 0f,
                            endX = 900f,
                        )
                    ),
            )
            // Vertical gradient (bottom dark)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                OmniusDark.copy(alpha = 0.5f),
                                Color.Transparent,
                                OmniusDark.copy(alpha = 0.8f),
                                OmniusDark,
                            ),
                        )
                    ),
            )

            // Content: poster + info
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 16.dp, end = 32.dp),
            ) {
                // Poster
                AsyncImage(
                    model = movie.largeCoverImage.ifEmpty { movie.mediumCoverImage },
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(130.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                )

                Spacer(Modifier.width(24.dp))

                // Info column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 600.dp),
                ) {
                    // Title
                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 28.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Meta: year, runtime, language
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(movie.year.toString(), color = Color.White, fontSize = 13.sp)
                        if (movie.runtime > 0) {
                            val hours = movie.runtime / 60
                            val mins = movie.runtime % 60
                            val duration = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            MetaDot()
                            Text(duration, color = OmniusTextSecondary, fontSize = 13.sp)
                        }
                        if (!movie.mpaRating.isNullOrEmpty()) {
                            MetaDot()
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF333333), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(movie.mpaRating!!, color = Color.White, fontSize = 11.sp)
                            }
                        }
                        if (movie.language.isNotEmpty()) {
                            MetaDot()
                            Text(movie.language.uppercase(), color = OmniusTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Ratings + Genre tags in one row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val imdb = movie.imdbRating ?: movie.rating
                        if (imdb > 0) {
                            RatingBadge("IMDb", String.format("%.1f", imdb), OmniusGold)
                        }
                        movie.rottenTomatoes?.let {
                            RatingBadge("RT", "$it%", OmniusRed)
                        }
                        if (movie.genres.isNotEmpty()) {
                            Text(
                                text = movie.genres.take(3).joinToString(" \u2022 "),
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                            )
                        }
                    }

                    // Synopsis
                    val description = movie.synopsis.ifEmpty {
                        movie.descriptionFull.ifEmpty { movie.summary }
                    }
                    if (description.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = description,
                            color = Color(0xFFBBBBBB),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        // Scrollable content below hero
        TvLazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            // Quality buttons + Play
            item {
            Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)) {
                // Torrent quality buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    movie.torrents.forEach { torrent ->
                        val isSelected = state.selectedHash == torrent.hash
                        var torrentFocused by remember { mutableStateOf(false) }
                        val torrentShape = RoundedCornerShape(8.dp)
                        Surface(
                            onClick = {
                                if (!state.torrentState.isStreaming) {
                                    viewModel.startStream(torrent.hash, movie.title)
                                    onPlay(movie.title, "", movie.imdbCode, true)
                                }
                            },
                            modifier = Modifier
                                .onFocusChanged { torrentFocused = it.isFocused }
                                .then(
                                    if (torrentFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), torrentShape)
                                    else Modifier
                                ),
                            shape = ClickableSurfaceDefaults.shape(shape = torrentShape),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) OmniusGold else OmniusSurface,
                                focusedContainerColor = if (isSelected) OmniusGold.copy(alpha = 0.8f) else OmniusRed,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "\u25B6  ${torrent.quality}",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = "${torrent.size} \u2022 ${torrent.videoCodec ?: torrent.type ?: ""}",
                                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                )
                                Text(
                                    text = "${torrent.seeds} seeds \u2022 ${torrent.peers} peers",
                                    color = if (isSelected) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
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
                Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                    Text(
                        text = "Cast",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 32.dp),
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
                                        .size(64.dp)
                                        .clip(CircleShape),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = castMember.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = castMember.characterName,
                                    color = OmniusTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Franchise movies
        if (state.franchiseMovies.isNotEmpty()) {
            item {
                val franchiseName = movie.franchise ?: "Collection"
                Column(modifier = Modifier.padding(start = 32.dp, top = 16.dp)) {
                    Text(
                        text = franchiseName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 32.dp),
                    ) {
                        items(state.franchiseMovies, key = { it.id }) { m ->
                            ContentCard(
                                title = m.title,
                                imageUrl = m.mediumCoverImage,
                                rating = m.rating,
                                year = m.year,
                                onClick = { onMovieClick?.invoke(m.id) },
                            )
                        }
                    }
                }
            }
        }

        // Similar movies
        if (state.suggestions.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(start = 32.dp, top = 16.dp)) {
                    Text(
                        text = "More Like This",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 32.dp),
                    ) {
                        items(state.suggestions, key = { it.id }) { m ->
                            ContentCard(
                                title = m.title,
                                imageUrl = m.mediumCoverImage,
                                rating = m.rating,
                                year = m.year,
                                onClick = { onMovieClick?.invoke(m.id) },
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MetaDot() {
    Text("\u2022", color = Color(0xFF555555), fontSize = 14.sp)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RatingBadge(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
