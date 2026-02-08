package lol.omnius.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import lol.omnius.android.ui.theme.OmniusCard
import lol.omnius.android.ui.theme.OmniusGold
import lol.omnius.android.ui.theme.OmniusRed

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentCard(
    title: String,
    imageUrl: String,
    rating: Double? = null,
    year: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    aspectRatio: Float = 2f / 3f,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(BorderStroke(2.dp, OmniusRed), shape)
                else Modifier
            ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = OmniusCard,
            focusedContainerColor = OmniusCard,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Bottom gradient for rating badge readability
                if (rating != null && rating > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                )
                            ),
                    )
                }

                // Rating badge — bottom left
                if (rating != null && rating > 0) {
                    Row(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("★", color = OmniusGold, fontSize = 11.sp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", rating),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (year != null && year > 0) {
                    Text(
                        text = year.toString(),
                        color = Color(0xFF888888),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
