package com.tetris.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tetris.game.data.SaveManager
import com.tetris.game.engine.GameMode
import com.tetris.game.engine.LevelCatalog
import com.tetris.game.ui.screens.AdventureLevelScreen
import com.tetris.game.ui.screens.GameScreen
import com.tetris.game.ui.screens.HomeScreen

/** 应用内导航路由 */
sealed class Screen {
    data object Home : Screen()
    data object Adventure : Screen()
    data class Play(val mode: GameMode, val levelId: Int = 0) : Screen()
}

@Composable
fun TetrisApp() {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context.applicationContext) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home -> HomeScreen(
            saveManager = saveManager,
            onPlayEndless = { currentScreen = Screen.Play(GameMode.ENDLESS) },
            onPlayAdventure = { currentScreen = Screen.Adventure }
        )
        is Screen.Adventure -> AdventureLevelScreen(
            saveManager = saveManager,
            onPlayLevel = { levelId ->
                currentScreen = Screen.Play(GameMode.ADVENTURE, levelId)
            },
            onBack = { currentScreen = Screen.Home }
        )
        is Screen.Play -> GameScreen(
            saveManager = saveManager,
            mode = screen.mode,
            levelId = screen.levelId,
            levelConfig = if (screen.mode == GameMode.ADVENTURE && screen.levelId > 0) {
                LevelCatalog.getLevel(screen.levelId)
            } else null,
            onExit = {
                currentScreen = if (screen.mode == GameMode.ADVENTURE) Screen.Adventure else Screen.Home
            }
        )
    }
}
