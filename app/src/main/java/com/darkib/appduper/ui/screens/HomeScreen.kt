package com.darkib.appduper.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkib.appduper.core.AppEntry
import com.darkib.appduper.ui.DuperUiState
import com.darkib.appduper.ui.components.AnimatedBackground
import com.darkib.appduper.ui.components.ConfettiOverlay
import com.darkib.appduper.ui.components.DupeMotif
import com.darkib.appduper.ui.components.ShimmerTitle
import com.darkib.appduper.ui.theme.CardSurface
import com.darkib.appduper.ui.theme.Dimmed
import com.darkib.appduper.ui.theme.ElectricViolet
import com.darkib.appduper.ui.theme.HotMagenta
import com.darkib.appduper.ui.theme.MintGlow
import com.darkib.appduper.ui.theme.NeonCyan
import com.darkib.appduper.ui.theme.Starlight
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun HomeScreen(
    state: DuperUiState,
    onQueryChange: (String) -> Unit,
    onDupe: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Header(appCount = state.apps.size, dupeCount = state.duped.size)
            Spacer(Modifier.height(16.dp))
            SearchBar(query = state.query, onQueryChange = onQueryChange)
            Spacer(Modifier.height(12.dp))

            if (state.loadingApps) {
                LoadingApps()
            } else {
                val filtered = remember(state.apps, state.query) {
                    val q = state.query.trim().lowercase()
                    if (q.isEmpty()) state.apps
                    else state.apps.filter {
                        it.label.lowercase().contains(q) ||
                            it.packageName.lowercase().contains(q)
                    }
                }
                AppList(
                    apps = filtered,
                    duped = state.duped,
                    inFlight = state.inFlight,
                    onDupe = onDupe,
                    onOpen = onOpen,
                    onRemove = onRemove,
                )
            }
        }

        ConfettiOverlay(trigger = state.celebration)
        ErrorBanner(
            error = state.error,
            onDismiss = onDismissError,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Header(appCount: Int, dupeCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            ShimmerTitle("App Duper")
            Spacer(Modifier.height(6.dp))
            Row {
                StatChip(count = appCount, label = "apps", color = NeonCyan)
                Spacer(Modifier.width(8.dp))
                StatChip(count = dupeCount, label = "duped", color = HotMagenta)
            }
        }
        DupeMotif(size = 78.dp)
    }
}

@Composable
private fun StatChip(count: Int, label: String, color: Color) {
    val animated by animateIntAsState(
        targetValue = count,
        animationSpec = tween(900),
        label = "statCount",
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$animated",
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(5.dp))
        Text(label, color = Dimmed, fontSize = 13.sp)
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateFloatAsState(
        targetValue = if (query.isNotEmpty()) 1f else 0.35f,
        label = "searchBorder",
    )
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        ElectricViolet.copy(alpha = borderColor),
                        NeonCyan.copy(alpha = borderColor),
                    )
                ),
                RoundedCornerShape(50),
            ),
        placeholder = { Text("Search your apps…", color = Dimmed) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Dimmed) },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Dimmed)
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CardSurface.copy(alpha = 0.7f),
            unfocusedContainerColor = CardSurface.copy(alpha = 0.55f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = NeonCyan,
            focusedTextColor = Starlight,
            unfocusedTextColor = Starlight,
        ),
    )
}

@Composable
private fun LoadingApps() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DupeMotif(size = 120.dp)
            Spacer(Modifier.height(18.dp))
            Text("Scanning your apps…", color = Dimmed, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AppList(
    apps: List<AppEntry>,
    duped: Set<String>,
    inFlight: Set<String>,
    onDupe: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No apps match that search 👻", color = Dimmed, fontSize = 15.sp)
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                entry = app,
                index = index,
                isDuped = app.packageName in duped,
                isBusy = app.packageName in inFlight,
                onDupe = { onDupe(app.packageName) },
                onOpen = { onOpen(app.packageName) },
                onRemove = { onRemove(app.packageName) },
            )
        }
    }
}

@Composable
private fun AppCard(
    entry: AppEntry,
    index: Int,
    isDuped: Boolean,
    isBusy: Boolean,
    onDupe: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    // Staggered entrance
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(min(index, 12) * 45L)
        appeared = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(350),
        label = "cardAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (appeared) 0.dp else 26.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardOffset",
    )

    val borderBrush = if (isDuped) {
        Brush.horizontalGradient(listOf(NeonCyan, ElectricViolet, HotMagenta))
    } else {
        Brush.horizontalGradient(
            listOf(Starlight.copy(alpha = 0.08f), Starlight.copy(alpha = 0.08f))
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = offsetY)
            .alpha(alpha)
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface.copy(alpha = 0.72f))
            .border(1.5.dp, borderBrush, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                bitmap = entry.icon,
                contentDescription = entry.label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            if (isBusy) {
                SpinningRing(size = 60.dp)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.label,
                color = Starlight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            if (isDuped) {
                DupedBadge()
            } else {
                Text(
                    entry.packageName,
                    color = Dimmed,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))

        AnimatedContent(
            targetState = when {
                isBusy -> CardAction.BUSY
                isDuped -> CardAction.DUPED
                else -> CardAction.IDLE
            },
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.7f))
            },
            label = "cardAction",
        ) { action ->
            when (action) {
                CardAction.IDLE -> DupeButton(onClick = onDupe)
                CardAction.BUSY -> CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = NeonCyan,
                    strokeWidth = 3.dp,
                )
                CardAction.DUPED -> Row {
                    RoundIconButton(
                        icon = Icons.Rounded.PlayArrow,
                        tint = MintGlow,
                        contentDescription = "Open dupe",
                        onClick = onOpen,
                    )
                    Spacer(Modifier.width(6.dp))
                    RoundIconButton(
                        icon = Icons.Rounded.Delete,
                        tint = HotMagenta,
                        contentDescription = "Remove dupe",
                        onClick = onRemove,
                    )
                }
            }
        }
    }
}

private enum class CardAction { IDLE, BUSY, DUPED }

@Composable
private fun SpinningRing(size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "ring")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "ringAngle",
    )
    Canvas(Modifier.size(size)) {
        rotate(angle, center) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, NeonCyan, ElectricViolet)
                ),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun DupedBadge() {
    val transition = rememberInfiniteTransition(label = "badge")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "badgePulse",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(MintGlow),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "DUPED",
            color = MintGlow.copy(alpha = 0.6f + 0.4f * pulse),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun DupeButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dupePress",
    )
    Row(
        Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(ElectricViolet, HotMagenta)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Dupe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "roundPress",
    )
    Box(
        Modifier
            .scale(scale)
            .size(38.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.5f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ErrorBanner(error: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(error) {
        if (error != null) {
            delay(4200)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = error != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .safeDrawingPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                error ?: "",
                color = Starlight,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Starlight)
            }
        }
    }
}
