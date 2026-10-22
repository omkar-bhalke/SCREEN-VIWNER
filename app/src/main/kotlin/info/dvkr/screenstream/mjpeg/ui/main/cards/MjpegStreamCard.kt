package info.dvkr.screenstream.mjpeg.ui.main.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.mjpeg.ui.MjpegState
import info.dvkr.screenstream.mjpeg.ui.MjpegUiEvent

@Composable
fun MjpegStreamCard(
    state: MjpegState,
    onEvent: (MjpegUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("MJPEG Stream", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when {
                            state.waitingCastPermission -> "Waiting for permission..."
                            state.isStreaming -> "Streaming"
                            else -> "Stopped"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.isStreaming) {
                    FilledIconButton(
                        onClick = { onEvent(MjpegUiEvent.StartStopStream) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                } else {
                    FilledIconButton(
                        onClick = { onEvent(MjpegUiEvent.StartStopStream) },
                        enabled = state.error == null
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    }
                }
            }

            if (state.isStreaming) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // PIN row
            if (state.requiresPin) {
                when (val pin = state.pin) {
                    is MjpegState.Pin.Required -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PIN: ${pin.pin}", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onEvent(MjpegUiEvent.NewPin) }) { Text("New PIN") }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
