package com.orynex.bolosaathi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object Motion {
    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    fun <T> soft() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
}

fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.snappy(), label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = ripple(), enabled = enabled, onClick = onClick)
}

fun Modifier.enterStagger(index: Int, stepMs: Long = 55L, rise: Dp = 18.dp): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMs)
        a.animateTo(1f, tween(460, easing = Motion.EaseOut))
    }
    this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * rise.toPx()
    }
}

@Composable
fun MicHalo(listening: Boolean, level: Float, color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "halo")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "phase")
    val lvl by animateFloatAsState(if (listening) level else 0f, tween(140), label = "lvl")
    val show by animateFloatAsState(if (listening) 1f else 0f, tween(300, easing = Motion.EaseOut), label = "show")
    Canvas(modifier) {
        if (show <= 0.01f) return@Canvas
        val base = size.minDimension * 0.30f
        for (i in 0 until 3) {
            val p = (phase + i / 3f) % 1f
            val r = base + (size.minDimension * 0.22f) * p + lvl * size.minDimension * 0.06f
            drawCircle(color.copy(alpha = (1f - p) * 0.22f * show), radius = r)
        }
        drawCircle(color.copy(alpha = 0.35f * show), radius = base + lvl * size.minDimension * 0.08f, style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun VoiceBars(level: Float, color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bars")
    val weights = listOf(0.55f, 0.85f, 1f, 0.75f, 0.5f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        weights.forEachIndexed { i, w ->
            val wob by t.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(420 + i * 90, easing = Motion.EaseInOut), RepeatMode.Reverse),
                label = "b$i",
            )
            val h by animateFloatAsState((0.2f + level * w * wob).coerceIn(0.15f, 1f), tween(110), label = "h$i")
            Box(Modifier.width(4.dp).height(22.dp * h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

@Composable
fun ThinkingDots(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(900, delayMillis = i * 150, easing = Motion.EaseInOut), RepeatMode.Restart),
                label = "d$i",
            )
            val lift = if (y < 0.5f) y * 2f else (1f - y) * 2f
            Box(
                Modifier
                    .graphicsLayer { translationY = -lift * 6.dp.toPx(); alpha = 0.45f + 0.55f * lift }
                    .size(8.dp).clip(CircleShape).background(color)
            )
        }
    }
}
