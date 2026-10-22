package info.dvkr.screenstream.common.module

import android.content.Context
import info.dvkr.screenstream.common.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StreamingModuleManager(
    private val modules: List<StreamingModule>,
    private val appSettings: AppSettings
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _activeModuleState = MutableStateFlow<StreamingModule?>(null)
    val activeModuleStateFlow: StateFlow<StreamingModule?> = _activeModuleState.asStateFlow()

    private val _selectedModuleId = MutableStateFlow<StreamingModule.Id?>(null)
    val selectedModuleIdFlow: StateFlow<StreamingModule.Id?> = _selectedModuleId.asStateFlow()

    fun hasModule(id: StreamingModule.Id): Boolean = modules.any { it.getId() == id }

    fun isActive(id: StreamingModule.Id): Boolean = _activeModuleState.value?.getId() == id

    fun getSelectedOrDefault(): StreamingModule? =
        _selectedModuleId.value?.let { id -> modules.find { it.getId() == id } }
            ?: modules.minByOrNull { it.getPriority() }

    fun selectStreamingModule(id: StreamingModule.Id) {
        scope.launch {
            _selectedModuleId.value = id
            appSettings.updateData { data -> data.copy(lastSelectedModule = id.name) }
        }
    }

    fun startModule(id: StreamingModule.Id, context: Context) {
        val module = modules.find { it.getId() == id } ?: return
        scope.launch {
            _activeModuleState.value?.let { active ->
                if (active.getId() != id) active.stopModule("Switching module")
            }
            module.startModule(context)
            _activeModuleState.value = module
        }
    }

    suspend fun stopModule(id: StreamingModule.Id, reason: String? = null) {
        modules.find { it.getId() == id }?.stopModule(reason)
        if (_activeModuleState.value?.getId() == id) {
            _activeModuleState.value = null
        }
    }
}
