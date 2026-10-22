package info.dvkr.screenstream.mjpeg.ui.main.settings.general

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import org.koin.androidx.compose.koinViewModel
import info.dvkr.screenstream.mjpeg.ui.MjpegViewModel

@Composable
fun MjpegGeneralSettings(modifier: Modifier = Modifier) {
    val viewModel: MjpegViewModel = koinViewModel()
    // Settings changes sent via MjpegUiEvent
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("General", style = MaterialTheme.typography.titleLarge)
        MjpegSettingItem("Keep screen awake while streaming") { /* toggle */ }
        MjpegSettingItem("Stop stream when screen turns off") { /* toggle */ }
        MjpegSettingItem("Start streaming on app launch") { /* toggle */ }
    }
}

@Composable
private fun MjpegSettingItem(label: String, onToggle: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { checked = it; onToggle(it) })
    }
}
