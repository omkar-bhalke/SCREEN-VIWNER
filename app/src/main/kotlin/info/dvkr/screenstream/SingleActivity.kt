package info.dvkr.screenstream

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.ui.ScreenStreamContent
import info.dvkr.screenstream.ui.theme.ScreenStreamTheme
import org.koin.android.ext.android.inject

class SingleActivity : AppUpdateActivity() {

    private val streamingModulesManager: StreamingModuleManager by inject()
    private val appSettings: AppSettings by inject()

    override fun getAppSettings(): AppSettings = appSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenStreamTheme(appSettings = appSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenStreamContent(
                        streamingModulesManager = streamingModulesManager,
                        onModuleStart = { moduleId ->
                            streamingModulesManager.startModule(moduleId, this)
                        }
                    )
                }
            }
        }
    }
}
