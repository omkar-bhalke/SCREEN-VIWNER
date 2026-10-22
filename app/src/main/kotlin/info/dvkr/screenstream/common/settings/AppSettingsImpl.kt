package info.dvkr.screenstream.common.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

class AppSettingsImpl(private val context: Context) : AppSettings {

    companion object {
        private val KEY_NIGHT_MODE = intPreferencesKey("night_mode")
        private val KEY_DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
        private val KEY_LAST_MODULE = stringPreferencesKey("last_module")
        private val KEY_KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        private val KEY_STOP_ON_CONFIG = booleanPreferencesKey("stop_on_config_change")
    }

    override val data: Flow<AppSettings.Data> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings.Data(
            nightMode = prefs[KEY_NIGHT_MODE] ?: -1,
            dynamicTheme = prefs[KEY_DYNAMIC_THEME] ?: false,
            lastSelectedModule = prefs[KEY_LAST_MODULE] ?: "MJPEG",
            keepAwake = prefs[KEY_KEEP_AWAKE] ?: false,
            stopOnConfigurationChange = prefs[KEY_STOP_ON_CONFIG] ?: false
        )
    }

    override suspend fun updateData(transform: (AppSettings.Data) -> AppSettings.Data) {
        var current = AppSettings.Data()
        context.appSettingsDataStore.edit { prefs ->
            current = AppSettings.Data(
                nightMode = prefs[KEY_NIGHT_MODE] ?: -1,
                dynamicTheme = prefs[KEY_DYNAMIC_THEME] ?: false,
                lastSelectedModule = prefs[KEY_LAST_MODULE] ?: "MJPEG",
                keepAwake = prefs[KEY_KEEP_AWAKE] ?: false,
                stopOnConfigurationChange = prefs[KEY_STOP_ON_CONFIG] ?: false
            )
            val updated = transform(current)
            prefs[KEY_NIGHT_MODE] = updated.nightMode
            prefs[KEY_DYNAMIC_THEME] = updated.dynamicTheme
            prefs[KEY_LAST_MODULE] = updated.lastSelectedModule
            prefs[KEY_KEEP_AWAKE] = updated.keepAwake
            prefs[KEY_STOP_ON_CONFIG] = updated.stopOnConfigurationChange
        }
    }
}
