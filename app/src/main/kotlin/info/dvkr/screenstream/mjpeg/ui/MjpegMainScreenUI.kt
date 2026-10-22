package info.dvkr.screenstream.mjpeg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.mjpeg.ui.main.cards.MjpegAddressCard
import info.dvkr.screenstream.mjpeg.ui.main.cards.MjpegClientsCard
import info.dvkr.screenstream.mjpeg.ui.main.cards.MjpegStreamCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun MjpegMainScreenUI(
    windowSizeClass: StreamingModule.WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val viewModel: MjpegViewModel = koinViewModel()
    val state by viewModel.stateFlow.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { MjpegStreamCard(state = state, onEvent = viewModel::onEvent) }

        if (state.serverNetInterfaces.isNotEmpty()) {
            item { MjpegAddressCard(interfaces = state.serverNetInterfaces) }
        }

        if (state.clients.isNotEmpty()) {
            item { MjpegClientsCard(clients = state.clients, onEvent = viewModel::onEvent) }
        }

        state.error?.let { error ->
            item {
                MjpegErrorCard(error = error, onEvent = viewModel::onEvent)
            }
        }
    }
}

@Composable
internal fun MjpegErrorCard(
    error: MjpegError,
    onEvent: (MjpegUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (error) {
                    is MjpegError.CastPermissionsDenied -> "Cast permission denied"
                    is MjpegError.AddressInUse         -> "Port ${error.port} already in use"
                    is MjpegError.Other                -> error.throwable.message ?: "Unknown error"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = { onEvent(MjpegUiEvent.RecoverError) }) {
                Text("Retry")
            }
        }
    }
}
