package com.tetris.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.components.ParallaxBackground
import com.tetris.game.ui.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureLevelScreen(
    saveManager: SaveManager,
    onPlayLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val currentLevel by saveManager.currentLevel.collectAsState(initial = 1)
    val starLevels by saveManager.starLevels.collectAsState(initial = emptyMap())
    val coins by saveManager.coins.collectAsState(initial = 0)

    var worldIndex by remember { mutableIntStateOf(0) }
    var snappedWorld by remember { mutableStateOf(false) }
    var lockHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentLevel) {
        if (!snappedWorld && currentLevel > 1) {
            worldIndex = when {
                currentLevel > 150 -> 3
                currentLevel > 100 -> 2
                currentLevel > 50 -> 1
                else -> 0
            }
            snappedWorld = true
        }
    }
    val worlds = listOf(
        "世界一" to (1..50),
        "世界二" to (51..100),
        "世界三" to (101..150),
        "世界四" to (151..200)
    )
    val (worldName, worldRange) = worlds[worldIndex]

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        // 棋盘外背景：微渐变 + 星点/网格视差，按选中世界染一点主题色
        ParallaxBackground(theme = theme, levelId = worldRange.first, driftEnabled = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 6.dp)
        ) {
            TopAppBar(
                title = { Text("冒险闯关", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = theme.textPrimary)
                    }
                },
                actions = {
                    Text(
                        text = "金币: $coins",
                        color = theme.secondaryAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                worlds.forEachIndexed { index, (name, _) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (index == worldIndex) theme.primaryAccent else theme.boardBackground,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                worldIndex = index
                                lockHint = null
                            }
                    ) {
                        Text(
                            text = name,
                            color = if (index == worldIndex) theme.gameBackground else theme.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            val worldLocked = worldRange.first > currentLevel
            Text(
                text = if (worldLocked) {
                    "通过第 ${worldRange.first - 1} 关后解锁$worldName"
                } else {
                    "第 ${worldRange.first}–${worldRange.last} 关 · 下一关是第 $currentLevel 关"
                },
                color = if (worldLocked) theme.secondaryAccent else theme.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
            if (!worldLocked) {
                Text(
                    text = "过关至少 1 星，连消或省下步数可以到 3 星",
                    color = theme.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            if (lockHint != null) {
                Text(
                    text = lockHint!!,
                    color = theme.dangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    textAlign = TextAlign.Center
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(worldRange.toList()) { levelId ->
                    LevelCell(
                        levelId = levelId,
                        stars = starLevels[levelId] ?: 0,
                        unlocked = levelId <= currentLevel,
                        isCurrent = levelId == currentLevel,
                        onClick = {
                            if (levelId <= currentLevel) {
                                lockHint = null
                                onPlayLevel(levelId)
                            } else {
                                lockHint = "先通过第 ${levelId - 1} 关，才能挑战第 $levelId 关"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCell(
    levelId: Int,
    stars: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val bg = when {
        !unlocked -> theme.boardBorder.copy(alpha = 0.4f)
        stars == 3 -> theme.successGreen.copy(alpha = 0.22f)
        stars == 2 -> theme.secondaryAccent.copy(alpha = 0.22f)
        stars == 1 -> theme.primaryAccent.copy(alpha = 0.22f)
        else -> theme.boardBackground
    }
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(bg, RoundedCornerShape(12.dp))
            .then(
                if (isCurrent && unlocked) Modifier.border(2.dp, theme.primaryAccentStrong, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (unlocked) "$levelId" else "🔒",
            fontSize = if (isCurrent && unlocked) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) theme.textPrimary else theme.textSecondary
        )
        if (isCurrent && unlocked && stars == 0) {
            Text(text = "当前", fontSize = 9.sp, color = theme.primaryAccentStrong, fontWeight = FontWeight.Bold)
        }
        if (stars > 0) {
            Text(
                text = "★".repeat(stars),
                fontSize = 10.sp,
                color = theme.secondaryAccent,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
