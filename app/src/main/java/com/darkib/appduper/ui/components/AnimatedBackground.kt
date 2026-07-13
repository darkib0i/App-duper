package com.darkib.appduper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.darkib.appduper.ui.theme.SpaceBlack
import com.darkib.appduper.ui.theme.SpaceDeep
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Orb(
    val tone: Float,       // 0 = grey, 1 = white
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float,
    val drift: Float,
)

private data class Star(val x: Float, val y: Float, val size: Float, val twinklePhase: Float)

/**
 * Living monochrome background: soft grey/white light pools drifting over deep
 * black, a slow diagonal shimmer band, and a field of twinkling stars.
 */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val orbs = remember {
        listOf(
            Orb(0.9f, 0.15f, 0.18f, 0.55f, 0.0f, 1.0f, 0.10f),
            Orb(0.4f, 0.90f, 0.15f, 0.45f, 2.1f, 0.7f, 0.12f),
            Orb(0.6f, 0.82f, 0.85f, 0.50f, 4.2f, 0.9f, 0.09f),
            Orb(0.3f, 0.10f, 0.92f, 0.40f, 1.3f, 0.6f, 0.14f),
        )
    }
    val stars = remember {
        val rng = Random(42)
        List(70) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                size = 1f + rng.nextFloat() * 2.2f,
                twinklePhase = rng.nextFloat() * 6.28f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "bgTime",
    )
    val sweep by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "bgSweep",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(SpaceBlack, SpaceDeep, SpaceBlack)))

        val minDim = size.minDimension
        orbs.forEach { orb ->
            val cx = orb.baseX * size.width + cos(t * orb.speed + orb.phase) * orb.drift * minDim
            val cy = orb.baseY * size.height + sin(t * orb.speed + orb.phase) * orb.drift * minDim
            val r = orb.radius * minDim
            val core = lerpWhite(orb.tone)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(core.copy(alpha = 0.12f + 0.06f * orb.tone), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }

        // Diagonal shimmer band sliding across the screen.
        val bandX = sweep * size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                start = Offset(bandX - size.width * 0.25f, 0f),
                end = Offset(bandX + size.width * 0.25f, size.height),
            )
        )

        stars.forEach { star ->
            val twinkle = 0.35f + 0.65f * ((sin(t * 3f + star.twinklePhase) + 1f) / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.30f * twinkle),
                radius = star.size,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

private fun lerpWhite(tone: Float): Color {
    val v = (0.55f + 0.45f * tone).coerceIn(0f, 1f)
    return Color(v, v, v, 1f)
}
