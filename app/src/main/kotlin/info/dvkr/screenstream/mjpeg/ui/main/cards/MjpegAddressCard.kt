package info.dvkr.screenstream.mjpeg.ui.main.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.mjpeg.internal.MjpegNetInterface

@Composable
fun MjpegAddressCard(
    interfaces: List<MjpegNetInterface>,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stream URLs", style = MaterialTheme.typography.titleMedium)
            interfaces.forEach { iface ->
                val url = "${iface.getUrl()}/stream"
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
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
