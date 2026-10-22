package info.dvkr.screenstream.webrtc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.common.module.StreamingModule
import org.koin.androidx.compose.koinViewModel

@Composable
fun WebRtcMainScreenUI(
    windowSizeClass: StreamingModule.WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val viewModel: WebRtcViewModel = koinViewModel()
    val state by viewModel.stateFlow.collectAsState()
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Control card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("WebRTC Stream", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = when {
                                    state.isStreaming && state.signalingConnected -> "Live"
                                    state.isStreaming -> "Connecting..."
                                    else -> "Stopped"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isStreaming && state.signalingConnected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.isStreaming) {
                            FilledIconButton(
                                onClick = { viewModel.onEvent(WebRtcUiEvent.StartStopStream) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) { Icon(Icons.Default.Stop, "Stop") }
                        } else {
                            FilledIconButton(
                                onClick = { viewModel.onEvent(WebRtcUiEvent.StartStopStream) },
                                enabled = state.error == null
                            ) { Icon(Icons.Default.PlayArrow, "Start") }
                        }
                    }
                    if (state.isStreaming) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }

        // Viewer URL card
        if (state.viewerUrl.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Viewer URL", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.viewerUrl, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(state.viewerUrl))
                        }) { Icon(Icons.Default.ContentCopy, "Copy") }
                    }
                }
            }
        }

        // Clients
        if (state.clients.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Viewers (${state.clients.size})", style = MaterialTheme.typography.titleMedium)
                        state.clients.forEach { client ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(client.id.take(12) + "…", style = MaterialTheme.typography.bodySmall)
                                Badge(containerColor = if (client.connected)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                { Text(if (client.connected) "Connected" else "Connecting") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Error
        state.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = when (error) {
                                WebRtcError.SignalingFailed -> "Signaling server unreachable"
                                WebRtcError.CastPermissionsDenied -> "Cast permission denied"
                                is WebRtcError.Other -> error.throwable.message ?: "Unknown error"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { viewModel.onEvent(WebRtcUiEvent.RecoverError) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
