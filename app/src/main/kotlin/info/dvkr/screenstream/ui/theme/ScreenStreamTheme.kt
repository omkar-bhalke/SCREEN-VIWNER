package info.dvkr.screenstream.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import info.dvkr.screenstream.common.settings.AppSettings

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

@Composable
fun ScreenStreamTheme(
    dynamicColor: Boolean = false,
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val settingsData by appSettings.data.collectAsState(initial = AppSettings.Data())
    val darkTheme = when (settingsData.nightMode) {
        1 -> false  // Light
        2 -> true   // Dark
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && settingsData.dynamicTheme && android.os.Build.VERSION.SDK_INT >= 31 -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
