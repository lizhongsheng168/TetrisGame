package com.tetris.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tetris.game.audio.BgmManager
import com.tetris.game.audio.SoundManager
import com.tetris.game.data.SaveManager
import com.tetris.game.engine.GameMode
import com.tetris.game.engine.LevelCatalog
import com.tetris.game.license.LicenseManager
import com.tetris.game.ui.components.TrialGateSheet
import com.tetris.game.ui.screens.AdventureLevelScreen
import com.tetris.game.ui.screens.GameScreen
import com.tetris.game.ui.screens.HomeScreen
import com.tetris.game.ui.screens.LicenseScreen
import com.tetris.game.ui.screens.SettingsScreen
import com.tetris.game.ui.theme.LocalAppTheme
import com.tetris.game.ui.theme.themeOf
import kotlinx.coroutines.launch

/** 应用内导航路由 */
sealed class Screen {
    data object Home : Screen()
    data object Adventure : Screen()
    data object Settings : Screen()
    data object License : Screen()
    data class Play(val mode: GameMode, val levelId: Int = 0) : Screen()
}

@Composable
fun TetrisApp() {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context.applicationContext) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showTrialGate by remember { mutableStateOf(false) }
    var pendingPlay: Screen.Play? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    val verifyState by LicenseManager.verifyFlow(context).collectAsState(initial = null)
    val themeId by saveManager.themeId.collectAsState(initial = "starry")
    val appTheme = themeOf(themeId)
    val bgmEnabled by saveManager.bgmEnabled.collectAsState(initial = true)
    val soundEnabledGlobal by saveManager.soundEnabled.collectAsState(initial = true)
    val bgmTrack by saveManager.bgmTrack.collectAsState(initial = "energy")
    val trialPlays by saveManager.trialPlays.collectAsState(initial = 0)

    // 全局开关同步：音效为总开关，关掉则所有声音静音
    androidx.compose.runtime.LaunchedEffect(soundEnabledGlobal) { SoundManager.enabled = soundEnabledGlobal }
    androidx.compose.runtime.LaunchedEffect(soundEnabledGlobal) { if (!soundEnabledGlobal) BgmManager.stop() }
    // 首页/大厅 BGM：仅在 Home/Adventure/Settings 播放，进入对局/授权由各自接管；受 soundEnabled 总开关联动，随选曲即切换
    androidx.compose.runtime.LaunchedEffect(currentScreen, bgmEnabled, soundEnabledGlobal, bgmTrack) {
        val isLobby = currentScreen is Screen.Home || currentScreen is Screen.Adventure || currentScreen is Screen.Settings
        if (isLobby && bgmEnabled && soundEnabledGlobal) BgmManager.start(bgmTrack) else BgmManager.stop()
    }

    // 切后台全局静音：大厅/对局 BGM 都需随后台静音
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0L) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> BgmManager.pause()
                Lifecycle.Event.ON_RESUME -> resumeTick++
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    // 回前台后：大厅由本层恢复，对局由 GameScreen 自行判断（暂停/终局/断点弹窗需保持静音）
    androidx.compose.runtime.LaunchedEffect(resumeTick, currentScreen, bgmEnabled, soundEnabledGlobal) {
        if (resumeTick == 0L) return@LaunchedEffect
        if (!bgmEnabled || !soundEnabledGlobal) { BgmManager.pause(); return@LaunchedEffect }
        val isLobby = currentScreen is Screen.Home || currentScreen is Screen.Adventure || currentScreen is Screen.Settings
        if (isLobby) BgmManager.resume()
        // 对局页不自动 resume，避免在暂停/终局/断点恢复弹窗时误播
    }

    if (verifyState == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(appTheme.gameBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = appTheme.primaryAccent)
        }
        return
    }

    val isLicensed = verifyState is LicenseManager.VerifyResult.Valid
    val canTrial = trialPlays < SaveManager.TRIAL_LIMIT
    val forceLock = !isLicensed && !canTrial

    // 试玩用完强制停留在授权页，不可通过返回绕过
    LaunchedEffect(forceLock) {
        if (forceLock) currentScreen = Screen.License
    }

    fun gateOrPlay(play: Screen.Play) {
        if (isLicensed || canTrial) {
            currentScreen = play
            return
        }
        pendingPlay = play
        showTrialGate = true
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        // 强制锁死：直接展示授权页（不可返回）
        if (forceLock) {
            LicenseScreen(
                onActivated = {},
                onBack = null
            )
            // 锁死时也允许弹引导层（去激活本身就在此页，这里不额外弹）
            return@CompositionLocalProvider
        }

        when (val screen = currentScreen) {
            is Screen.Home -> HomeScreen(
                saveManager = saveManager,
                isLicensed = isLicensed,
                trialRemain = (SaveManager.TRIAL_LIMIT - trialPlays).coerceAtLeast(0),
                onPlayEndless = { gateOrPlay(Screen.Play(GameMode.ENDLESS)) },
                onPlayAdventure = {
                    if (isLicensed || canTrial) currentScreen = Screen.Adventure
                    else {
                        pendingPlay = null
                        showTrialGate = true
                    }
                },
                onOpenSettings = { currentScreen = Screen.Settings },
                onActivate = {
                    pendingPlay = null
                    showTrialGate = true
                }
            )
            is Screen.Adventure -> AdventureLevelScreen(
                saveManager = saveManager,
                onPlayLevel = { levelId ->
                    gateOrPlay(Screen.Play(GameMode.ADVENTURE, levelId))
                },
                onBack = { currentScreen = Screen.Home }
            )
            is Screen.Settings -> SettingsScreen(
                saveManager = saveManager,
                onBack = { currentScreen = Screen.Home }
            )
            is Screen.License -> LicenseScreen(
                onActivated = { currentScreen = Screen.Home },
                onBack = { currentScreen = Screen.Home }
            )
            is Screen.Play -> GameScreen(
                saveManager = saveManager,
                mode = screen.mode,
                levelId = screen.levelId,
                levelConfig = if (screen.mode == GameMode.ADVENTURE && screen.levelId > 0) {
                    LevelCatalog.getLevel(screen.levelId)
                } else null,
                isLicensed = isLicensed,
                trialRemainBefore = (SaveManager.TRIAL_LIMIT - trialPlays).coerceAtLeast(0),
                onTrialConsumed = {
                    if (!isLicensed) scope.launch { saveManager.incrementTrialPlays() }
                },
                onExit = {
                    currentScreen = if (screen.mode == GameMode.ADVENTURE) Screen.Adventure else Screen.Home
                },
                onNextLevel = { nextId ->
                    val next = Screen.Play(GameMode.ADVENTURE, nextId)
                    if (isLicensed || canTrial) {
                        currentScreen = next
                    } else {
                        pendingPlay = next
                        showTrialGate = true
                        currentScreen = Screen.Adventure
                    }
                },
                onRequireLicense = {
                    pendingPlay = screen
                    showTrialGate = true
                }
            )
        }

        if (showTrialGate) {
            TrialGateSheet(
                isLicensed = isLicensed,
                trialRemain = (SaveManager.TRIAL_LIMIT - trialPlays).coerceAtLeast(0),
                onDismiss = { showTrialGate = false },
                onGoLicense = {
                    showTrialGate = false
                    currentScreen = Screen.License
                },
                onContinueTrial = {
                    showTrialGate = false
                    pendingPlay?.let { currentScreen = it; pendingPlay = null }
                }
            )
        }
    }
}
