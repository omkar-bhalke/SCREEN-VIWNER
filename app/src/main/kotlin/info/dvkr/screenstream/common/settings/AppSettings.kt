package info.dvkr.screenstream.common.settings

import kotlinx.coroutines.flow.Flow

interface AppSettings {

    data class Data(
        val nightMode: Int = -1,           // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        val dynamicTheme: Boolean = false,
        val lastSelectedModule: String = "MJPEG",
        val keepAwake: Boolean = false,
        val stopOnConfigurationChange: Boolean = false
    )

    val data: Flow<Data>
    suspend fun updateData(transform: (Data) -> Data)
}
