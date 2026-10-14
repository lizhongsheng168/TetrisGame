package com.tetris.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tetris.game.audio.BgmManager
import com.tetris.game.audio.SoundManager
import com.tetris.game.data.GameSnapshot
import com.tetris.game.data.SaveManager
import com.tetris.game.engine.*
import com.tetris.game.ui.components.BoardView
import com.tetris.game.ui.components.MiniBoardView
import com.tetris.game.ui.components.ParallaxBackground
import com.tetris.game.ui.components.TutorialOverlay
import com.tetris.game.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun GameScreen(
    saveManager: SaveManager,
    mode: GameMode,
    levelId: Int,
    levelConfig: LevelConfig?,
    isLicensed: Boolean = true,
    trialRemainBefore: Int = SaveManager.TRIAL_LIMIT,
    onTrialConsumed: () -> Unit = {},
    onExit: () -> Unit,
    onRequireLicense: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    var restartKey by remember { mutableIntStateOf(0) }
    val engine = remember(mode, levelId, restartKey) {
        val e = if (mode == GameMode.ADVENTURE && levelConfig != null) {
            TetrisEngine(
                levelGoal = levelConfig.goalLines,
                moveLimit = levelConfig.moveLimit,
                obstacleRows = levelConfig.obstacleRows
            )
        } else {
            TetrisEngine()
        }
        e.start()
        e
    }

    var tickCount by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var levelClear by remember { mutableStateOf(false) }
    var levelFail by remember { mutableStateOf(false) }
    var stars by remember { mutableIntStateOf(0) }
    val soundEnabled by saveManager.soundEnabled.collectAsState(initial = true)
    val bgmEnabled by saveManager.bgmEnabled.collectAsState(initial = true)
    val hapticEnabled by saveManager.hapticEnabled.collectAsState(initial = true)
    val ghostEnabled by saveManager.ghostEnabled.collectAsState(initial = true)
    val autoPauseOnBg by saveManager.autoPauseOnBg.collectAsState(initial = true)
    val controlSensitivity by saveManager.controlSensitivity.collectAsState(initial = 3)
    val controlBtnScale by saveManager.controlBtnScale.collectAsState(initial = 100)
    val controlBtnSpacing by saveManager.controlBtnSpacing.collectAsState(initial = 10)
    val bgmTrack by saveManager.bgmTrack.collectAsState(initial = "energy")
    val snapshotJson by saveManager.gameSnapshot.collectAsState(initial = null)
    // 试玩消耗：进入对局即计1局（未授权时），仅计一次避免重组重复
    var trialEntryCounted by remember(mode, levelId) { mutableStateOf(false) }
    LaunchedEffect(mode, levelId, isLicensed) {
        if (!isLicensed && !trialEntryCounted) {
            trialEntryCounted = true
            onTrialConsumed()
        }
    }

    var bannerText by remember { mutableStateOf<String?>(null) }
    var bannerKey by remember { mutableIntStateOf(0) }
    var showBanner by remember { mutableStateOf(false) }
    var scorePop by remember { mutableStateOf<Int?>(null) }
    var scorePopKey by remember { mutableIntStateOf(0) }
    val shakeX = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 新手引导：仅首局（tutorialDone==false 且无断点续玩）时展示
    val tutorialDone by saveManager.tutorialDone.collectAsState(initial = true)
    var showTutorial by remember { mutableStateOf(false) }

    // 断点续玩：发现同模式同关的快照时弹窗让玩家选择
    var hasCheckedResume by remember(mode, levelId, restartKey) { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snapshotJson, mode, levelId, restartKey) {
        if (hasCheckedResume) return@LaunchedEffect
        val json = snapshotJson
        if (json != null && engine.gameState == GameState.PLAYING) {
            val snapMode = GameSnapshot.modeOf(json)
            val snapId = GameSnapshot.levelIdOf(json)
            if (snapMode == mode && snapId == levelId) {
                // 有可恢复进度，自动暂停等待选择
                if (!paused) {
                    paused = true
                    engine.togglePause()
                    BgmManager.pause()
                }
                showResumeDialog = true
            }
        }
        hasCheckedResume = true
        // 首局教学：无断点且未完成过引导时，暂停并弹出教学
        if (!tutorialDone && !showResumeDialog && !showTutorial) {
            val hasSnapshot = json != null && GameSnapshot.modeOf(json) == mode && GameSnapshot.levelIdOf(json) == levelId
            if (!hasSnapshot) {
                showTutorial = true
                if (!paused) {
                    paused = true
                    engine.togglePause()
                    BgmManager.pause()
                }
            }
        }
    }

    LaunchedEffect(soundEnabled) { SoundManager.enabled = soundEnabled }
    // 音效总开关关闭时 BGM 也静音
    LaunchedEffect(soundEnabled) { if (!soundEnabled) BgmManager.stop() }

    // BGM：按所选曲目起播，暂停/结束时暂停，离开时停播；切歌即换轨
    DisposableEffect(bgmTrack, bgmEnabled, soundEnabled) {
        if (bgmEnabled && soundEnabled) BgmManager.start(bgmTrack)
        onDispose { BgmManager.stop() }
    }
    LaunchedEffect(paused, gameOver, levelClear, levelFail, bgmEnabled, soundEnabled, showTutorial) {
        if (!bgmEnabled || !soundEnabled) { BgmManager.stop(); return@LaunchedEffect }
        val shouldPause = paused || gameOver || levelClear || levelFail || showResumeDialog || showTutorial
        if (shouldPause) BgmManager.pause() else BgmManager.resume()
    }

    // 切后台 / 来电 自动暂停 + 保存快照，回前台保持暂停等手动继续（教学期间不自动切暂停，避免与引导叠加）
    DisposableEffect(lifecycleOwner, autoPauseOnBg) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (showTutorial) { BgmManager.pause() }
                    else if (autoPauseOnBg && engine.gameState == GameState.PLAYING && !gameOver && !levelClear && !levelFail && !showResumeDialog) {
                        paused = true
                        engine.togglePause()
                        BgmManager.pause()
                    } else {
                        BgmManager.pause()
                    }
                    // 只要还在对局就保存快照，防止被系统回收丢进度
                    if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED) {
                        val json = try { GameSnapshot.encode(engine, mode, levelId) } catch (_: Exception) { null }
                        if (json != null) {
                            // 用 IO 协程保存，避免阻塞主线程
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { saveManager.saveSnapshot(json) }
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // 周期性保存（每约 3 秒），兜底崩溃/杀进程（教学期间暂停不保存）
    LaunchedEffect(tickCount) {
        if (tickCount != 0L && tickCount % 6 == 0L && engine.gameState == GameState.PLAYING && !paused && !gameOver && !levelClear && !levelFail && !showResumeDialog && !showTutorial) {
            scope.launch(Dispatchers.IO) {
                try { saveManager.saveSnapshot(GameSnapshot.encode(engine, mode, levelId)) } catch (_: Exception) {}
            }
        }
    }

    // 终局后清除快照，避免下次误恢复
    LaunchedEffect(gameOver, levelClear, levelFail) {
        if (gameOver || levelClear || levelFail) {
            scope.launch(Dispatchers.IO) { saveManager.clearSnapshot() }
        }
    }

    LaunchedEffect(engine, restartKey) {
        tickCount = 0L
        while (isActive) {
            if (paused || gameOver || levelClear || levelFail || showResumeDialog || showTutorial) {
                delay(100)
                continue
            }
            delay(engine.fallInterval())
            if (engine.gameState == GameState.PLAYING) {
                if (!engine.softDrop()) {
                    val prevSeq = engine.clearSeq
                    engine.hardDrop()
                    if (engine.clearSeq == prevSeq) {
                        if (soundEnabled) SoundManager.playLock()
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    when (engine.gameState) {
                        GameState.GAME_OVER -> {
                            gameOver = true
                            if (mode == GameMode.ENDLESS) {
                                launch(Dispatchers.IO) { saveManager.updateEndlessResult(engine.score, engine.combo, engine.linesCleared) }
                            }
                        }
                        GameState.LEVEL_FAIL -> levelFail = true
                        GameState.LEVEL_CLEAR -> levelClear = true
                        else -> {}
                    }
                }
                tickCount++
            } else {
                when (engine.gameState) {
                    GameState.LEVEL_FAIL -> levelFail = true
                    GameState.LEVEL_CLEAR -> levelClear = true
                    GameState.GAME_OVER -> {
                        gameOver = true
                        if (mode == GameMode.ENDLESS) {
                            launch(Dispatchers.IO) { saveManager.updateEndlessResult(engine.score, engine.combo, engine.linesCleared) }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    LaunchedEffect(levelClear) {
        if (mode == GameMode.ADVENTURE && levelClear) {
            val s = calculateStars(engine, levelConfig)
            stars = s
            val coinsReward = 20 + s * 10
            withContext(Dispatchers.IO) { saveManager.completeAdventureLevel(levelId, s, coinsReward) }
        }
    }

    LaunchedEffect(engine.clearSeq) {
        if (engine.clearSeq == 0L) return@LaunchedEffect
        val ev = engine.lastClearEvent ?: return@LaunchedEffect
        if (soundEnabled) SoundManager.playClear(ev.lines, ev.combo, ev.isTetris)
        if (hapticEnabled) {
            when {
                ev.isTetris -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ev.lines >= 2 -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        bannerText = when {
            ev.isTetris && ev.isCombo -> "TETRIS x${ev.combo}!"
            ev.isTetris -> "TETRIS!"
            ev.lines == 3 && ev.isCombo -> "TRIPLE x${ev.combo}!"
            ev.lines == 3 -> "TRIPLE!"
            ev.lines == 2 && ev.isCombo -> "DOUBLE x${ev.combo}!"
            ev.lines == 2 -> "DOUBLE!"
            ev.isCombo -> "COMBO x${ev.combo}!"
            else -> "CLEAR!"
        }
        val pts = when (ev.lines) { 1 -> 100; 2 -> 300; 3 -> 500; else -> 800 } * engine.level.coerceAtLeast(1) + if (ev.combo > 1) (ev.combo - 1) * 50 else 0
        scorePop = pts
        scorePopKey++
        bannerKey++
        showBanner = true
        scope.launch {
            val amp = when { ev.isTetris -> 14f; ev.lines == 3 -> 10f; ev.lines == 2 -> 7f; else -> 4f }
            shakeX.snapTo(0f)
            shakeX.animateTo(amp, tween(45))
            shakeX.animateTo(-amp * 0.72f, tween(60))
            shakeX.animateTo(amp * 0.38f, tween(68))
            shakeX.animateTo(0f, tween(78))
        }
    }
    LaunchedEffect(bannerKey) {
        if (bannerKey == 0) return@LaunchedEffect
        delay(920)
        showBanner = false
    }
    LaunchedEffect(gameOver) { if (gameOver && soundEnabled) SoundManager.playGameOver() }
    LaunchedEffect(levelClear) { if (levelClear && soundEnabled) SoundManager.playLevelClear() }

    Box(modifier = Modifier.fillMaxSize()) {
        ParallaxBackground(
            theme = theme,
            levelId = if (mode == GameMode.ADVENTURE) levelId else null,
            driftEnabled = true
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).height(12.dp))
            GameTopBar(
                mode = mode,
                levelId = levelId,
                score = engine.score,
                level = engine.level,
                lines = engine.linesCleared,
                combo = engine.combo,
                paused = paused,
                soundEnabled = soundEnabled,
                onToggleSound = { scope.launch { saveManager.setSoundEnabled(!soundEnabled) } },
                onPause = {
                    if (showResumeDialog) return@GameTopBar
                    paused = !paused; engine.togglePause()
                    if (paused) BgmManager.pause() else BgmManager.resume()
                    if (soundEnabled) SoundManager.playPause()
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onExit = {
                    // 手动退出：保存进度以便下次继续（若还在对局）
                    if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED) {
                        scope.launch(Dispatchers.IO) {
                            try { saveManager.saveSnapshot(GameSnapshot.encode(engine, mode, levelId)) } catch (_: Exception) {}
                        }
                    }
                    BgmManager.stop()
                    onExit()
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .weight(2f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                        .graphicsLayer(translationX = shakeX.value)
                ) {
                    BoardView(
                        engine = engine,
                        tick = tickCount,
                        ghostEnabled = ghostEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScorePanel(score = engine.score, linesCleared = engine.linesCleared, level = engine.level)
                    if (mode == GameMode.ADVENTURE) {
                        AdventureGoalPanel(linesCleared = engine.linesCleared, moveCount = engine.moveCount, config = levelConfig)
                    }
                    NextQueueSection(next1 = engine.nextQueue.getOrNull(0), next2 = engine.nextQueue.getOrNull(1))
                    if (engine.combo > 1) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = theme.primaryAccentStrong.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, theme.primaryAccentStrong.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "COMBO x${engine.combo}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.primaryAccentStrong,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }

            androidx.compose.runtime.key(restartKey) {
                ControlArea(
                    hapticEnabled = hapticEnabled,
                    enabled = !showTutorial,
                    sensitivity = controlSensitivity,
                    btnScale = controlBtnScale,
                    btnSpacing = controlBtnSpacing,
                    onMoveLeft = {
                        if (showResumeDialog || showTutorial) return@ControlArea
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.movePiece(-1, 0)) { if (soundEnabled) SoundManager.playMove() }
                            tickCount++
                        }
                    },
                    onMoveRight = {
                        if (showResumeDialog || showTutorial) return@ControlArea
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.movePiece(1, 0)) { if (soundEnabled) SoundManager.playMove() }
                            tickCount++
                        }
                    },
                    onRotate = {
                        if (showResumeDialog || showTutorial) return@ControlArea
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.rotatePiece()) { if (soundEnabled) SoundManager.playRotate() }
                            tickCount++
                        }
                    },
                    onSoftDrop = {
                        if (showResumeDialog || showTutorial) return@ControlArea
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.softDrop()) { if (soundEnabled) SoundManager.playSoftDrop() }
                            tickCount++
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showBanner && bannerText != null,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.86f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center).padding(bottom = 88.dp)
        ) {
            val isTetris = (bannerText ?: "").contains("TETRIS")
            if (isTetris) {
                TetrisBannerFull(bannerText ?: "")
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, theme.primaryAccentStrong.copy(alpha = 0.55f)),
                    shadowElevation = 14.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(theme.primaryAccentStrong, theme.secondaryAccent)))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = bannerText ?: "", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.2.sp)
                    }
                }
            }
        }
        key(scorePopKey) {
            if (scorePop != null && showBanner) {
                val popAnim = remember { Animatable(0f) }
                LaunchedEffect(scorePopKey) {
                    popAnim.snapTo(0f)
                    popAnim.animateTo(1f, tween(680))
                }
                val offsetY = -popAnim.value * 44f
                val alpha = (1f - popAnim.value).coerceIn(0f, 1f)
                Text(
                    text = "+$scorePop",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.secondaryAccent.copy(alpha = alpha),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 148.dp)
                        .graphicsLayer(translationY = offsetY, alpha = alpha)
                )
            }
        }

        // 断点续玩弹窗
        if (showResumeDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = theme.boardCardBackground,
                    border = BorderStroke(1.dp, theme.boardBorderGlow.copy(alpha = 0.6f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("发现未完成的对局", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary, textAlign = TextAlign.Center)
                        Text("是否从上次中断处继续？", fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    // 放弃进度，重新开始
                                    scope.launch(Dispatchers.IO) { saveManager.clearSnapshot() }
                                    showResumeDialog = false
                                    // 确保处于可玩状态
                                    if (engine.gameState == GameState.PAUSED) {
                                        paused = false
                                        engine.togglePause()
                                        BgmManager.resume()
                                    } else {
                                        paused = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("重新开始", color = theme.textPrimary) }
                            Button(
                                onClick = {
                                    val json = snapshotJson ?: return@Button
                                    val ok = try { GameSnapshot.decode(json, engine) } catch (_: Exception) { false }
                                    if (ok) {
                                        tickCount++
                                        showResumeDialog = false
                                        // 保持暂停，让玩家手动点继续
                                        if (engine.gameState != GameState.PAUSED) {
                                            // 解码后若是 PLAYING，自动切暂停等待手动继续
                                            if (engine.gameState == GameState.PLAYING) {
                                                engine.togglePause()
                                                paused = true
                                            }
                                        } else {
                                            paused = true
                                        }
                                    } else {
                                        scope.launch(Dispatchers.IO) { saveManager.clearSnapshot() }
                                        showResumeDialog = false
                                        paused = false
                                        if (engine.gameState == GameState.PAUSED) engine.togglePause()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong)
                            ) { Text("继续游戏", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        // 试玩用完的终局：立即引导激活（试玩期内为普通游戏结束）
        if (gameOver) {
            val trialExhausted = !isLicensed && trialRemainBefore <= 1
            OverlayDialog(
                title = if (trialExhausted) "试玩已结束" else if (mode == GameMode.ENDLESS) "游戏结束" else "关卡失败",
                subTitle = if (trialExhausted) "已用完 ${SaveManager.TRIAL_LIMIT} 局试玩 · 得分 ${engine.score} · 激活后畅玩无限制" else "得分 ${engine.score} · 消除 ${engine.linesCleared} 行",
                buttonText = if (trialExhausted) "去激活" else "重新开始",
                secondaryText = if (trialExhausted) "返回首页" else null,
                onAction = {
                    if (trialExhausted) {
                        BgmManager.stop()
                        onRequireLicense()
                    } else {
                        scope.launch(Dispatchers.IO) { saveManager.clearSnapshot() }
                        paused = false; gameOver = false; levelFail = false; levelClear = false; tickCount = 0L; restartKey++; hasCheckedResume = false; showResumeDialog = false
                    }
                },
                onSecondary = if (trialExhausted) { { BgmManager.stop(); onExit() } } else null
            )
        }
        if (paused && !showResumeDialog && !showTutorial) {
            OverlayDialog(title = "已暂停", subTitle = "切后台已自动暂停", buttonText = "继续", onAction = { paused = false; engine.togglePause(); BgmManager.resume() })
        }
        if (levelClear) {
            OverlayDialog(title = "关卡完成!", subTitle = "获得 $stars 星 · 得分 ${engine.score}", buttonText = "继续闯关", onAction = { BgmManager.stop(); onExit() })
        }
        if (levelFail) {
            OverlayDialog(title = "步数用尽", subTitle = "已达步数限制，再试一次吧", buttonText = "返回关卡", onAction = { BgmManager.stop(); onExit() })
        }

        if (showTutorial) {
            TutorialOverlay(
                onDone = {
                    scope.launch(Dispatchers.IO) { saveManager.setTutorialDone(true) }
                    showTutorial = false
                    paused = false
                    engine.togglePause()
                    BgmManager.resume()
                },
                onSkip = {
                    scope.launch(Dispatchers.IO) { saveManager.setTutorialDone(true) }
                    showTutorial = false
                    paused = false
                    engine.togglePause()
                    BgmManager.resume()
                }
            )
        }
    }
}

private fun calculateStars(engine: TetrisEngine, config: LevelConfig?): Int {
    if (config == null) return 3
    if (config.moveLimit > 0) {
        val remain = config.moveLimit - engine.moveCount
        return when {
            remain >= 5 -> 3
            remain >= 0 -> 2
            else -> 1
        }
    }
    return if (engine.linesCleared >= config.goalLines + 3) 3 else 2
}

@Composable
private fun GameTopBar(
    mode: GameMode,
    levelId: Int,
    score: Int,
    level: Int,
    lines: Int,
    combo: Int,
    paused: Boolean,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = theme.boardCardBackground,
            border = BorderStroke(1.dp, theme.boardBorder),
            modifier = Modifier.size(36.dp)
        ) {
            IconButton(onClick = onExit, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = theme.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (mode == GameMode.ENDLESS) theme.primaryAccentStrong else theme.secondaryAccent))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (mode == GameMode.ENDLESS) "经典无尽" else "关卡 $levelId",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder)) {
                    Text(text = "Lv.$level", fontSize = 10.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(text = "$lines 行 · COMBO x$combo", fontSize = 11.sp, color = theme.textMuted, modifier = Modifier.padding(top = 1.dp))
        }
        val scoreScale by animateFloatAsState(targetValue = 1f, animationSpec = tween(120), label = "score")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.boardCardBackground,
            border = BorderStroke(1.dp, theme.boardBorder),
            modifier = Modifier.graphicsLayer(scaleX = scoreScale, scaleY = scoreScale)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
                Text(text = "SCORE", fontSize = 8.sp, color = theme.textMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text(text = "$score", fontSize = 17.sp, fontWeight = FontWeight.Black, color = theme.textPrimary, letterSpacing = 0.3.sp)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = CircleShape, color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder), modifier = Modifier.size(36.dp)) {
            IconButton(onClick = onToggleSound, modifier = Modifier.fillMaxSize()) {
                Icon(if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = "音效", tint = theme.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = CircleShape, color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder), modifier = Modifier.size(36.dp)) {
            IconButton(onClick = onPause, modifier = Modifier.fillMaxSize()) {
                Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = "暂停", tint = theme.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SidePanel(title: String, content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (title.isNotEmpty()) {
            Text(text = title, fontSize = 9.sp, color = theme.textMuted, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.boardCardBackground,
            border = BorderStroke(1.dp, theme.boardBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().height(if (title.isEmpty()) 40.dp else 54.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun ScorePanel(score: Int, linesCleared: Int, level: Int) {
    val theme = LocalAppTheme.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = theme.boardCardBackground,
        border = BorderStroke(1.dp, theme.boardBorder),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "分数", fontSize = 10.sp, color = theme.textMuted, letterSpacing = 0.6.sp)
            Text(text = "$score", fontSize = 16.sp, fontWeight = FontWeight.Black, color = theme.textPrimary, modifier = Modifier.padding(top = 1.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp).height(1.dp).background(theme.boardBorder.copy(alpha = 0.7f)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "消除", fontSize = 9.sp, color = theme.textMuted)
                    Text(text = "$linesCleared", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                }
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(theme.boardBorder.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "等级", fontSize = 9.sp, color = theme.textMuted)
                    Text(text = "$level", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.secondaryAccent)
                }
            }
        }
    }
}

@Composable
private fun NextQueueSection(next1: TetrominoType?, next2: TetrominoType?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SidePanel(title = "NEXT") { MiniBoardView(next1, "next1") }
        SidePanel(title = "") { MiniBoardView(next2, "next2") }
    }
}

@Composable
private fun AdventureGoalPanel(linesCleared: Int, moveCount: Int, config: LevelConfig?) {
    val theme = LocalAppTheme.current
    if (config == null) return
    val progress = (linesCleared.toFloat() / config.goalLines.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = theme.boardCardBackground,
        border = BorderStroke(1.dp, theme.boardBorder),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "目标 ${config.goalLines} 行", fontSize = 10.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(theme.boardBackground)) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(progress)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(theme.primaryAccentStrong, theme.secondaryAccent)))
                )
            }
            Text(text = "$linesCleared / ${config.goalLines}", fontSize = 10.sp, color = theme.textMuted, modifier = Modifier.padding(top = 4.dp))
            if (config.moveLimit > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.boardBorder.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "剩余步数", fontSize = 9.sp, color = theme.textMuted)
                Text(text = "${maxOf(0, config.moveLimit - moveCount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (config.moveLimit - moveCount <= 3) theme.dangerRed else theme.textPrimary)
            }
        }
    }
}

@Composable
private fun OverlayDialog(title: String, subTitle: String? = null, buttonText: String, secondaryText: String? = null, onAction: () -> Unit, onSecondary: (() -> Unit)? = null) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.boardCardBackground,
            border = BorderStroke(1.dp, theme.boardBorderGlow.copy(alpha = 0.6f)),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(theme.primaryAccentStrong.copy(alpha = 0.9f), theme.secondaryAccent.copy(alpha = 0.9f)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = theme.textPrimary, textAlign = TextAlign.Center)
                if (subTitle != null) {
                    Text(text = subTitle, fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) { Text(text = buttonText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                if (secondaryText != null && onSecondary != null) {
                    TextButton(onClick = onSecondary, modifier = Modifier.padding(top = 6.dp)) {
                        Text(secondaryText, fontSize = 13.sp, color = theme.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlArea(enabled: Boolean = true, hapticEnabled: Boolean, sensitivity: Int = 3, btnScale: Int = 100, btnSpacing: Int = 10, onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onRotate: () -> Unit, onSoftDrop: () -> Unit) {
    ButtonControlArea(enabled = enabled, hapticEnabled = hapticEnabled, sensitivity = sensitivity, btnScale = btnScale, btnSpacing = btnSpacing, onMoveLeft = onMoveLeft, onMoveRight = onMoveRight, onRotate = onRotate, onSoftDrop = onSoftDrop)
}

private fun moveRepeatMs(s: Int): Long = when (s.coerceIn(1, 5)) { 1 -> 95L; 2 -> 80L; 3 -> 70L; 4 -> 52L; 5 -> 38L; else -> 70L }
private fun moveInitialMs(s: Int): Long = when (s.coerceIn(1, 5)) { 1 -> 270L; 2 -> 240L; 3 -> 220L; 4 -> 190L; 5 -> 160L; else -> 220L }
private fun dropRepeatMs(s: Int): Long = when (s.coerceIn(1, 5)) { 1 -> 38L; 2 -> 30L; 3 -> 24L; 4 -> 18L; 5 -> 13L; else -> 24L }
private fun dropInitialMs(s: Int): Long = when (s.coerceIn(1, 5)) { 1 -> 165L; 2 -> 140L; 3 -> 120L; 4 -> 95L; 5 -> 75L; else -> 120L }

@Composable
private fun ButtonControlArea(enabled: Boolean = true, hapticEnabled: Boolean, sensitivity: Int = 3, btnScale: Int = 100, btnSpacing: Int = 10, onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onRotate: () -> Unit, onSoftDrop: () -> Unit) {
    val theme = LocalAppTheme.current
    val btnDp = (68f * (btnScale / 100f)).dp
    val sp = btnSpacing.dp
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.boardCardBackground.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, theme.boardBorder.copy(alpha = 0.7f)),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(sp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoldMoveButton(hapticEnabled = hapticEnabled, sensitivity = sensitivity, btnSize = btnDp, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDesc = "左移", onMove = onMoveLeft)
            HoldMoveButton(hapticEnabled = hapticEnabled, sensitivity = sensitivity, btnSize = btnDp, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDesc = "右移", onMove = onMoveRight)
            RotateButton(hapticEnabled = hapticEnabled, btnSize = btnDp, onRotate = onRotate)
            HoldSoftDropButton(hapticEnabled = hapticEnabled, sensitivity = sensitivity, btnSize = btnDp, onSoftDrop = onSoftDrop)
        }
    }
}

@Composable
private fun RotateButton(hapticEnabled: Boolean, btnSize: androidx.compose.ui.unit.Dp = 68.dp, onRotate: () -> Unit) {
    val theme = LocalAppTheme.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "rotScale")
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) theme.controlButtonBgPressed else theme.controlButtonBg,
        border = BorderStroke(1.dp, theme.controlButtonBorder),
        shadowElevation = if (pressed) 2.dp else 6.dp,
        modifier = Modifier.size(btnSize).graphicsLayer(scaleX = scale, scaleY = scale).clickable {
            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pressed = true
            onRotate()
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Filled.Refresh, contentDescription = "旋转", tint = theme.textPrimary, modifier = Modifier.size(26.dp))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
}

@Composable
private fun HoldMoveButton(hapticEnabled: Boolean, sensitivity: Int = 3, btnSize: androidx.compose.ui.unit.Dp = 68.dp, icon: ImageVector, contentDesc: String, onMove: () -> Unit) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onMove)
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "mvScale")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) theme.controlButtonBgPressed else theme.controlButtonBg,
        border = BorderStroke(1.dp, theme.controlButtonBorder),
        shadowElevation = if (pressed) 2.dp else 8.dp,
        modifier = Modifier.size(btnSize).graphicsLayer(scaleX = scale, scaleY = scale).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latest()
                    val earlyUp = withTimeoutOrNull(moveInitialMs(sensitivity)) { waitForUpOrCancellation() }
                    if (earlyUp != null) { pressed = false; continue }
                    while (true) {
                        latest()
                        val up = withTimeoutOrNull(moveRepeatMs(sensitivity)) { waitForUpOrCancellation() }
                        if (up != null) { pressed = false; break }
                    }
                }
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDesc, tint = theme.textPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun HoldSoftDropButton(hapticEnabled: Boolean, sensitivity: Int = 3, btnSize: androidx.compose.ui.unit.Dp = 68.dp, onSoftDrop: () -> Unit) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onSoftDrop)
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "dropScale")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) theme.controlPrimaryBgPressed else theme.controlPrimaryBg,
        border = BorderStroke(1.dp, theme.primaryAccentStrong.copy(alpha = 0.5f)),
        shadowElevation = if (pressed) 2.dp else 8.dp,
        modifier = Modifier.size(btnSize).graphicsLayer(scaleX = scale, scaleY = scale).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latest()
                    val earlyUp = withTimeoutOrNull(dropInitialMs(sensitivity)) { waitForUpOrCancellation() }
                    if (earlyUp != null) { pressed = false; continue }
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    while (true) {
                        latest()
                        val up = withTimeoutOrNull(dropRepeatMs(sensitivity)) { waitForUpOrCancellation() }
                        if (up != null) { pressed = false; break }
                    }
                }
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "加速", tint = Color.White, modifier = Modifier.size(26.dp))
                Text(text = "加速", fontSize = 9.sp, color = Color.White.copy(alpha = 0.95f), fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
private fun TetrisBannerFull(text: String) {
    // 全屏横幅发光：金紫渐变底 + 外发光扫光 + 弹跳入场
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(text) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(780))
    }
    val glowAlpha = 0.92f + sweep.value * 0.08f
    Box(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .shadow(22.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFFD54F), Color(0xFFFF8A3D), Color(0xFFB388FF), Color(0xFF7C3AED))
                )
            )
            .padding(1.4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1A1033).copy(alpha = glowAlpha),
            border = BorderStroke(1.6.dp, Color(0xFFFFD54F).copy(alpha = 0.95f)),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFB74D).copy(alpha = 0.98f),
                                Color(0xFFFF6B6B).copy(alpha = 0.96f),
                                Color(0xFF7C3AED).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                // 扫光高光
                val w = 160f
                val x = -w + sweep.value * (1080f + w)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .graphicsLayer(translationX = x - 540f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(w.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent)))
                    )
                }
                Text(
                    text = text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.6.sp,
                    modifier = Modifier.graphicsLayer(scaleX = 1f + sweep.value * 0.02f, scaleY = 1f + sweep.value * 0.02f)
                )
            }
        }
        // 外发光晕
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFFD54F).copy(alpha = 0.75f * (1f - sweep.value * 0.4f)), Color.Transparent)))
        )
    }
}
