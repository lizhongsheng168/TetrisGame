package com.tetris.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tetris.game.engine.BOARD_HEIGHT
import com.tetris.game.engine.BOARD_WIDTH
import com.tetris.game.engine.TetrisEngine
import com.tetris.game.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BoardView(
    engine: TetrisEngine,
    tick: Long,
    clearSeq: Long = engine.clearSeq,
    lockSeq: Long = engine.lockSeq,
    ghostEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val forceRecompose = tick
    val theme = LocalAppTheme.current
    val aspectRatio = BOARD_WIDTH.toFloat() / BOARD_HEIGHT.toFloat()

    val clearAnim = remember { Animatable(1f) }
    val lockBump = remember { Animatable(0f) }
    var activeRows by remember { mutableStateOf(emptyList<Int>()) }
    var snapshot by remember { mutableStateOf(emptyList<List<Int>>()) }
    var animSeq by remember { mutableStateOf(0L) }

    LaunchedEffect(clearSeq) {
        if (clearSeq != 0L && clearSeq != animSeq) {
            animSeq = clearSeq
            activeRows = engine.lastClearedRows.toList()
            snapshot = engine.clearedSnapshot.map { it.toList() }
            clearAnim.snapTo(0f)
            launch { clearAnim.animateTo(1f, animationSpec = tween(520)) }
        }
    }
    LaunchedEffect(lockSeq) {
        if (lockSeq == 0L) return@LaunchedEffect
        // 每次锁定：重落地小弹跳 1.0 -> 1.07 -> 0.97 -> 1.0（约140ms）
        lockBump.snapTo(0f)
        lockBump.animateTo(1f, animationSpec = tween(72))
        lockBump.animateTo(0.42f, animationSpec = tween(48))
        lockBump.animateTo(0f, animationSpec = tween(56))
    }
    LaunchedEffect(tick) {
        if (clearSeq == 0L && activeRows.isNotEmpty()) {
            activeRows = emptyList()
            snapshot = emptyList()
        }
    }
    val clearProgress = clearAnim.value
    // lockBump 0..1 -> scale 1.0..1.035 回弹，不影响布局
    val bump = lockBump.value
    val lockScale = 1f + (if (bump < 0.55f) bump / 0.55f * 0.035f else (1f - (bump - 0.55f) / 0.45f) * 0.035f - (bump - 0.55f) / 0.45f * 0.015f)

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .shadow(18.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.5.dp)
                .clip(RoundedCornerShape(14.dp))
                .graphicsLayer(scaleX = lockScale, scaleY = lockScale)
        ) {
            drawBoardOptimized(engine, ghostEnabled, activeRows, snapshot, clearProgress, theme)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = theme.boardBorderGlow.copy(alpha = 0.55f),
                style = Stroke(width = 1.2.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawBoardOptimized(
    engine: TetrisEngine,
    ghostEnabled: Boolean,
    clearRows: List<Int>,
    snapshot: List<List<Int>>,
    clearProgress: Float,
    theme: com.tetris.game.ui.theme.AppTheme
) {
    val cellW = size.width / BOARD_WIDTH
    val cellH = size.height / BOARD_HEIGHT
    val gap = cellW * 0.08f
    val radius = cellW * 0.12f
    val blockRadius = cellW * 0.20f
    val isClearing = clearProgress < 1f && clearRows.isNotEmpty()

    drawRect(theme.boardBackground)

    val grid = theme.cellEmptyStroke.copy(alpha = 0.18f)
    for (c in 1 until BOARD_WIDTH) {
        val x = c * cellW
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.7f)
    }
    for (r in 1 until BOARD_HEIGHT) {
        val y = r * cellH
        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.7f)
    }

    for (row in 0 until BOARD_HEIGHT) {
        val isClearRow = isClearing && clearRows.contains(row)
        if (isClearRow) continue
        for (col in 0 until BOARD_WIDTH) {
            val cell = engine.board[row][col]
            val left = col * cellW
            val top = row * cellH
            when {
                cell == null -> {
                    drawRoundRect(
                        color = theme.cellEmptyStroke.copy(alpha = 0.42f),
                        topLeft = Offset(left + gap * 0.55f, top + gap * 0.55f),
                        size = Size(cellW - gap * 1.1f, cellH - gap * 1.1f),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = 0.9f)
                    )
                }
                cell == -1 -> drawObstacleFast(left, top, cellW, cellH, gap, theme)
                else -> drawBlockFast(
                    color = theme.blockColors[cell % theme.blockColors.size],
                    left = left, top = top,
                    cellW = cellW, cellH = cellH, gap = gap,
                    blockRadius = blockRadius, withGlow = false
                )
            }
        }
    }

    if (isClearing) {
        for ((idx, row) in clearRows.withIndex()) {
            val rowSnapshot = snapshot.getOrNull(idx) ?: List(BOARD_WIDTH) { 0 }
            val yTop = row * cellH
            when {
                clearProgress < 0.28f -> {
                    val flashAlpha = (1f - clearProgress / 0.28f) * 0.95f
                    drawRect(Color.White.copy(alpha = flashAlpha), Offset(0f, yTop), Size(size.width, cellH))
                    drawRect(Color.White.copy(alpha = flashAlpha * 0.35f), Offset(0f, yTop - 2f), Size(size.width, 2f))
                    drawRect(Color.White.copy(alpha = flashAlpha * 0.35f), Offset(0f, yTop + cellH), Size(size.width, 2f))
                    for (col in 0 until BOARD_WIDTH) {
                        val ci = rowSnapshot.getOrNull(col) ?: 0
                        val colColor = theme.blockColors[ci % theme.blockColors.size]
                        drawBlockFast(colColor.copy(alpha = 0.55f), col * cellW, yTop, cellW, cellH, gap, blockRadius, false)
                    }
                }
                clearProgress < 0.55f -> {
                    val t = (clearProgress - 0.28f) / 0.27f
                    val shrink = 1f - t
                    val wobble = sin(t * 3.14159f * 2f) * 0.06f
                    for (col in 0 until BOARD_WIDTH) {
                        val ci = rowSnapshot.getOrNull(col) ?: 0
                        val colColor = theme.blockColors[ci % theme.blockColors.size]
                        val cx = col * cellW + cellW / 2f
                        val cy = yTop + cellH / 2f
                        val curW = (cellW - gap) * (shrink + wobble)
                        val curH = (cellH - gap) * (shrink + wobble)
                        if (curW > 1f && curH > 1f) {
                            drawRoundRect(
                                color = colColor,
                                topLeft = Offset(cx - curW / 2f, cy - curH / 2f),
                                size = Size(curW, curH),
                                cornerRadius = CornerRadius(blockRadius * shrink)
                            )
                            if (shrink > 0.4f) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.18f * shrink),
                                    topLeft = Offset(cx - curW / 2f + 1f, cy - curH / 2f + 1f),
                                    size = Size(curW * 0.6f, curH * 0.28f),
                                    cornerRadius = CornerRadius(blockRadius * 0.6f)
                                )
                            }
                        }
                    }
                    val sweepX = size.width * ((clearProgress - 0.28f) / 0.27f)
                    drawRect(Color.White.copy(alpha = 0.22f * (1f - t)), Offset(sweepX - 18f, yTop), Size(36f, cellH))
                }
                else -> {
                    val t = (clearProgress - 0.55f) / 0.45f
                    for (col in 0 until BOARD_WIDTH) {
                        val ci = rowSnapshot.getOrNull(col) ?: 0
                        val colColor = theme.blockColors[ci % theme.blockColors.size]
                        val baseX = col * cellW + cellW / 2f
                        val baseY = yTop + cellH / 2f
                        val seed = row * 100 + col
                        val rnd = Random(seed)
                        for (p in 0 until 4) {
                            val ang = rnd.nextFloat() * 3.14159f * 2f + p * 0.9f
                            val speed = (cellW * 0.55f + rnd.nextFloat() * cellW * 0.9f)
                            val dx = kotlin.math.cos(ang) * speed * t
                            val dy = kotlin.math.sin(ang) * speed * t - t * 10f
                            val alpha = (1f - t) * 0.95f
                            val sz = (cellW * 0.22f - t * cellW * 0.12f).coerceAtLeast(1.2f)
                            drawRoundRect(
                                color = colColor.copy(alpha = alpha),
                                topLeft = Offset(baseX + dx - sz / 2f, baseY + dy - sz / 2f),
                                size = Size(sz, sz),
                                cornerRadius = CornerRadius(sz / 2f)
                            )
                            if (p == 0) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = alpha * 0.9f),
                                    topLeft = Offset(baseX + dx * 0.55f - 1f, baseY + dy * 0.55f - 1f),
                                    size = Size(2.2f, 2.2f),
                                    cornerRadius = CornerRadius(1.1f)
                                )
                            }
                        }
                    }
                    drawRect(Color.White.copy(alpha = 0.12f * (1f - t)), Offset(0f, yTop + cellH / 2f - 1f), Size(size.width, 2f))
                }
            }
        }
        if (clearProgress in 0.55f..0.92f) {
            val dropT = (clearProgress - 0.55f) / 0.37f
            drawRect(Color.White.copy(alpha = 0.02f * (1f - dropT)), Offset.Zero, size)
        }
    }

    engine.current?.let { piece ->
        val ghostDist = if (ghostEnabled) engine.ghostOffset() else 0
        val shape = piece.shape()
        if (ghostEnabled && ghostDist > 0) {
            for (r in shape.indices) for (c in shape[r].indices) if (shape[r][c] != 0) {
                val bx = piece.x + c
                val by = piece.y + r + ghostDist
                if (by in 0 until BOARD_HEIGHT && bx in 0 until BOARD_WIDTH) {
                    val l = bx * cellW; val t = by * cellH
                    drawRoundRect(
                        color = theme.cellGhost.copy(alpha = 0.16f),
                        topLeft = Offset(l + gap / 2, t + gap / 2),
                        size = Size(cellW - gap, cellH - gap),
                        cornerRadius = CornerRadius(cellW * 0.16f)
                    )
                    drawRoundRect(
                        color = theme.cellGhostBorder.copy(alpha = 0.82f),
                        topLeft = Offset(l + gap / 2, t + gap / 2),
                        size = Size(cellW - gap, cellH - gap),
                        cornerRadius = CornerRadius(cellW * 0.16f),
                        style = Stroke(width = 1.35f)
                    )
                }
            }
        }
        val curColor = theme.blockColors[piece.type.colorIndex % theme.blockColors.size]
        for (r in shape.indices) for (c in shape[r].indices) if (shape[r][c] != 0) {
            val bx = piece.x + c; val by = piece.y + r
            if (by in 0 until BOARD_HEIGHT && bx in 0 until BOARD_WIDTH) {
                drawBlockFast(curColor, bx * cellW, by * cellH, cellW, cellH, gap, blockRadius, withGlow = true)
            }
        }
    }
}

