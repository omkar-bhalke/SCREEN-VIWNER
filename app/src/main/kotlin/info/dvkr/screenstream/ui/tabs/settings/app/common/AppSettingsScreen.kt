package info.dvkr.screenstream.ui.tabs.settings.app.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.common.settings.AppSettings
import org.koin.compose.koinInject

@Composable
fun AppSettingsScreen(modifier: Modifier = Modifier) {
    val appSettings: AppSettings = koinInject()
    val settingsData by appSettings.data.collectAsState(initial = AppSettings.Data())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("App Settings", style = MaterialTheme.typography.titleLarge)

        // Night mode
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        val modes = listOf("System default" to -1, "Light" to 1, "Dark" to 2)
        modes.forEach { (label, mode) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                RadioButton(
                    selected = settingsData.nightMode == mode,
                    onClick = { /* update via appSettings */ }
                )
            }
        }

        HorizontalDivider()

        // Dynamic color
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dynamic color", style = MaterialTheme.typography.bodyMedium)
                    Text("Use wallpaper colors (Android 12+)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settingsData.dynamicTheme,
                    onCheckedChange = { /* update */ }
                )
            }
        }

        HorizontalDivider()

        // Keep awake
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Keep screen awake", style = MaterialTheme.typography.bodyMedium)
                Text("Prevent screen from sleeping while app is open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settingsData.keepAwake,
                onCheckedChange = { /* update */ }
            )
        }
    }
}
