package com.darkib.appduper.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkib.appduper.ui.SpaceState
import com.darkib.appduper.ui.components.AnimatedBackground
import com.darkib.appduper.ui.components.DupeMotif
import com.darkib.appduper.ui.components.GlowButton
import com.darkib.appduper.ui.components.ShimmerTitle
import com.darkib.appduper.ui.theme.CardSurface
import com.darkib.appduper.ui.theme.Dimmed
import com.darkib.appduper.ui.theme.NeonCyan
import com.darkib.appduper.ui.theme.Starlight
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    spaceState: SpaceState,
    onCreateSpace: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            DupeMotif(size = 190.dp)
            Spacer(Modifier.height(30.dp))
            ShimmerTitle("App Duper", fontSize = 40.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Run two accounts of the same app.\nOne tap. Zero root.",
                color = Starlight,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                lineHeight = 25.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "App Duper builds a private, isolated Dupe Space on your phone " +
                    "and drops perfect copies of your apps inside it — each with " +
                    "its own data, its own login, its own life.",
                color = Dimmed,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(38.dp))

            AnimatedContent(
                targetState = spaceState,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.85f))
                },
                label = "onboardingCta",
            ) { st ->
                when (st) {
                    SpaceState.PROVISIONING -> ProvisioningIndicator()
                    SpaceState.UNSUPPORTED -> UnsupportedCard()
                    else -> GlowButton(
                        text = "Create Dupe Space",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onCreateSpace,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvisioningIndicator() {
    val dots by produceState(initialValue = "") {
        var n = 0
        while (true) {
            value = ".".repeat(n % 4)
            n++
            delay(400)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            "Building your Dupe Space$dots",
            color = NeonCyan,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "This can take a minute — Android is creating the isolated profile.",
            color = Dimmed,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UnsupportedCard() {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface.copy(alpha = 0.8f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                RoundedCornerShape(20.dp),
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "This device doesn't allow creating a work profile, so apps can't " +
                "be duplicated here. (Some devices managed by a school or " +
                "employer block this.)",
            color = Starlight,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}
