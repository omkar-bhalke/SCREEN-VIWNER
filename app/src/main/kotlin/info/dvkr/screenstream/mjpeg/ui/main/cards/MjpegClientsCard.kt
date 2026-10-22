package info.dvkr.screenstream.mjpeg.ui.main.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.mjpeg.ui.MjpegState
import info.dvkr.screenstream.mjpeg.ui.MjpegUiEvent

@Composable
fun MjpegClientsCard(
    clients: List<MjpegState.Client>,
    onEvent: (MjpegUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Connected Clients (${clients.size})", style = MaterialTheme.typography.titleMedium)
            clients.forEach { client ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(client.remoteAddress, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${client.transferBytes / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEvent(MjpegUiEvent.BlockClient(client.id)) }) {
                        Icon(Icons.Default.Block, contentDescription = "Block")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
