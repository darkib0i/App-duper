package com.darkib.appduper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkib.appduper.ui.theme.ElectricViolet
import com.darkib.appduper.ui.theme.HotMagenta
import com.darkib.appduper.ui.theme.NeonCyan
import kotlin.math.sin

/** Big title with an endlessly flowing neon gradient. */
@Composable
fun ShimmerTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 34.sp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "shift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(ElectricViolet, NeonCyan, HotMagenta, ElectricViolet),
        start = Offset(shift, 0f),
        end = Offset(shift + 600f, 220f),
        tileMode = TileMode.Mirror,
    )
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        ),
    )
}

/** Gradient CTA button with a breathing glow and springy press feedback. */
@Composable
fun GlowButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "glow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ElectricViolet.copy(alpha = glow), Color.Transparent),
                        center = center,
                        radius = size.maxDimension * 0.85f,
                    ),
                    radius = size.maxDimension * 0.85f,
                    center = center,
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(ElectricViolet, HotMagenta)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

/**
 * The App Duper motif: two neon cards that endlessly split apart and merge
 * back together — literally an app being duplicated.
 */
@Composable
fun DupeMotif(modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val transition = rememberInfiniteTransition(label = "motif")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "motifTime",
    )
    val sep = (sin(t) + 1f) / 2f  // 0..1 split factor
    val offset = 6.dp + 22.dp * sep
    val cardSize = size * 0.62f

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // Ghost twin drifting away
        Box(
            Modifier
                .size(cardSize)
                .offset(x = -offset, y = -offset)
                .graphicsLayer {
                    rotationZ = -8f * sep
                    alpha = 0.45f + 0.4f * sep
                }
                .clip(RoundedCornerShape(22.dp))
                .background(NeonCyan.copy(alpha = 0.25f))
                .border(2.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
        )
        // Original card
        Box(
            Modifier
                .size(cardSize)
                .offset(x = offset, y = offset)
                .graphicsLayer { rotationZ = 8f * sep }
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(ElectricViolet, HotMagenta))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(cardSize / 2.4f),
            )
        }
    }
}
