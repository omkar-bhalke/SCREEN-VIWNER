package info.dvkr.screenstream.rtsp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
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
fun RtspMainScreenUI(
    windowSizeClass: StreamingModule.WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val viewModel: RtspViewModel = koinViewModel()
    val state by viewModel.stateFlow.collectAsState()
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Control
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("RTSP Stream", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.isStreaming) "Streaming" else "Stopped",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.isStreaming) {
                            FilledIconButton(
                                onClick = { viewModel.onEvent(RtspUiEvent.StartStopStream) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) { Icon(Icons.Default.Stop, "Stop") }
                        } else {
                            FilledIconButton(
                                onClick = { viewModel.onEvent(RtspUiEvent.StartStopStream) },
                                enabled = state.error == null
                            ) { Icon(Icons.Default.PlayArrow, "Start") }
                        }
                    }
                    if (state.isStreaming) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }

        // Addresses
        if (state.serverNetInterfaces.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RTSP URLs", style = MaterialTheme.typography.titleMedium)
                        state.serverNetInterfaces.forEach { iface ->
                            val url = iface.rtspUrl()
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(iface.name, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(url, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(url)) }) {
                                        Text("Copy")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clients
        if (state.clients.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Clients (${state.clients.size})", style = MaterialTheme.typography.titleMedium)
                        state.clients.forEach { client ->
                            Text(client.address, style = MaterialTheme.typography.bodySmall)
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
                                RtspState.Error.CastPermissionsDenied -> "Cast permission denied"
                                is RtspState.Error.PortInUse -> "Port ${error.port} already in use"
                                is RtspState.Error.Other -> error.throwable.message ?: "Unknown error"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { viewModel.onEvent(RtspUiEvent.RecoverError) }) {
                            Text("Retry") }
                    }
                }
            }
        }
    }
}
