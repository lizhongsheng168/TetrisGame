package com.tetris.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.theme.*

@Composable
fun HomeScreen(
    saveManager: SaveManager,
    onPlayEndless: () -> Unit,
    onPlayAdventure: () -> Unit
) {
    val highScore by saveManager.highScore.collectAsState(initial = 0)
    val maxLevel by saveManager.maxLevel.collectAsState(initial = 0)
    val coins by saveManager.coins.collectAsState(initial = 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                text = "方块奇旅",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryAccent
            )
            Text(
                text = "经典俄罗斯方块 · 冒险之旅",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 统计面板
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BoardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("最高分", highScore.toString())
                    StatItem("最高关卡", if (maxLevel > 0) ("第" + maxLevel + "关") else "未通关")
                    StatItem("金币", coins.toString())
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 主要按钮
            MainMenuButton(
                title = "经典无尽",
                subtitle = "无体力无限制 · 挑战高分",
                accentColor = PrimaryAccent,
                onClick = onPlayEndless
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuButton(
                title = "冒险闯关",
                subtitle = "500+ 主题关卡 · 三星挑战",
                accentColor = SecondaryAccent,
                onClick = onPlayAdventure
            )

            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "轻休闲 · 强竞技 · 碎片化游玩",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MainMenuButton(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BoardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(accentColor, RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
