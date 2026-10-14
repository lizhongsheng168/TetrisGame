package com.tetris.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.components.ParallaxBackground
import com.tetris.game.ui.components.ThemePickerSheet
import com.tetris.game.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    saveManager: SaveManager,
    isLicensed: Boolean = true,
    trialRemain: Int = SaveManager.TRIAL_LIMIT,
    onPlayEndless: () -> Unit,
    onPlayAdventure: () -> Unit,
    onOpenSettings: () -> Unit,
    onActivate: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val highScore by saveManager.highScore.collectAsState(initial = 0)
    val maxLevel by saveManager.maxLevel.collectAsState(initial = 0)
    val coins by saveManager.coins.collectAsState(initial = 0)
    val themeId by saveManager.themeId.collectAsState(initial = theme.id.id)
    val scope = rememberCoroutineScope()
    var showThemeSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ParallaxBackground(theme = theme, driftEnabled = true)
        // 顶部：主题 + 设置（避开状态栏：insets + 额外下移，不与顶部图标重合；与对局喇叭同高对齐）
        Row(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp).padding(top = 12.dp).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = theme.boardCardBackground,
                modifier = Modifier.clickable { onOpenSettings() }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = theme.boardCardBackground,
                modifier = Modifier.clickable { showThemeSheet = true }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = theme.primaryAccentStrong, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "主题", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "方块奇旅", fontSize = 42.sp, fontWeight = FontWeight.Black, color = theme.primaryAccent)
            Text(
                text = "经典俄罗斯方块 · 冒险之旅",
                fontSize = 14.sp,
                color = theme.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 试玩/授权状态条
            if (!isLicensed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (trialRemain > 0) theme.primaryAccentStrong.copy(alpha = 0.14f) else theme.dangerRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, if (trialRemain > 0) theme.primaryAccentStrong.copy(alpha = 0.28f) else theme.dangerRed.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable { onActivate() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = if (trialRemain > 0) theme.primaryAccentStrong else theme.dangerRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (trialRemain > 0) "试玩模式 · 剩余 $trialRemain 局" else "试玩已结束",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (trialRemain > 0) theme.textPrimary else theme.dangerRed
                            )
                            Text(
                                text = if (trialRemain > 0) "先体验 ${SaveManager.TRIAL_LIMIT} 局，再决定是否激活" else "请输入 CDKey 激活后继续游玩",
                                fontSize = 11.sp, color = theme.textSecondary
                            )
                        }
                        if (trialRemain <= 0) {
                            Surface(shape = RoundedCornerShape(99.dp), color = theme.dangerRed) {
                                Text("去激活", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        } else {
                            Text("去激活", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.primaryAccentStrong)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = theme.boardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("最高分", highScore.toString())
                    StatItem("最高关卡", if (maxLevel > 0) ("第" + maxLevel + "关") else "未通关")
                    StatItem("金币", coins.toString())
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            MainMenuButton(title = "经典无尽", subtitle = "无体力无限制 · 挑战高分", accentColor = theme.primaryAccent, onClick = onPlayEndless)
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuButton(title = "冒险闯关", subtitle = "500+ 主题关卡 · 三星挑战", accentColor = theme.secondaryAccent, onClick = onPlayAdventure)

            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "轻休闲 · 强竞技 · 碎片化游玩", fontSize = 12.sp, color = theme.textSecondary)
        }

        if (showThemeSheet) {
            ModalBottomSheet(onDismissRequest = { showThemeSheet = false }, containerColor = theme.gameBackground) {
                ThemePickerSheet(currentId = themeId, onPick = { id ->
                    scope.launch { saveManager.setThemeId(id) }
                    showThemeSheet = false
                })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    val theme = LocalAppTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
        Text(text = label, fontSize = 12.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MainMenuButton(title: String, subtitle: String, accentColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.boardBackground,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(accentColor, RoundedCornerShape(6.dp)))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Text(text = subtitle, fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
