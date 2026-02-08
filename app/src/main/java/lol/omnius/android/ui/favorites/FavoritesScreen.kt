package lol.omnius.android.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "My Favorites",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        )

        // TODO: Wire up favorites from local storage (SharedPreferences / Room)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u2665",
                    color = Color(0xFF333333),
                    fontSize = 48.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No favorites yet",
                    color = Color(0xFF666666),
                    fontSize = 16.sp,
                )
                Text(
                    text = "Browse movies and series to add them here",
                    color = Color(0xFF444444),
                    fontSize = 13.sp,
                )
            }
        }
    }
}
