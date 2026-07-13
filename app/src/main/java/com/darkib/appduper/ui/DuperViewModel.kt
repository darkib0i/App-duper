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

/** Which top-level screen should be visible. */
enum class SpaceState {
    CHECKING,      // still figuring things out
    INSIDE_SPACE,  // this process runs inside the Dupe Space (companion screen)
    UNSUPPORTED,   // device can't create managed profiles
    NEEDS_SETUP,   // supported, but the Dupe Space doesn't exist yet
    PROVISIONING,  // user kicked off setup, waiting for the profile to appear
    READY,         // Dupe Space exists, main UI
}

data class DuperUiState(
    val spaceState: SpaceState = SpaceState.CHECKING,
    val loadingApps: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val duped: Set<String> = emptySet(),
    val inFlight: Set<String> = emptySet(),
    val query: String = "",
    val celebration: Long = 0L,   // bump to fire the confetti overlay
    val error: String? = null,
)

class DuperViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DuperUiState())
    val state: StateFlow<DuperUiState> = _state.asStateFlow()

    private val context get() = getApplication<Application>()

    /** Re-evaluate everything; called on every onResume. */
    fun refresh() {
        viewModelScope.launch {
            val spaceState = withContext(Dispatchers.IO) { detectSpaceState() }
            _state.update { it.copy(spaceState = spaceState) }
            if (spaceState == SpaceState.READY) {
                loadApps()
            }
        }
    }

    private fun detectSpaceState(): SpaceState = when {
        Profiles.isInsideDupeSpace(context) -> SpaceState.INSIDE_SPACE
        Profiles.dupeSpace(context) != null -> SpaceState.READY
        !Profiles.isManagedProfileSupported(context) ||
            !Profiles.isProvisioningAllowed(context) -> SpaceState.UNSUPPORTED
        _state.value.spaceState == SpaceState.PROVISIONING -> SpaceState.PROVISIONING
        else -> SpaceState.NEEDS_SETUP
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

    fun dismissError() = _state.update { it.copy(error = null) }

    /** Called after the user finishes (or cancels) the system provisioning flow. */
    fun onProvisioningLaunched() {
        _state.update { it.copy(spaceState = SpaceState.PROVISIONING) }
        viewModelScope.launch {
            // The profile can take a little while to appear and finalize.
            repeat(120) {
                val space = withContext(Dispatchers.IO) { Profiles.dupeSpace(context) }
                if (space != null) {
                    _state.update { it.copy(spaceState = SpaceState.READY) }
                    loadApps()
                    return@launch
                }
                delay(1000)
            }
            _state.update { it.copy(spaceState = SpaceState.NEEDS_SETUP) }
        }
    }

    fun provisioningCancelled() {
        _state.update { it.copy(spaceState = SpaceState.NEEDS_SETUP) }
    }

    fun dupe(packageName: String) {
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
                        error = "Couldn't reach the Dupe Space. Try re-opening the app."
                    )
                }
                return@launch
            }
            // Wait for the dupe to land in the profile.
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
                    error = "Duping timed out — this app may not be clonable on your device."
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
                // The user confirms removal in a system dialog; poll for a while.
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
                _state.update { it.copy(error = "Couldn't open the duped app.") }
            }
        }
    }
}
