package lol.omnius.android.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import lol.omnius.android.api.ApiClient
import lol.omnius.android.ui.theme.OmniusGreen
import lol.omnius.android.ui.theme.OmniusRed
import lol.omnius.android.ui.theme.OmniusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var serverUrl by remember { mutableStateOf(ApiClient.getBaseUrl()) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // Server URL
        Text("Server URL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OmniusSurface, RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    saved = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            var saveFocused by remember { mutableStateOf(false) }
            val saveShape = RoundedCornerShape(8.dp)
            Surface(
                onClick = {
                    ApiClient.setBaseUrl(serverUrl)
                    saved = true
                },
                modifier = Modifier
                    .onFocusChanged { saveFocused = it.isFocused }
                    .then(
                        if (saveFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), saveShape)
                        else Modifier
                    ),
                shape = ClickableSurfaceDefaults.shape(shape = saveShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                colors = ClickableSurfaceDefaults.colors(containerColor = OmniusRed),
            ) {
                Text(
                    "Save",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }

            if (saved) {
                Text(
                    "Saved!",
                    color = OmniusGreen,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // App info
        Text("About", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("Omnius for Android TV", color = Color(0xFF888888), fontSize = 13.sp)
        Text("Version 1.0.0", color = Color(0xFF888888), fontSize = 13.sp)
        Text("Package: lol.omnius.android", color = Color(0xFF555555), fontSize = 12.sp)
    }
}
