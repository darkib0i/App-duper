package com.darkib.appduper.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkib.appduper.core.AppEntry
import com.darkib.appduper.core.AppRepository
import com.darkib.appduper.core.ApkCloner
import com.darkib.appduper.core.CloneInstaller
import com.darkib.appduper.core.Clones
import com.darkib.appduper.core.Profiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Top-level screen. */
enum class Stage { CHECKING, INSIDE_SPACE, HOME }

data class DuperUiState(
    val stage: Stage = Stage.CHECKING,
    val loadingApps: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val duped: Set<String> = emptySet(),
    val inFlight: Set<String> = emptySet(),
    val query: String = "",
    val celebration: Long = 0L,
    val message: String? = null,
)

class DuperViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DuperUiState())
    val state: StateFlow<DuperUiState> = _state.asStateFlow()

    private val context get() = getApplication<Application>()

    fun refresh() {
        viewModelScope.launch {
            val stage = withContext(Dispatchers.IO) {
                runCatching {
                    if (Profiles.isInsideDupeSpace(context)) Stage.INSIDE_SPACE else Stage.HOME
                }.getOrDefault(Stage.HOME)
            }
            _state.update { it.copy(stage = stage) }
            if (stage == Stage.HOME) loadApps()
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val (apps, duped) = withContext(Dispatchers.IO) {
                    val loaded = if (_state.value.apps.isEmpty()) {
                        AppRepository.loadLaunchableApps(context)
                    } else {
                        _state.value.apps
                    }
                    loaded to runCatching { Clones.basePackagesWithClones(context) }.getOrDefault(emptySet())
                }
                _state.update { it.copy(loadingApps = false, apps = apps, duped = duped) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(loadingApps = false, message = "Couldn't load your apps: ${t.message}")
                }
            }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Clone [packageName] into a second, independent app and install it. */
    fun dupe(packageName: String) {
        if (packageName in _state.value.inFlight) return
        _state.update { it.copy(inFlight = it.inFlight + packageName) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ApkCloner.buildClone(context, packageName) }
            if (!result.success) {
                _state.update {
                    it.copy(inFlight = it.inFlight - packageName, message = "Couldn't clone: ${result.error}")
                }
                return@launch
            }
            val installed = runCatching {
                withContext(Dispatchers.IO) { CloneInstaller.install(context, result.apks) }
            }
            if (installed.isFailure) {
                _state.update {
                    it.copy(
                        inFlight = it.inFlight - packageName,
                        message = "Install failed: ${installed.exceptionOrNull()?.message}",
                    )
                }
                return@launch
            }
            // The user confirms in the system installer dialog; wait for the clone to land.
            repeat(180) {
                delay(1000)
                val duped = withContext(Dispatchers.IO) { Clones.basePackagesWithClones(context) }
                if (packageName in duped) {
                    result.apks.forEach { runCatching { it.delete() } }
                    _state.update {
                        it.copy(
                            duped = duped,
                            inFlight = it.inFlight - packageName,
                            celebration = System.currentTimeMillis(),
                        )
                    }
                    return@launch
                }
            }
            // Timed out or cancelled.
            result.apks.forEach { runCatching { it.delete() } }
            _state.update { it.copy(inFlight = it.inFlight - packageName) }
        }
    }

    fun removeDupe(basePackage: String) {
        if (basePackage in _state.value.inFlight) return
        val clone = Clones.firstClone(context, basePackage) ?: return
        _state.update { it.copy(inFlight = it.inFlight + basePackage) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { CloneInstaller.uninstall(context, clone) } }
            repeat(120) {
                delay(1000)
                val duped = withContext(Dispatchers.IO) { Clones.basePackagesWithClones(context) }
                if (basePackage !in duped) {
                    _state.update { it.copy(duped = duped, inFlight = it.inFlight - basePackage) }
                    return@launch
                }
            }
            _state.update { it.copy(inFlight = it.inFlight - basePackage) }
        }
    }

    fun openDupe(basePackage: String) {
        viewModelScope.launch {
            val intent = withContext(Dispatchers.IO) {
                Clones.firstClone(context, basePackage)?.let { Clones.launchIntentFor(context, it) }
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onFailure { _state.update { s -> s.copy(message = "Couldn't open the dupe.") } }
            } else {
                _state.update { it.copy(message = "Couldn't open the dupe.") }
            }
        }
    }
}
