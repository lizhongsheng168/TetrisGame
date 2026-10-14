package com.tetris.game.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import com.tetris.game.ui.theme.LocalAppTheme

private data class TutorialStep(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accentBottom: Color? = null
)

@Composable
fun TutorialOverlay(
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    val theme = LocalAppTheme.current
    var step by remember { mutableIntStateOf(0) }

    val steps = remember(theme) {
        listOf(
            TutorialStep(
                title = "左右移动",
                desc = "点 ← → 单步移动\n长按可连续平移",
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft
            ),
            TutorialStep(
                title = "旋转方块",
                desc = "点 旋转 让方块顺时针转 90°\n卡住也能贴墙转（踢墙）",
                icon = Icons.Filled.Refresh
            ),
            TutorialStep(
                title = "加速下落",
                desc = "长按 加速 让方块快速下落\n松手即停，用于精准落位",
                icon = Icons.Filled.KeyboardArrowDown
            ),
            TutorialStep(
                title = "幽灵块",
                desc = "半透明的影子就是落点预览\n在设置里可开关",
                icon = Icons.Filled.Visibility
            ),
        )
    }
    val total = steps.size
    val cur = steps[step.coerceIn(0, total - 1)]

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.boardCardBackground,
            border = BorderStroke(1.dp, theme.boardBorderGlow.copy(alpha = 0.6f)),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 步骤指示
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(total) { i ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == step) 22.dp else 10.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (i == step) theme.primaryAccentStrong else theme.boardBorder.copy(alpha = 0.9f))
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("${step + 1}/$total", fontSize = 11.sp, color = theme.textMuted, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))

                // 图标
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(theme.primaryAccentStrong.copy(alpha = 0.95f), theme.secondaryAccent.copy(alpha = 0.95f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(cur.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(cur.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary, textAlign = TextAlign.Center)
                Text(cur.desc, fontSize = 13.sp, color = theme.textSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))

                // 幽灵块示意（仅最后一步）
                if (step == total - 1) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.boardBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (i < 2) theme.primaryAccentStrong else theme.primaryAccentStrong.copy(alpha = 0.22f))
                            )
                        }
                        Text("← 幽灵块", fontSize = 11.sp, color = theme.textSecondary, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                        Text("跳过", color = theme.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (step < total - 1) step++ else onDone()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong)
                    ) {
                        Text(if (step < total - 1) "下一步" else "开始游戏", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