private fun DrawScope.drawObstacleFast(left: Float, top: Float, cw: Float, ch: Float, gap: Float, theme: com.tetris.game.ui.theme.AppTheme) {
    val r = cw * 0.14f
    drawRoundRect(theme.obstacleColor, Offset(left + gap / 2, top + gap / 2), Size(cw - gap, ch - gap), CornerRadius(r))
    drawRoundRect(theme.obstacleInner.copy(alpha = 0.55f), Offset(left + gap / 2 + 1.2f, top + gap / 2 + 1.2f), Size(cw - gap - 2.4f, ch - gap - 2.4f), CornerRadius(r * 0.85f), style = Stroke(1f))
}

private fun DrawScope.drawBlockFast(
    color: Color, left: Float, top: Float, cellW: Float, cellH: Float, gap: Float, blockRadius: Float, withGlow: Boolean
) {
    val pad = gap / 2; val w = cellW - gap; val h = cellH - gap
    if (withGlow) {
        drawRoundRect(color.copy(alpha = 0.22f), Offset(left + pad - 1.2f, top + pad - 1.2f), Size(w + 2.4f, h + 2.4f), CornerRadius(blockRadius + 1.2f))
    }
    drawRoundRect(color, Offset(left + pad, top + pad), Size(w, h), CornerRadius(blockRadius))
    drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(left + pad + 1.2f, top + pad + 1.2f), Size(w * 0.62f, h * 0.28f), CornerRadius(blockRadius * 0.65f))
    drawRoundRect(Color.Black.copy(alpha = 0.14f), Offset(left + pad + w * 0.45f, top + pad + h * 0.5f), Size(w * 0.55f, h * 0.5f), CornerRadius(blockRadius * 0.7f))
    drawRoundRect(Color.White.copy(alpha = 0.09f), Offset(left + pad + 0.6f, top + pad + 0.6f), Size(w - 1.2f, h - 1.2f), CornerRadius(blockRadius * 0.95f), style = Stroke(0.9f))
}
