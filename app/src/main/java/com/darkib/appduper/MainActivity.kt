package com.darkib.appduper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkib.appduper.ui.theme.CardSurface
import com.darkib.appduper.ui.theme.Dimmed
import com.darkib.appduper.ui.theme.Starlight
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

    var crashReport by remember { mutableStateOf(AppDuperApp.consumeLastCrash(context)) }

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
                    onDismissMessage = viewModel::dismissMessage,
                )
            }
        }

        crashReport?.let { report ->
            CrashCard(report = report, onDismiss = { crashReport = null })
        }
    }
}

/** Shows the last captured crash trace with a copy button, so it can be shared. */
@Composable
private fun CrashCard(report: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .safeDrawingPadding()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            Text("It crashed last time", color = Starlight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "Copy this and send it over so the exact cause can be fixed.",
                color = Dimmed,
                fontSize = 12.5.sp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                report.take(2000),
                color = Starlight,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                PillButton("Copy") { clipboard.setText(AnnotatedString(report)) }
                Spacer(Modifier.width(10.dp))
                PillButton("Dismiss", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    )
}
