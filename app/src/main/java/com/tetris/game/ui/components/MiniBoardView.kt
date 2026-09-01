package com.tetris.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tetris.game.engine.TetrominoType
import com.tetris.game.ui.theme.*
import kotlin.math.min

@Composable
fun MiniBoardView(
    piece: TetrominoType?,
    label: String,
    modifier: Modifier = Modifier
) {
    if (piece == null) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRoundRect(
                color = BoardCardBackground,
                cornerRadius = CornerRadius(10.dp.toPx())
            )
        }
        return
    }
    val shape = com.tetris.game.engine.SHAPES[piece]!![0]
    val color = BlockColors[piece.colorIndex % BlockColors.size]
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRoundRect(
            brush = Brush.linearGradient(listOf(BoardCardBackground, BoardBackground)),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        var minR = 4; var maxR = -1; var minC = 4; var maxC = -1
        for (r in shape.indices) for (c in shape[r].indices) if (shape[r][c] == 1) {
            if (r < minR) minR = r; if (r > maxR) maxR = r
            if (c < minC) minC = c; if (c > maxC) maxC = c
        }
        val rows = maxR - minR + 1
        val cols = maxC - minC + 1
        // 极限贴边 0.98 / 无上限
        val cell = min(size.width * 0.98f / cols, size.height * 0.98f / rows)
        val offsetX = (size.width - cols * cell) / 2f
        val offsetY = (size.height - rows * cell) / 2f
        for (r in minR..maxR) for (c in minC..maxC) if (shape[r][c] == 1) {
            val left = offsetX + (c - minC) * cell
            val top = offsetY + (r - minR) * cell
            val pad = 1.0f
            drawRoundRect(
                color = color.copy(alpha = 0.24f),
                topLeft = Offset(left - 0.6f, top - 0.6f),
                size = Size(cell - 0.2f, cell - 0.2f),
                cornerRadius = CornerRadius(cell * 0.22f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.88f))),
                topLeft = Offset(left + pad, top + pad),
                size = Size(cell - pad * 2, cell - pad * 2),
                cornerRadius = CornerRadius(cell * 0.22f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(left + pad + 1f, top + pad + 1f),
                size = Size((cell - pad * 2) * 0.55f, (cell - pad * 2) * 0.32f),
                cornerRadius = CornerRadius(cell * 0.12f)
            )
        }
    }
}
