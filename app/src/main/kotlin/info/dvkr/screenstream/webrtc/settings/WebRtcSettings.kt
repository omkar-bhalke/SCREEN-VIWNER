package info.dvkr.screenstream.webrtc.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.webRtcDataStore by preferencesDataStore("webrtc_settings")

class WebRtcSettings private constructor(private val context: Context) {

    data class Data(
        val videoWidth: Int = 1280,
        val videoHeight: Int = 720,
        val fps: Int = 30,
        val videoBitrateKbps: Int = 3000,
        val enableMic: Boolean = false,
        val audioBitrateKbps: Int = 128,
        val keepAwake: Boolean = false
    )

    companion object {
        fun create(context: Context): WebRtcSettings = WebRtcSettings(context)
        private val KEY_WIDTH   = intPreferencesKey("video_width")
        private val KEY_HEIGHT  = intPreferencesKey("video_height")
        private val KEY_FPS     = intPreferencesKey("fps")
        private val KEY_VBITRATE= intPreferencesKey("video_bitrate_kbps")
        private val KEY_MIC     = booleanPreferencesKey("enable_mic")
        private val KEY_ABITRATE= intPreferencesKey("audio_bitrate_kbps")
        private val KEY_AWAKE   = booleanPreferencesKey("keep_awake")
    }

    @Volatile private var _cache: Data? = null
    fun cachedData() = _cache ?: Data()

    val data: Flow<Data> = context.webRtcDataStore.data.map { prefs ->
        Data(
            videoWidth        = prefs[KEY_WIDTH]    ?: 1280,
            videoHeight       = prefs[KEY_HEIGHT]   ?: 720,
            fps               = prefs[KEY_FPS]      ?: 30,
            videoBitrateKbps  = prefs[KEY_VBITRATE] ?: 3000,
            enableMic         = prefs[KEY_MIC]      ?: false,
            audioBitrateKbps  = prefs[KEY_ABITRATE] ?: 128,
            keepAwake         = prefs[KEY_AWAKE]    ?: false
        ).also { _cache = it }
    }
}
