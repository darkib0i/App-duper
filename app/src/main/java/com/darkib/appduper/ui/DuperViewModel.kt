package com.darkib.appduper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.darkib.appduper.core.AppEntry
import com.darkib.appduper.core.AppRepository
import com.darkib.appduper.core.Bridge
import com.darkib.appduper.core.Profiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Top-level screen. Everything that isn't the companion is the single Home. */
enum class Stage { CHECKING, INSIDE_SPACE, HOME }

/** How this device can actually make a second copy of an app. */
enum class DupeMethod {
    OWN_SPACE,     // App Duper owns a Dupe Space and clones into it directly
    CAN_SETUP,     // No space yet, but we're allowed to create one
    SYSTEM_CLONE,  // A work profile already exists -> hand off to the OS cloner
}

data class DuperUiState(
    val stage: Stage = Stage.CHECKING,
    val method: DupeMethod = DupeMethod.CAN_SETUP,
    val loadingApps: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val duped: Set<String> = emptySet(),
    val inFlight: Set<String> = emptySet(),
    val query: String = "",
    val provisioning: Boolean = false,
    val celebration: Long = 0L,   // bump to fire the burst overlay
    val message: String? = null,  // transient info / error banner
)

class DuperViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DuperUiState())
    val state: StateFlow<DuperUiState> = _state.asStateFlow()

    private val context get() = getApplication<Application>()

    /** Re-evaluate everything; called on every onResume. */
    fun refresh() {
        viewModelScope.launch {
            val (stage, method) = withContext(Dispatchers.IO) { detect() }
            _state.update { it.copy(stage = stage, method = method) }
            if (stage == Stage.HOME) loadApps()
        }
    }

    private fun detect(): Pair<Stage, DupeMethod> = when {
        Profiles.isInsideDupeSpace(context) -> Stage.INSIDE_SPACE to DupeMethod.OWN_SPACE
        Profiles.dupeSpace(context) != null -> Stage.HOME to DupeMethod.OWN_SPACE
        Profiles.canCreateOwnSpace(context) -> Stage.HOME to DupeMethod.CAN_SETUP
        else -> Stage.HOME to DupeMethod.SYSTEM_CLONE
    }

    private fun loadApps() {
        viewModelScope.launch {
            val (apps, duped) = withContext(Dispatchers.IO) {
                val loaded = if (_state.value.apps.isEmpty()) {
                    AppRepository.loadLaunchableApps(context)
                } else {
                    _state.value.apps
                }
                loaded to Profiles.dupedPackages(context)
            }
            _state.update { it.copy(loadingApps = false, apps = apps, duped = duped) }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    // ---- Dupe Space provisioning -------------------------------------------

    fun onProvisioningLaunched() {
        _state.update { it.copy(provisioning = true) }
        viewModelScope.launch {
            repeat(120) {
                val space = withContext(Dispatchers.IO) { Profiles.dupeSpace(context) }
                if (space != null) {
                    _state.update {
                        it.copy(provisioning = false, method = DupeMethod.OWN_SPACE)
                    }
                    loadApps()
                    return@launch
                }
                delay(1000)
            }
            _state.update { it.copy(provisioning = false) }
        }
    }

    fun provisioningCancelled() = _state.update { it.copy(provisioning = false) }

    // ---- Duping ------------------------------------------------------------

    /** Requests that MainActivity launch the system provisioning flow. */
    val provisionRequests = MutableStateFlow(0)

    fun dupe(packageName: String) {
        when (_state.value.method) {
            DupeMethod.OWN_SPACE -> cloneIntoSpace(packageName)
            DupeMethod.CAN_SETUP -> provisionRequests.update { it + 1 }
            DupeMethod.SYSTEM_CLONE -> {
                val ok = Profiles.openSystemClone(context, packageName)
                _state.update {
                    it.copy(
                        message = if (ok) {
                            "Opened your device's app-cloning screen — turn on the copy there."
                        } else {
                            "Your device doesn't expose an app-cloning option."
                        }
                    )
                }
            }
        }
    }

    private fun cloneIntoSpace(packageName: String) {
        if (packageName in _state.value.inFlight) return
        _state.update { it.copy(inFlight = it.inFlight + packageName) }
        viewModelScope.launch {
            val sent = withContext(Dispatchers.IO) {
                Profiles.sendToDupeSpace(context, Bridge.CMD_CLONE, packageName)
            }
            if (!sent) {
                _state.update {
                    it.copy(
                        inFlight = it.inFlight - packageName,
                        message = "Couldn't reach the Dupe Space. Try re-opening the app.",
                    )
                }
                return@launch
            }
            repeat(40) {
                delay(500)
                val duped = withContext(Dispatchers.IO) { Profiles.dupedPackages(context) }
                if (packageName in duped) {
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
            _state.update {
                it.copy(
                    inFlight = it.inFlight - packageName,
                    message = "Duping timed out — this app may not be clonable here.",
                )
            }
        }
    }

    fun removeDupe(packageName: String) {
        if (packageName in _state.value.inFlight) return
        _state.update { it.copy(inFlight = it.inFlight + packageName) }
        viewModelScope.launch {
            val sent = withContext(Dispatchers.IO) {
                Profiles.sendToDupeSpace(context, Bridge.CMD_REMOVE, packageName)
            }
            if (sent) {
                repeat(60) {
                    delay(1000)
                    val duped = withContext(Dispatchers.IO) { Profiles.dupedPackages(context) }
                    if (packageName !in duped) {
                        _state.update {
                            it.copy(duped = duped, inFlight = it.inFlight - packageName)
                        }
                        return@launch
                    }
                }
            }
            _state.update { it.copy(inFlight = it.inFlight - packageName) }
        }
    }

    fun openDupe(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!Profiles.launchDupe(context, packageName)) {
                _state.update { it.copy(message = "Couldn't open the duped app.") }
            }
        }
    }
}
