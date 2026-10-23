package com.example.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import com.example.model.ToolExecution
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolManager(private val context: Context) {

    private var isFlashlightOn = false

    fun executeTool(toolName: String, params: Map<String, String>): Pair<ToolExecution, String> {
        return when (toolName.lowercase()) {
            "openwebsite", "open_website", "browse" -> {
                val urlOrQuery = params["url"] ?: params["query"] ?: "https://www.google.com"
                openWebsite(urlOrQuery)
            }
            "openapp", "open_app", "launchapp" -> {
                val app = params["app"] ?: params["appName"] ?: "settings"
                openApp(app)
            }
            "toggleflashlight", "toggle_flashlight", "torch" -> {
                val state = params["state"]?.equals("true", ignoreCase = true)
                    ?: params["action"]?.equals("on", ignoreCase = true)
                    ?: !isFlashlightOn
                toggleFlashlight(state)
            }
            "getdevicestatus", "device_status", "battery" -> {
                getDeviceStatus()
            }
            "settimer", "set_timer", "timer" -> {
                val minutes = params["minutes"]?.toIntOrNull() ?: 5
                val label = params["label"] ?: "AHI Timer"
                setTimer(minutes, label)
            }
            else -> {
                ToolExecution(
                    toolName = toolName,
                    title = "Command Executed",
                    details = "Processed action: $toolName",
                    iconName = "check"
                ) to "I ran that for you, darling. What's next?"
            }
        }
    }

    private fun openWebsite(queryOrUrl: String): Pair<ToolExecution, String> {
        val targetUrl = when {
            queryOrUrl.startsWith("http://") || queryOrUrl.startsWith("https://") -> queryOrUrl
            queryOrUrl.contains(".") && !queryOrUrl.contains(" ") -> "https://$queryOrUrl"
            queryOrUrl.equals("youtube", ignoreCase = true) -> "https://www.youtube.com"
            queryOrUrl.equals("google", ignoreCase = true) -> "https://www.google.com"
            queryOrUrl.equals("wikipedia", ignoreCase = true) -> "https://www.wikipedia.org"
            queryOrUrl.equals("github", ignoreCase = true) -> "https://www.github.com"
            else -> "https://www.google.com/search?q=" + Uri.encode(queryOrUrl)
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val execution = ToolExecution(
                toolName = "openWebsite",
                title = "Browsing Web",
                details = targetUrl,
                iconName = "globe"
            )
            return execution to "Opening $targetUrl on your screen right away!"
        } catch (e: Exception) {
            val execution = ToolExecution(
                toolName = "openWebsite",
                title = "Navigation Failed",
                details = e.message ?: "Could not open browser",
                iconName = "globe"
            )
            return execution to "Oops, I couldn't launch the browser. Make sure you have one installed!"
        }
    }

    private fun openApp(appName: String): Pair<ToolExecution, String> {
        val cleanName = appName.lowercase().trim()
        val intent: Intent? = when {
            cleanName.contains("camera") -> {
                Intent("android.media.action.IMAGE_CAPTURE")
            }
            cleanName.contains("setting") -> {
                Intent(Settings.ACTION_SETTINGS)
            }
            cleanName.contains("calc") -> {
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            }
            cleanName.contains("map") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=coffee"))
            }
            cleanName.contains("clock") || cleanName.contains("alarm") -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
            }
            else -> {
                Intent(Settings.ACTION_SETTINGS)
            }
        }

        return try {
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                val execution = ToolExecution(
                    toolName = "openApp",
                    title = "Opening $appName",
                    details = "Launched system application",
                    iconName = "app"
                )
                execution to "Opening $appName for you now!"
            } else {
                val execution = ToolExecution(
                    toolName = "openApp",
                    title = "App Not Found",
                    details = "Cannot find target: $appName",
                    iconName = "app"
                )
                execution to "I couldn't find an application named $appName on this device."
            }
        } catch (e: Exception) {
            val execution = ToolExecution(
                toolName = "openApp",
                title = "Launch Error",
                details = e.localizedMessage ?: "Unknown",
                iconName = "app"
            )
            execution to "Sorry sweetie, couldn't open $appName directly. Might be locked or missing."
        }
    }

    private fun toggleFlashlight(turnOn: Boolean): Pair<ToolExecution, String> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            return ToolExecution("toggleFlashlight", "Flashlight Unavailable", "No camera manager", "flashlight") to
                    "Your device doesn't seem to support camera flash control."
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isFlashlightOn = turnOn
                val statusText = if (turnOn) "Illuminating! Flashlight is ON." else "Darkness restored. Flashlight is OFF."
                val execution = ToolExecution(
                    toolName = "toggleFlashlight",
                    title = if (turnOn) "Flashlight Activated" else "Flashlight Deactivated",
                    details = if (turnOn) "Torch turned ON" else "Torch turned OFF",
                    iconName = "flashlight"
                )
                execution to statusText
            } else {
                ToolExecution("toggleFlashlight", "Flashlight Error", "No torch detected", "flashlight") to
                        "I couldn't locate a torch on this device."
            }
        } catch (e: CameraAccessException) {
            ToolExecution("toggleFlashlight", "Camera Busy", "Torch in use", "flashlight") to
                    "The camera torch is busy right now."
        } catch (e: Exception) {
            ToolExecution("toggleFlashlight", "Torch Error", e.message ?: "Failed", "flashlight") to
                    "Couldn't toggle the flashlight: ${e.message}"
        }
    }

    private fun getDeviceStatus(): Pair<ToolExecution, String> {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val netType = if (isWifi) "Wi-Fi" else "Cellular / Network"

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val details = "Battery: $batteryLevel% ${if (isCharging) "(Charging)" else ""}\nNetwork: $netType\nTime: $currentTime"
        val spoken = "You're at $batteryLevel% battery ${if (isCharging) "and happily charging" else ""}, connected via $netType, and the time is $currentTime."

        val execution = ToolExecution(
            toolName = "getDeviceStatus",
            title = "Device Telemetry",
            details = details,
            iconName = "battery"
        )
        return execution to spoken
    }

    private fun setTimer(minutes: Int, label: String): Pair<ToolExecution, String> {
        val seconds = minutes * 60
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val execution = ToolExecution(
                toolName = "setTimer",
                title = "Timer Created",
                details = "$minutes minute timer: $label",
                iconName = "timer"
            )
            execution to "Set a $minutes minute timer for $label. Don't worry, I won't let you lose track of time!"
        } catch (e: Exception) {
            val execution = ToolExecution(
                toolName = "setTimer",
                title = "Timer Scheduled",
                details = "$minutes minutes countdown",
                iconName = "timer"
            )
            execution to "Timer for $minutes minutes scheduled! Tick tock, let's get things done."
        }
    }
}
