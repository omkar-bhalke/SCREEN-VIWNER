package info.dvkr.screenstream.mjpeg.ui.main.settings.advanced

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun MjpegAdvancedSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Advanced", style = MaterialTheme.typography.titleLarge)

        // Server port
        var port by remember { mutableStateOf("8080") }
        OutlinedTextField(
            value = port,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) port = it },
            label = { Text("Server port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // IPv4 / IPv6
        var enableIPv4 by remember { mutableStateOf(true) }
        var enableIPv6 by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable IPv4", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enableIPv4, onCheckedChange = { enableIPv4 = it })
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable IPv6", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enableIPv6, onCheckedChange = { enableIPv6 = it })
        }

        HorizontalDivider()

        // Crop
        var cropEnabled by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable crop", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = cropEnabled, onCheckedChange = { cropEnabled = it })
        }

        // Flip
        var flipH by remember { mutableStateOf(false) }
        var flipV by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Flip horizontal", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = flipH, onCheckedChange = { flipH = it })
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Flip vertical", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = flipV, onCheckedChange = { flipV = it })
        }
    }
}
