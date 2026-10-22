package info.dvkr.screenstream.rtsp.ui.main.settings.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun RtspServerSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Server Settings", style = MaterialTheme.typography.titleLarge)

        var port by remember { mutableStateOf("8554") }
        OutlinedTextField(
            value = port,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) port = it },
            label = { Text("RTSP Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        var streamPath by remember { mutableStateOf("/screenstream") }
        OutlinedTextField(
            value = streamPath,
            onValueChange = { streamPath = it },
            label = { Text("Stream path") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Video codec", style = MaterialTheme.typography.bodyMedium)
        var codec by remember { mutableStateOf("H264") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("H264", "H265").forEach { c ->
                FilterChip(selected = codec == c, onClick = { codec = c }, label = { Text(c) })
            }
        }

        Text("Resolution", style = MaterialTheme.typography.bodyMedium)
        var res by remember { mutableStateOf("1280x720") }
        listOf("1920x1080", "1280x720", "854x480").forEach { r ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = res == r, onClick = { res = r })
                Text(r, style = MaterialTheme.typography.bodyMedium)
            }
        }

        var fps by remember { mutableFloatStateOf(30f) }
        Text("FPS: ${fps.toInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = fps, onValueChange = { fps = it }, valueRange = 1f..60f, steps = 58)
    }
}
