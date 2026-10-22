package info.dvkr.screenstream.rtsp.ui.main.settings.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RtspClientSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Auth Settings", style = MaterialTheme.typography.titleLarge)

        var authEnabled by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable authentication", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = authEnabled, onCheckedChange = { authEnabled = it })
        }

        if (authEnabled) {
            var username by remember { mutableStateOf("admin") }
            var password by remember { mutableStateOf("admin") }
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
