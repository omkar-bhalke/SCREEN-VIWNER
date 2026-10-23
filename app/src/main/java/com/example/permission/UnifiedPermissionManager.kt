package com.example.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class UnifiedReadinessState {
    ALL_READY,
    NEEDS_MICROPHONE,
    NEEDS_INTERNET,
    NEEDS_BOTH
}

class UnifiedPermissionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val networkMonitor = NetworkMonitor(context)

    private val _isMicGranted = MutableStateFlow(checkMicPermission())
    val isMicGranted: StateFlow<Boolean> = _isMicGranted

    val isInternetConnected: StateFlow<Boolean> = networkMonitor.isConnected

    val readinessState: StateFlow<UnifiedReadinessState> = combine(
        _isMicGranted,
        isInternetConnected
    ) { hasMic, hasInternet ->
        when {
            hasMic && hasInternet -> UnifiedReadinessState.ALL_READY
            !hasMic && !hasInternet -> UnifiedReadinessState.NEEDS_BOTH
            !hasMic -> UnifiedReadinessState.NEEDS_MICROPHONE
            else -> UnifiedReadinessState.NEEDS_INTERNET
        }
    }.stateIn(scope, SharingStarted.Eagerly, checkCurrentReadiness())

    fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onMicPermissionResult(granted: Boolean) {
        _isMicGranted.value = granted
    }

    fun refreshState() {
        _isMicGranted.value = checkMicPermission()
        networkMonitor.refresh()
    }

    fun checkCurrentReadiness(): UnifiedReadinessState {
        val hasMic = checkMicPermission()
        val hasInternet = networkMonitor.checkCurrentConnectivity()
        return when {
            hasMic && hasInternet -> UnifiedReadinessState.ALL_READY
            !hasMic && !hasInternet -> UnifiedReadinessState.NEEDS_BOTH
            !hasMic -> UnifiedReadinessState.NEEDS_MICROPHONE
            else -> UnifiedReadinessState.NEEDS_INTERNET
        }
    }

    fun release() {
        networkMonitor.unregister()
    }
}
