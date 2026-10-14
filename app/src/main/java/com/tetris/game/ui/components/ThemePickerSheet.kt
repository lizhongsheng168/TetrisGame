package com.tetris.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.ui.theme.AppTheme
import com.tetris.game.ui.theme.AppThemes

@Composable
fun ThemePickerSheet(
    currentId: String,
    onPick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "选择主题", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppThemes.first().textPrimary)
        // 实际颜色由外层 LocalAppTheme 决定，这里仅展示名称，卡片背景用主题色
        Spacer(modifier = Modifier.height(10.dp))
        AppThemes.forEach { theme ->
            val selected = theme.id.id == currentId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.boardCardBackground)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) theme.boardBorderGlow else theme.boardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onPick(theme.id.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemePreviewMini(theme)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = theme.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    Text(text = theme.subtitle, fontSize = 11.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    Row(modifier = Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        theme.blockColors.take(7).forEach { c ->
                            Box(modifier = Modifier.size(13.dp).clip(CircleShape).background(c).border(0.5.dp, theme.boardBorder, CircleShape))
                        }
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(theme.primaryAccentStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = theme.gameBackground, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ThemePreviewMini(theme: AppTheme) {
    // 4 个小方块 + 棋盘微缩
    Canvas(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(theme.boardBackground)) {
        val cw = size.width / 4f
        val ch = size.height / 5f
        val gap = 2f
        val r = 3.5f
        // 4 块示例占据 2x2 区域
        val cols = listOf(
            0 to theme.blockColors[0],
            1 to theme.blockColors[2],
            2 to theme.blockColors[5],
            3 to theme.blockColors[6],
        )
        cols.forEachIndexed { idx, (_, color) ->
            val col = idx % 2
            val row = idx / 2
            val left = col * (cw) + gap
            val top = row * (ch) + gap
            val w = cw - gap * 2
            val h = ch - gap * 2
            drawRoundRect(color = color, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(r))
            drawRoundRect(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                topLeft = Offset(left + 1.2f, top + 1.2f),
                size = Size(w * 0.55f, h * 0.28f),
                cornerRadius = CornerRadius(r * 0.6f)
            )
        }
        // 底部淡线示意棋盘网格
        val lineColor = theme.cellEmptyStroke.copy(alpha = 0.35f)
        for (c in 1..3) drawLine(lineColor, Offset(c * cw, 0f), Offset(c * cw, size.height), strokeWidth = 0.6f)
        for (r2 in 1..4) drawLine(lineColor, Offset(0f, r2 * ch), Offset(size.width, r2 * ch), strokeWidth = 0.6f)
    }
}
