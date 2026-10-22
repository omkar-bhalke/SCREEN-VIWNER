package info.dvkr.screenstream.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import info.dvkr.screenstream.R
import info.dvkr.screenstream.SingleActivity
import info.dvkr.screenstream.common.module.StreamingModuleManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject

class TileActionService : TileService() {

    private val streamingModulesManager: StreamingModuleManager by inject()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            streamingModulesManager.activeModuleStateFlow.collectLatest { module ->
                if (module == null) {
                    updateTile(Tile.STATE_INACTIVE, "Tap to stream")
                } else {
                    module.isStreaming().collectLatest { streaming ->
                        if (streaming)
                            updateTile(Tile.STATE_ACTIVE, "Streaming")
                        else
                            updateTile(Tile.STATE_INACTIVE, "Tap to stream")
                    }
                }
            }
        }
    }

    override fun onStopListening() {
        scope.coroutineContext.cancelChildren()
        super.onStopListening()
    }

    override fun onClick() {
        val module = streamingModulesManager.getSelectedOrDefault()
        if (module?.isStreaming()?.value == true) {
            module.stopStream("Tile")
        } else {
            // Launch app to get MediaProjection permission
            val intent = Intent(this, SingleActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("TILE_STREAM_REQUEST", true)
            }
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateTile(state: Int, subtitle: String) {
        qsTile?.let { tile ->
            tile.state = state
            tile.subtitle = subtitle
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
            tile.updateTile()
        }
    }
}
