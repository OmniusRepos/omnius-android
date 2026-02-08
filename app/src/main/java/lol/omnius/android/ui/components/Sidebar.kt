package lol.omnius.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import lol.omnius.android.ui.navigation.SidebarItem
import lol.omnius.android.ui.theme.OmniusDark
import lol.omnius.android.ui.theme.OmniusRed
import lol.omnius.android.ui.theme.OmniusTextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Sidebar(
    currentRoute: String?,
    expanded: Boolean,
    onItemClick: (SidebarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = if (expanded) 200.dp else 56.dp

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(OmniusDark)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo
        if (expanded) {
            Text(
                text = "OMNIUS",
                color = OmniusRed,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp, start = 16.dp),
            )
        } else {
            Text(
                text = "O",
                color = OmniusRed,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }

        SidebarItem.entries.forEach { item ->
            // Exact match for top-level, prefix match for detail pages
            // e.g. "movies/{movieId}" starts with "movies", "live/country/{code}" starts with "live"
            val isActive = currentRoute == item.route ||
                (currentRoute != null && currentRoute.startsWith(item.route + "/"))

            SidebarNavItem(
                item = item,
                isActive = isActive,
                expanded = expanded,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    isActive: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isActive -> OmniusRed.copy(alpha = 0.15f)
        isFocused -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val textColor = when {
        isActive -> OmniusRed
        isFocused -> Color.White
        else -> OmniusTextSecondary
    }

    val navShape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), navShape)
                else Modifier
            ),
        shape = ClickableSurfaceDefaults.shape(shape = navShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (item) {
                SidebarItem.HOME -> "\u2302"
                SidebarItem.SEARCH -> "\u2315"
                SidebarItem.MOVIES -> "\u25B6"
                SidebarItem.SERIES -> "\u25A3"
                SidebarItem.LIVE -> "\u25C9"
                SidebarItem.FAVORITES -> "\u2665"
                SidebarItem.SETTINGS -> "\u2699"
            }
            Text(
                text = icon,
                color = textColor,
                fontSize = 18.sp,
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
