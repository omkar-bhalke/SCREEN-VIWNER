package info.dvkr.screenstream.rtsp.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rtspDataStore by preferencesDataStore("rtsp_settings")

class RtspSettings private constructor(private val context: Context) {

    enum class AudioSource { NONE, MIC, DEVICE }
    enum class VideoCodec { H264, H265 }
    enum class AudioCodec { AAC, OPUS }
    enum class ServerMode { LOCAL, RELAY }

    data class Data(
        // Server
        val serverPort: Int = 8554,
        val serverMode: ServerMode = ServerMode.LOCAL,
        val relayAddress: String = "",
        val relayPort: Int = 8554,
        val relayUsername: String = "",
        val relayPassword: String = "",
        val enableIPv4: Boolean = true,
        val enableIPv6: Boolean = false,
        // Video
        val videoCodec: VideoCodec = VideoCodec.H264,
        val videoWidth: Int = 1280,
        val videoHeight: Int = 720,
        val videoFps: Int = 30,
        val videoBitrateKbps: Int = 3000,
        val videoRotation: Int = 0,
        // Audio
        val audioSource: AudioSource = AudioSource.NONE,
        val audioCodec: AudioCodec = AudioCodec.AAC,
        val audioBitrateKbps: Int = 128,
        val audioSampleRate: Int = 44100,
        val audioChannels: Int = 1,
        // Auth
        val enableAuth: Boolean = false,
        val username: String = "admin",
        val password: String = "admin",
        // Misc
        val enableOnvif: Boolean = false,
        val keepAwake: Boolean = false,
        val streamPath: String = "/screenstream"
    )

    companion object {
        fun create(context: Context): RtspSettings = RtspSettings(context)
        private val KEY_PORT        = intPreferencesKey("server_port")
        private val KEY_SERVER_MODE = stringPreferencesKey("server_mode")
        private val KEY_RELAY_ADDR  = stringPreferencesKey("relay_address")
        private val KEY_RELAY_PORT  = intPreferencesKey("relay_port")
        private val KEY_RELAY_USER  = stringPreferencesKey("relay_username")
        private val KEY_RELAY_PASS  = stringPreferencesKey("relay_password")
        private val KEY_IPV4        = booleanPreferencesKey("enable_ipv4")
        private val KEY_IPV6        = booleanPreferencesKey("enable_ipv6")
        private val KEY_VCODEC      = stringPreferencesKey("video_codec")
        private val KEY_VWIDTH      = intPreferencesKey("video_width")
        private val KEY_VHEIGHT     = intPreferencesKey("video_height")
        private val KEY_VFPS        = intPreferencesKey("video_fps")
        private val KEY_VBITRATE    = intPreferencesKey("video_bitrate_kbps")
        private val KEY_VROTATION   = intPreferencesKey("video_rotation")
        private val KEY_ASRC        = stringPreferencesKey("audio_source")
        private val KEY_ACODEC      = stringPreferencesKey("audio_codec")
        private val KEY_ABITRATE    = intPreferencesKey("audio_bitrate_kbps")
        private val KEY_ASAMPLE     = intPreferencesKey("audio_sample_rate")
        private val KEY_ACHAN       = intPreferencesKey("audio_channels")
        private val KEY_AUTH        = booleanPreferencesKey("enable_auth")
        private val KEY_AUTH_USER   = stringPreferencesKey("auth_username")
        private val KEY_AUTH_PASS   = stringPreferencesKey("auth_password")
        private val KEY_ONVIF       = booleanPreferencesKey("enable_onvif")
        private val KEY_AWAKE       = booleanPreferencesKey("keep_awake")
        private val KEY_PATH        = stringPreferencesKey("stream_path")
    }

    @Volatile private var _cache: Data? = null
    fun cachedData() = _cache ?: Data()

    val data: Flow<Data> = context.rtspDataStore.data.map { prefs ->
        Data(
            serverPort       = prefs[KEY_PORT]       ?: 8554,
            serverMode       = ServerMode.valueOf(prefs[KEY_SERVER_MODE] ?: "LOCAL"),
            relayAddress     = prefs[KEY_RELAY_ADDR] ?: "",
            relayPort        = prefs[KEY_RELAY_PORT] ?: 8554,
            relayUsername    = prefs[KEY_RELAY_USER] ?: "",
            relayPassword    = prefs[KEY_RELAY_PASS] ?: "",
            enableIPv4       = prefs[KEY_IPV4]       ?: true,
            enableIPv6       = prefs[KEY_IPV6]       ?: false,
            videoCodec       = VideoCodec.valueOf(prefs[KEY_VCODEC]  ?: "H264"),
            videoWidth       = prefs[KEY_VWIDTH]     ?: 1280,
            videoHeight      = prefs[KEY_VHEIGHT]    ?: 720,
            videoFps         = prefs[KEY_VFPS]       ?: 30,
            videoBitrateKbps = prefs[KEY_VBITRATE]   ?: 3000,
            videoRotation    = prefs[KEY_VROTATION]  ?: 0,
            audioSource      = AudioSource.valueOf(prefs[KEY_ASRC]   ?: "NONE"),
            audioCodec       = AudioCodec.valueOf(prefs[KEY_ACODEC]  ?: "AAC"),
            audioBitrateKbps = prefs[KEY_ABITRATE]   ?: 128,
            audioSampleRate  = prefs[KEY_ASAMPLE]    ?: 44100,
            audioChannels    = prefs[KEY_ACHAN]      ?: 1,
            enableAuth       = prefs[KEY_AUTH]       ?: false,
            username         = prefs[KEY_AUTH_USER]  ?: "admin",
            password         = prefs[KEY_AUTH_PASS]  ?: "admin",
            enableOnvif      = prefs[KEY_ONVIF]      ?: false,
            keepAwake        = prefs[KEY_AWAKE]      ?: false,
            streamPath       = prefs[KEY_PATH]       ?: "/screenstream"
        ).also { _cache = it }
    }
}
