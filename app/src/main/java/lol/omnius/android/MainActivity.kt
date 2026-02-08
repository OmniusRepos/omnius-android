package lol.omnius.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lol.omnius.android.ui.components.Sidebar
import lol.omnius.android.ui.navigation.AppNavGraph
import lol.omnius.android.ui.navigation.SidebarItem
import lol.omnius.android.ui.theme.OmniusDark
import lol.omnius.android.ui.theme.OmniusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            OmniusTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var sidebarExpanded by remember { mutableStateOf(false) }
    val sidebarFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniusDark),
    ) {
        // Sidebar — expands on focus
        Box(
            modifier = Modifier
                .focusRequester(sidebarFocusRequester)
                .onFocusChanged { focusState ->
                    sidebarExpanded = focusState.hasFocus || focusState.isFocused
                },
        ) {
            Sidebar(
                currentRoute = currentRoute,
                expanded = sidebarExpanded,
                onItemClick = { item ->
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to home so back works: detail -> browse -> home
                            popUpTo(SidebarItem.HOME.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            AppNavGraph(navController = navController)
        }
    }
}
