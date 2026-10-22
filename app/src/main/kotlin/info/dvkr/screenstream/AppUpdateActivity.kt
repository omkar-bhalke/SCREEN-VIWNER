package info.dvkr.screenstream

import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import info.dvkr.screenstream.common.settings.AppSettings

abstract class AppUpdateActivity : ComponentActivity() {

    abstract fun getAppSettings(): AppSettings

    override fun onResume() {
        super.onResume()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        runCatching {
            AppUpdateManagerFactory.create(this)
                .appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        AppUpdateManagerFactory.create(this).startUpdateFlowForResult(
                            info, AppUpdateType.IMMEDIATE, this, 1001
                        )
                    }
                }
        }
    }
}
