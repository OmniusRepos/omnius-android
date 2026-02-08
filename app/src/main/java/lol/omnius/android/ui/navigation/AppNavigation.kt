package lol.omnius.android.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import lol.omnius.android.ui.home.HomeScreen
import lol.omnius.android.ui.movies.MovieBrowseScreen
import lol.omnius.android.ui.movies.MovieDetailScreen
import lol.omnius.android.ui.series.SeriesBrowseScreen
import lol.omnius.android.ui.series.SeriesDetailScreen
import lol.omnius.android.ui.live.LiveBrowseScreen
import lol.omnius.android.ui.live.LiveCountryScreen
import lol.omnius.android.ui.search.SearchScreen
import lol.omnius.android.ui.favorites.FavoritesScreen
import lol.omnius.android.ui.settings.SettingsScreen
import lol.omnius.android.ui.player.PlayerActivity

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current

    fun launchPlayer(title: String, streamUrl: String, imdbCode: String = "", isTorrent: Boolean = false) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("title", title)
            putExtra("stream_url", streamUrl)
            putExtra("imdb_code", imdbCode)
            putExtra("is_torrent", isTorrent)
        }
        context.startActivity(intent)
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onMovieClick = { navController.navigate(NavRoutes.movieDetail(it)) },
                onSeriesClick = { navController.navigate(NavRoutes.seriesDetail(it)) },
            )
        }

        composable(NavRoutes.MOVIES) {
            MovieBrowseScreen(
                onMovieClick = { navController.navigate(NavRoutes.movieDetail(it)) },
            )
        }

        composable(
            route = NavRoutes.MOVIE_DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onPlay = { title, streamUrl, imdbCode, isTorrent -> launchPlayer(title, streamUrl, imdbCode, isTorrent) },
                onMovieClick = { navController.navigate(NavRoutes.movieDetail(it)) },
            )
        }

        composable(NavRoutes.SERIES) {
            SeriesBrowseScreen(
                onSeriesClick = { navController.navigate(NavRoutes.seriesDetail(it)) },
            )
        }

        composable(
            route = NavRoutes.SERIES_DETAIL,
            arguments = listOf(navArgument("seriesId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: return@composable
            SeriesDetailScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onPlay = { title, streamUrl, imdbCode, isTorrent -> launchPlayer(title, streamUrl, imdbCode, isTorrent) },
            )
        }

        composable(NavRoutes.LIVE) {
            LiveBrowseScreen(
                onCountryClick = { navController.navigate(NavRoutes.liveCountry(it)) },
            )
        }

        composable(
            route = NavRoutes.LIVE_COUNTRY,
            arguments = listOf(navArgument("countryCode") { type = NavType.StringType }),
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("countryCode") ?: return@composable
            LiveCountryScreen(
                countryCode = code,
                onBack = { navController.popBackStack() },
                onChannelPlay = { name, url -> launchPlayer(name, url) },
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onMovieClick = { navController.navigate(NavRoutes.movieDetail(it)) },
                onSeriesClick = { navController.navigate(NavRoutes.seriesDetail(it)) },
            )
        }

        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                onMovieClick = { navController.navigate(NavRoutes.movieDetail(it)) },
                onSeriesClick = { navController.navigate(NavRoutes.seriesDetail(it)) },
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
