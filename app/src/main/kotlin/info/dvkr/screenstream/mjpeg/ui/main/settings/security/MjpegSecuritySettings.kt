package info.dvkr.screenstream.mjpeg.ui.main.settings.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun MjpegSecuritySettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Security", style = MaterialTheme.typography.titleLarge)

        var pinEnabled by remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable PIN protection", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = pinEnabled, onCheckedChange = { pinEnabled = it })
        }

        if (pinEnabled) {
            var pin by remember { mutableStateOf("0000") }
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                label = { Text("PIN (4 digits)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            var hidePinOnStart by remember { mutableStateOf(false) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hide PIN on stream start", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = hidePinOnStart, onCheckedChange = { hidePinOnStart = it })
            }
            var newPinOnStart by remember { mutableStateOf(false) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New PIN on each start", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = newPinOnStart, onCheckedChange = { newPinOnStart = it })
            }
        }

        HorizontalDivider()
        Text("Blocked addresses", style = MaterialTheme.typography.titleMedium)
        Text(
            "No addresses blocked",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
