package lol.omnius.android.ui.live

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lol.omnius.android.api.ApiClient
import lol.omnius.android.data.model.ChannelCountry
import lol.omnius.android.ui.theme.OmniusCard
import lol.omnius.android.ui.theme.OmniusRed

class LiveBrowseViewModel : ViewModel() {
    private val _countries = MutableStateFlow<List<ChannelCountry>>(emptyList())
    val countries = _countries.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.getApi().getChannelCountries()
                _countries.value = response.data?.countries ?: emptyList()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveBrowseScreen(
    onCountryClick: (String) -> Unit,
    viewModel: LiveBrowseViewModel = viewModel(),
) {
    val countries by viewModel.countries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Live TV",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading countries...", color = Color.White)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(180.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(countries, key = { it.code }) { country ->
                    var countryFocused by remember { mutableStateOf(false) }
                    val countryShape = RoundedCornerShape(12.dp)
                    Surface(
                        onClick = { onCountryClick(country.code) },
                        modifier = Modifier
                            .onFocusChanged { countryFocused = it.isFocused }
                            .then(
                                if (countryFocused) Modifier.border(BorderStroke(2.dp, OmniusRed), countryShape)
                                else Modifier
                            ),
                        shape = ClickableSurfaceDefaults.shape(shape = countryShape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = OmniusCard,
                            focusedContainerColor = OmniusRed.copy(alpha = 0.3f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = country.flag ?: "",
                                fontSize = 28.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(country.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                country.channelCount?.let {
                                    Text("$it channels", color = Color(0xFF888888), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
