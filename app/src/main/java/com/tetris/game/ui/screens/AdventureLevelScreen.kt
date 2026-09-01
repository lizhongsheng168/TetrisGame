package com.tetris.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureLevelScreen(
    saveManager: SaveManager,
    onPlayLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    val currentLevel by saveManager.currentLevel.collectAsState(initial = 1)
    val starLevels by saveManager.starLevels.collectAsState(initial = emptyMap())
    val coins by saveManager.coins.collectAsState(initial = 0)

    // 世界页签：1-50, 51-100, 101-150, 151-200
    var worldIndex by remember { mutableIntStateOf(0) }
    val worlds = listOf(
        "世界一" to (1..50),
        "世界二" to (51..100),
        "世界三" to (101..150),
        "世界四" to (151..200)
    )
    val (worldName, worldRange) = worlds[worldIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("冒险闯关", color = TextPrimary, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                }
            },
            actions = {
                Text(
                    text = "金币: $coins",
                    color = SecondaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = GameBackground)
        )

        // 世界切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            worlds.forEachIndexed { index, (name, _) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (index == worldIndex) PrimaryAccent else BoardBackground,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { worldIndex = index }
                ) {
                    Text(
                        text = name,
                        color = if (index == worldIndex) GameBackground else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 关卡网格
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
                    onClick = { if (levelId <= currentLevel) onPlayLevel(levelId) }
                )
            }
        }
    }
}

@Composable
private fun LevelCell(
    levelId: Int,
    stars: Int,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        !unlocked -> BoardBorder.copy(alpha = 0.4f)
        stars == 3 -> SuccessGreen.copy(alpha = 0.25f)
        stars == 2 -> SecondaryAccent.copy(alpha = 0.25f)
        stars == 1 -> PrimaryAccent.copy(alpha = 0.25f)
        else -> BoardBackground
    }
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(enabled = unlocked, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (unlocked) "$levelId" else "🔒",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) TextPrimary else TextSecondary
        )
        if (stars > 0) {
            Text(
                text = "★".repeat(stars),
                fontSize = 10.sp,
                color = SecondaryAccent,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
