package com.tetris.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tetris.game.ui.theme.AppTheme
import com.tetris.game.ui.theme.ThemeId
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val alpha: Float, val speed: Float, val twinklePhase: Float)

@Composable
fun ParallaxBackground(
    theme: AppTheme,
    modifier: Modifier = Modifier,
    levelId: Int? = null,
    // tie parallax to gameplay tick if you want; default infinite drift
    driftEnabled: Boolean = true,
) {
    val drift by rememberInfiniteTransition(label = "bgDrift").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val twinkle by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f, targetValue = 6.2831f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )

    // deterministic stars per theme (+level world variation)
    val stars = remember(theme.id, levelId) {
        val rnd = Random(theme.id.ordinal * 1009L + (levelId ?: 0) * 17L + 42L)
        List(56) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                r = rnd.nextFloat() * 1.6f + 0.7f,
                alpha = rnd.nextFloat() * 0.55f + 0.28f,
                speed = rnd.nextFloat() * 0.45f + 0.45f,
                twinklePhase = rnd.nextFloat() * 6.28f,
            )
        }
    }

    val density = LocalDensity.current
    val worldTint = remember(levelId) { worldTintFor(levelId) }

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.gameBackgroundTop, theme.gameBackgroundBottom)))) {
        // 1) 主渐变已在Box背景；叠两枚径向光斑（随主题/世界微变色）
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(theme.primaryAccent.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(0.85f * 1080f, 0.12f * 1920f),
                    radius = 900f
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(theme.secondaryAccent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0.18f * 1080f, 0.92f * 1920f),
                    radius = 820f
                )
            )
        )
        // 世界主题微 tint（仅冒险关卡）
        if (worldTint != null) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(worldTint.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(0.5f * 1080f, 0.06f * 1920f),
                        radius = 760f
                    )
                )
            )
        }
        // 2) 纹理层：Canvas 按主题绘制
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (theme.id) {
                ThemeId.STARRY -> {
                    // 星点视差
                    val d = if (driftEnabled) drift else 0f
                    stars.forEach { s ->
                        val y0 = (s.y + d * 0.12f * s.speed) % 1f
                        val tw = 0.62f + 0.38f * sin(twinkle * 0.7f + s.twinklePhase)
                        val a = (s.alpha * tw).coerceIn(0.12f, 1f) * 0.95f
                        drawCircle(
                            color = Color.White.copy(alpha = a),
                            radius = with(density) { s.r.dp.toPx() },
                            center = Offset(s.x * w, y0 * h)
                        )
                        // 微光晕（大星）
                        if (s.r > 1.55f) {
                            drawCircle(
                                color = theme.primaryAccent.copy(alpha = a * 0.18f),
                                radius = with(density) { (s.r * 2.2f).dp.toPx() },
                                center = Offset(s.x * w, y0 * h)
                            )
                        }
                    }
                    // 淡淡星尘带（横向）
                    drawCircle(
                        color = theme.primaryAccent.copy(alpha = 0.035f),
                        radius = w * 0.62f,
                        center = Offset(w * 0.52f, h * 0.22f)
                    )
                }
                ThemeId.CYBER -> {
                    // 细网格 + 霓虹斜纹
                    val grid = with(density) { 28.dp.toPx() }
                    val gridColor = Color.White.copy(alpha = 0.035f)
                    var x = 0f
                    while (x < w) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                        x += grid
                    }
                    var y = 0f
                    while (y < h) {
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        y += grid
                    }
                    // 霓虹对角线
                    val d = if (driftEnabled) drift else 0f
                    val neonA = theme.primaryAccentStrong.copy(alpha = 0.08f)
                    val neonB = theme.secondaryAccent.copy(alpha = 0.07f)
                    val off = d * w * 0.18f
                    drawLine(neonA, Offset(-w * 0.1f + off, h * 0.32f), Offset(w * 0.9f + off, h * 0.08f), strokeWidth = with(density) { 1.6.dp.toPx() })
                    drawLine(neonB, Offset(-w * 0.08f + off * 0.7f, h * 0.72f), Offset(w * 0.92f + off * 0.7f, h * 0.48f), strokeWidth = with(density) { 1.4.dp.toPx() })
                    // 星点少量
                    stars.take(18).forEach { s ->
                        val y0 = (s.y + d * 0.08f * s.speed) % 1f
                        drawCircle(Color.White.copy(alpha = s.alpha * 0.42f), with(density) { (s.r * 0.85f).dp.toPx() }, Offset(s.x * w, y0 * h))
                    }
                }
                ThemeId.CANDY -> {
                    // 柔和大光斑 blobs
                    val d = if (driftEnabled) drift else 0f
                    val blobs = listOf(
                        Triple(0.22f, 0.16f, theme.primaryAccent.copy(alpha = 0.10f)),
                        Triple(0.84f, 0.24f, theme.secondaryAccent.copy(alpha = 0.09f)),
                        Triple(0.18f, 0.86f, Color(0xFFB794F6).copy(alpha = 0.08f)),
                        Triple(0.78f, 0.82f, Color(0xFF7DD3C8).copy(alpha = 0.07f)),
                    )
                    blobs.forEachIndexed { i, (bx, by, col) ->
                        val dy = sin(d * 6.28f + i * 1.9f) * h * 0.012f
                        drawCircle(col, radius = w * 0.36f, center = Offset(bx * w, by * h + dy))
                    }
                    // 细星粉
                    stars.take(22).forEach { s ->
                        val y0 = (s.y + d * 0.06f * s.speed) % 1f
                        drawCircle(Color.White.copy(alpha = s.alpha * 0.32f), with(density) { (s.r * 0.9f).dp.toPx() }, Offset(s.x * w, y0 * h))
                    }
                }
            }
        }
        // 3) 边缘暗角 vignette
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                    center = Offset(0.5f * 1080f, 0.5f * 1920f),
                    radius = 1300f
                )
            )
        )
        // 顶部微高光
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.045f), Color.Transparent),
                    startY = 0f, endY = 220f
                )
            )
        )
    }
}

private fun worldTintFor(levelId: Int?): Color? {
    if (levelId == null || levelId <= 0) return null
    return when ((levelId - 1) / 50) {
        0 -> Color(0xFF22D3EE) // W1 深海
        1 -> Color(0xFFFF6B6B) // W2 火山
        2 -> Color(0xFF60A5FA) // W3 冰川
        else -> Color(0xFFC084FC) // W4 星域
    }
}
