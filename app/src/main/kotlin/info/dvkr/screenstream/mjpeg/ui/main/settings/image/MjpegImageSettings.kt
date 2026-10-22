package info.dvkr.screenstream.mjpeg.ui.main.settings.image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MjpegImageSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Image", style = MaterialTheme.typography.titleLarge)

        // JPEG quality
        var quality by remember { mutableFloatStateOf(80f) }
        Text("JPEG Quality: ${quality.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(value = quality, onValueChange = { quality = it },
            valueRange = 10f..100f, steps = 17)

        // Max FPS
        var fps by remember { mutableFloatStateOf(30f) }
        Text("Max FPS: ${fps.toInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(value = fps, onValueChange = { fps = it },
            valueRange = 1f..60f, steps = 58)

        // Resize
        var resizeEnabled by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resize output", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = resizeEnabled, onCheckedChange = { resizeEnabled = it })
        }
        if (resizeEnabled) {
            var factor by remember { mutableFloatStateOf(50f) }
            Text("Resize factor: ${factor.toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = factor, onValueChange = { factor = it },
                valueRange = 10f..100f, steps = 8)
        }

        // Rotation
        Text("Rotation", style = MaterialTheme.typography.bodyMedium)
        var rotation by remember { mutableStateOf(0) }
        val options = listOf(0, 90, 180, 270)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { deg ->
                FilterChip(
                    selected = rotation == deg,
                    onClick = { rotation = deg },
                    label = { Text("$deg°") }
                )
            }
        }

        // Grayscale
        var grayscale by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grayscale", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = grayscale, onCheckedChange = { grayscale = it })
        }
    }
}
