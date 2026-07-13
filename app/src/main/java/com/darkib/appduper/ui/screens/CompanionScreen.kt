package com.darkib.appduper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkib.appduper.ui.components.AnimatedBackground
import com.darkib.appduper.ui.components.DupeMotif
import com.darkib.appduper.ui.components.ShimmerTitle
import com.darkib.appduper.ui.theme.CardSurface
import com.darkib.appduper.ui.theme.Dimmed
import com.darkib.appduper.ui.theme.NeonCyan
import com.darkib.appduper.ui.theme.Starlight

/** Shown when App Duper is opened from inside the Dupe Space itself. */
@Composable
fun CompanionScreen() {
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DupeMotif(size = 150.dp)
            Spacer(Modifier.height(26.dp))
            ShimmerTitle("Dupe Space", fontSize = 32.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "You're inside the Dupe Space 🌌",
                color = Starlight,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSurface.copy(alpha = 0.8f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
            ) {
                Text(
                    "This companion keeps your duped apps alive. Your duplicated " +
                        "apps appear in the work tab of your launcher with a " +
                        "briefcase badge — each one runs with completely separate " +
                        "data, so you can sign in with a different account.",
                    color = Dimmed,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "To manage dupes, open App Duper from your normal app drawer.",
                color = Dimmed,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
