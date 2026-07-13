package com.darkib.appduper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.darkib.appduper.ui.theme.ElectricViolet
import com.darkib.appduper.ui.theme.HotMagenta
import com.darkib.appduper.ui.theme.MintGlow
import com.darkib.appduper.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val wobble: Float,
)

/**
 * One-shot neon confetti burst from the center of the screen. Fires every
 * time [trigger] changes to a non-zero value.
 */
@Composable
fun ConfettiOverlay(trigger: Long, modifier: Modifier = Modifier) {
    if (trigger == 0L) return

    val particles = remember(trigger) {
        val rng = Random(trigger)
        val palette = listOf(ElectricViolet, NeonCyan, HotMagenta, MintGlow, Color.White)
        List(90) {
            Particle(
                angle = rng.nextFloat() * 6.28318f,
                speed = 0.35f + rng.nextFloat() * 0.9f,
                size = 5f + rng.nextFloat() * 11f,
                color = palette[rng.nextInt(palette.size)],
                spin = (rng.nextFloat() - 0.5f) * 20f,
                wobble = rng.nextFloat() * 6.28f,
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, animationSpec = tween(1400, easing = LinearEasing))
    }

    if (progress.value >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        val cx = size.width / 2f
        val cy = size.height * 0.4f
        val reach = size.minDimension

        particles.forEach { particle ->
            val dist = particle.speed * reach * p
            val gravity = 350f * p * p
            val x = cx + cos(particle.angle) * dist + sin(p * 12f + particle.wobble) * 14f
            val y = cy + sin(particle.angle) * dist + gravity
            val alpha = (1f - p).coerceIn(0f, 1f)
            rotate(degrees = particle.spin * p * 360f / 20f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - particle.size / 2f, y - particle.size / 2f),
                    size = Size(particle.size, particle.size * 0.6f),
                )
            }
        }
    }
}
