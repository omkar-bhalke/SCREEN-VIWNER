package info.dvkr.screenstream.common.ui.mediaprojection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Composable helper: triggers MediaProjection permission flow.
 * Usage:
 *   val launcher = rememberMediaProjectionLauncher { projection -> /* start stream */ }
 *   Button(onClick = { launcher.launch() }) { ... }
 */
@Composable
fun rememberMediaProjectionLauncher(
    onGranted: (Intent) -> Unit,
    onDenied: () -> Unit = {}
): MediaProjectionLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            onGranted(result.data!!)
        } else {
            onDenied()
        }
    }
    return MediaProjectionLauncher {
        val pm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(pm.createScreenCaptureIntent())
    }
}

fun interface MediaProjectionLauncher {
    fun launch()
}
