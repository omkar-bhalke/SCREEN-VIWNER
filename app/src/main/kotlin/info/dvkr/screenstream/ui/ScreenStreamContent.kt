package info.dvkr.screenstream.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenStreamContent(
    streamingModulesManager: StreamingModuleManager,
    onModuleStart: (StreamingModule.Id) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedId by streamingModulesManager.selectedModuleIdFlow.collectAsState()
    val selected = selectedId ?: StreamingModule.Id.MJPEG

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenStream") },
                actions = {
                    IconButton(onClick = { /* TODO: open app settings screen */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                StreamingModule.Id.entries.forEach { id ->
                    NavigationBarItem(
                        selected = selected == id,
                        onClick = { streamingModulesManager.selectStreamingModule(id) },
                        icon = { Icon(Icons.Default.Cast, contentDescription = id.name) },
                        label = { Text(id.name) }
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            streamingModulesManager.getSelectedOrDefault()?.StreamUIContent(
                windowSizeClass = StreamingModule.WindowWidthSizeClass.COMPACT,
                modifier = Modifier.fillMaxSize(),
                onBackPressed = {}
            )
        }
    }
}
