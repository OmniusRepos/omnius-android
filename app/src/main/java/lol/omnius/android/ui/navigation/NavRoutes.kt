package lol.omnius.android.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val MOVIES = "movies"
    const val MOVIE_DETAIL = "movies/{movieId}"
    const val SERIES = "series"
    const val SERIES_DETAIL = "series/{seriesId}"
    const val LIVE = "live"
    const val LIVE_COUNTRY = "live/country/{countryCode}"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"

    fun movieDetail(movieId: Int) = "movies/$movieId"
    fun seriesDetail(seriesId: Int) = "series/$seriesId"
    fun liveCountry(countryCode: String) = "live/country/$countryCode"
}

enum class SidebarItem(val route: String, val label: String) {
    HOME(NavRoutes.HOME, "Home"),
    SEARCH(NavRoutes.SEARCH, "Search"),
    MOVIES(NavRoutes.MOVIES, "Movies"),
    SERIES(NavRoutes.SERIES, "TV Series"),
    LIVE(NavRoutes.LIVE, "Live TV"),
    FAVORITES(NavRoutes.FAVORITES, "Favorites"),
    SETTINGS(NavRoutes.SETTINGS, "Settings"),
}
