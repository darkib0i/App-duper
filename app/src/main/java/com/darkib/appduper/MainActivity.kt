package com.darkib.appduper

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkib.appduper.core.Profiles
import com.darkib.appduper.ui.DuperViewModel
import com.darkib.appduper.ui.Stage
import com.darkib.appduper.ui.components.DupeMotif
import com.darkib.appduper.ui.screens.CompanionScreen
import com.darkib.appduper.ui.screens.HomeScreen
import com.darkib.appduper.ui.theme.AppDuperTheme
import com.darkib.appduper.ui.theme.SpaceBlack

class MainActivity : ComponentActivity() {

    private val viewModel: DuperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDuperTheme {
                Root(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

@Composable
private fun Root(viewModel: DuperViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val provisioningLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onProvisioningLaunched()
        } else {
            viewModel.provisioningCancelled()
        }
    }

    fun createSpace() {
        try {
            provisioningLauncher.launch(Profiles.provisioningIntent(context))
        } catch (e: Exception) {
            viewModel.provisioningCancelled()
        }
    }

    // Duping an app when no space exists yet triggers provisioning.
    val requests by viewModel.provisionRequests.collectAsStateWithLifecycle()
    LaunchedEffect(requests) {
        if (requests > 0) createSpace()
    }

    Box(Modifier.fillMaxSize().background(SpaceBlack)) {
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut()
            },
            label = "rootScreen",
        ) { stage ->
            when (stage) {
                Stage.CHECKING -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    DupeMotif(size = 130.dp)
                }

                Stage.INSIDE_SPACE -> CompanionScreen()

                Stage.HOME -> HomeScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onDupe = viewModel::dupe,
                    onOpen = viewModel::openDupe,
                    onRemove = viewModel::removeDupe,
                    onCreateSpace = ::createSpace,
                    onDismissMessage = viewModel::dismissMessage,
                )
            }
        }
    }
}
