package info.dvkr.screenstream.mjpeg.settings

import android.content.Context
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.filterNotNull
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mjpegDataStore by preferencesDataStore("mjpeg_settings")

class MjpegSettings private constructor(private val context: Context) {

    data class Data(
        val serverPort: Int = 8080,
        val enableIPv4: Boolean = true,
        val enableIPv6: Boolean = false,
        val ipv4Address: Int = 0,
        val ipv6Address: Int = 0,
        val enablePin: Boolean = false,
        val hidePinOnStart: Boolean = false,
        val newPinOnAppStart: Boolean = false,
        val autoChangePin: Boolean = false,
        val pin: String = "0000",
        val blockAddressList: List<String> = emptyList(),
        val jpegQuality: Int = 80,
        val resizeEnabled: Boolean = false,
        val resizeFactor: Int = 50,
        val rotation: Int = 0,
        val maxFPS: Int = 30,
        val enableCrop: Boolean = false,
        val cropTop: Int = 0, val cropBottom: Int = 0,
        val cropLeft: Int = 0, val cropRight: Int = 0,
        val grayscale: Boolean = false,
        val flipVertical: Boolean = false,
        val flipHorizontal: Boolean = false,
        val keepAwake: Boolean = false,
        val stopOnSleep: Boolean = false,
        val htmlEnableButtons: Boolean = true,
        val htmlBackColor: Long = 0xFF000000,
        val enableSSL: Boolean = false,
        val sslCertsData: String = "",
        val vrMode: Int = 0
    )

    companion object {
        fun create(context: Context): MjpegSettings = MjpegSettings(context)

        private val KEY_PORT = intPreferencesKey("server_port")
        private val KEY_JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        private val KEY_MAX_FPS = intPreferencesKey("max_fps")
        private val KEY_ENABLE_PIN = booleanPreferencesKey("enable_pin")
        private val KEY_PIN = stringPreferencesKey("pin")
        private val KEY_ENABLE_IPV4 = booleanPreferencesKey("enable_ipv4")
        private val KEY_ENABLE_IPV6 = booleanPreferencesKey("enable_ipv6")
        private val KEY_RESIZE_ENABLED = booleanPreferencesKey("resize_enabled")
        private val KEY_RESIZE_FACTOR = intPreferencesKey("resize_factor")
        private val KEY_ROTATION = intPreferencesKey("rotation")
        private val KEY_GRAYSCALE = booleanPreferencesKey("grayscale")
        private val KEY_KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        private val KEY_STOP_ON_SLEEP = booleanPreferencesKey("stop_on_sleep")
        private val KEY_ENABLE_CROP = booleanPreferencesKey("enable_crop")
        private val KEY_CROP_TOP = intPreferencesKey("crop_top")
        private val KEY_CROP_BOTTOM = intPreferencesKey("crop_bottom")
        private val KEY_CROP_LEFT = intPreferencesKey("crop_left")
        private val KEY_CROP_RIGHT = intPreferencesKey("crop_right")
        private val KEY_FLIP_V = booleanPreferencesKey("flip_vertical")
        private val KEY_FLIP_H = booleanPreferencesKey("flip_horizontal")
        private val KEY_BLOCK_LIST = stringPreferencesKey("block_address_list")
        private val KEY_HTML_BUTTONS = booleanPreferencesKey("html_enable_buttons")
        private val KEY_HTML_BACK_COLOR = longPreferencesKey("html_back_color")
        private val KEY_ENABLE_SSL = booleanPreferencesKey("enable_ssl")
        private val KEY_VR_MODE = intPreferencesKey("vr_mode")
    }

    val data: Flow<Data?> = context.mjpegDataStore.data.map { prefs ->
        Data(
            serverPort     = prefs[KEY_PORT] ?: 8080,
            jpegQuality    = prefs[KEY_JPEG_QUALITY] ?: 80,
            maxFPS         = prefs[KEY_MAX_FPS] ?: 30,
            enablePin      = prefs[KEY_ENABLE_PIN] ?: false,
            pin            = prefs[KEY_PIN] ?: "0000",
            enableIPv4     = prefs[KEY_ENABLE_IPV4] ?: true,
            enableIPv6     = prefs[KEY_ENABLE_IPV6] ?: false,
            resizeEnabled  = prefs[KEY_RESIZE_ENABLED] ?: false,
            resizeFactor   = prefs[KEY_RESIZE_FACTOR] ?: 50,
            rotation       = prefs[KEY_ROTATION] ?: 0,
            grayscale      = prefs[KEY_GRAYSCALE] ?: false,
            keepAwake      = prefs[KEY_KEEP_AWAKE] ?: false,
            stopOnSleep    = prefs[KEY_STOP_ON_SLEEP] ?: false,
            enableCrop     = prefs[KEY_ENABLE_CROP] ?: false,
            cropTop        = prefs[KEY_CROP_TOP] ?: 0,
            cropBottom     = prefs[KEY_CROP_BOTTOM] ?: 0,
            cropLeft       = prefs[KEY_CROP_LEFT] ?: 0,
            cropRight      = prefs[KEY_CROP_RIGHT] ?: 0,
            flipVertical   = prefs[KEY_FLIP_V] ?: false,
            flipHorizontal = prefs[KEY_FLIP_H] ?: false,
            blockAddressList = prefs[KEY_BLOCK_LIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            htmlEnableButtons = prefs[KEY_HTML_BUTTONS] ?: true,
            htmlBackColor  = prefs[KEY_HTML_BACK_COLOR] ?: 0xFF000000,
            enableSSL      = prefs[KEY_ENABLE_SSL] ?: false,
            vrMode         = prefs[KEY_VR_MODE] ?: 0
        )
    }

    // Returns latest value synchronously — requires collect() to have fired

    // Cache updated by dataFlow collector in each service
    @Volatile private var _cachedData: Data? = null
    fun cachedData(): Data = _cachedData ?: Data()

    // Non-nullable flow — update cache on each emission
    val dataFlow: Flow<Data> = data.filterNotNull().onEach { _cachedData = it }
}