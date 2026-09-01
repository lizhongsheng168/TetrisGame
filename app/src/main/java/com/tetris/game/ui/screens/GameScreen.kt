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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.tetris.game.audio.SoundManager
import com.tetris.game.data.SaveManager
import com.tetris.game.engine.*
import com.tetris.game.ui.components.BoardView
import com.tetris.game.ui.components.MiniBoardView
import com.tetris.game.ui.theme.*
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
    onExit: () -> Unit
) {
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
    var soundEnabled by remember { mutableStateOf(true) }

    // Banner / score pop / shake
    var bannerText by remember { mutableStateOf<String?>(null) }
    var bannerKey by remember { mutableIntStateOf(0) }
    var showBanner by remember { mutableStateOf(false) }
    var scorePop by remember { mutableStateOf<Int?>(null) }
    var scorePopKey by remember { mutableIntStateOf(0) }
    val shakeX = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(soundEnabled) { SoundManager.enabled = soundEnabled }

    // 单协程游戏循环
    LaunchedEffect(engine, restartKey) {
        tickCount = 0L
        while (isActive) {
            if (paused || gameOver || levelClear || levelFail) {
                delay(100)
                continue
            }
            delay(engine.fallInterval())
            if (engine.gameState == GameState.PLAYING) {
                if (!engine.softDrop()) {
                    val prevSeq = engine.clearSeq
                    engine.hardDrop()
                    if (engine.clearSeq == prevSeq) {
                        // 未消除，仅锁定
                        if (SoundManager.enabled) SoundManager.playLock()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

    // 消除事件：音效 + 横幅 + 分数飘字 + 震屏
    LaunchedEffect(engine.clearSeq) {
        if (engine.clearSeq == 0L) return@LaunchedEffect
        val ev = engine.lastClearEvent ?: return@LaunchedEffect
        if (soundEnabled) SoundManager.playClear(ev.lines, ev.combo, ev.isTetris)
        // 震动
        when {
            ev.isTetris -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            ev.lines >= 2 -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        // 震屏动画
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameBackgroundTop, GameBackgroundBottom)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryAccent.copy(alpha = 0.08f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(18.dp))
            GameTopBar(
                mode = mode,
                levelId = levelId,
                score = engine.score,
                level = engine.level,
                lines = engine.linesCleared,
                combo = engine.combo,
                paused = paused,
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = !soundEnabled },
                onPause = { paused = !paused; engine.togglePause(); if (soundEnabled) SoundManager.playPause() },
                onExit = onExit
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .weight(2f)
            ) {
                // 棋盘（仅棋盘做震屏）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                        .graphicsLayer(translationX = shakeX.value)
                ) {
                    BoardView(
                        engine = engine,
                        tick = tickCount,
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
                            color = PrimaryAccentStrong.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PrimaryAccentStrong.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "COMBO x${engine.combo}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryAccentStrong,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }

            androidx.compose.runtime.key(restartKey) {
                ControlArea(
                    onMoveLeft = {
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.movePiece(-1, 0)) { if (soundEnabled) SoundManager.playMove() }
                            tickCount++
                        }
                    },
                    onMoveRight = {
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.movePiece(1, 0)) { if (soundEnabled) SoundManager.playMove() }
                            tickCount++
                        }
                    },
                    onRotate = {
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.rotatePiece()) { if (soundEnabled) SoundManager.playRotate() }
                            tickCount++
                        }
                    },
                    onSoftDrop = {
                        if (engine.gameState == GameState.PLAYING) {
                            if (engine.softDrop()) { if (soundEnabled) SoundManager.playSoftDrop() }
                            tickCount++
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 消除横幅与分数飘字：覆盖在棋盘中央，不参与 Row 布局以避免 AnimatedVisibility 的 RowScope 限制
        androidx.compose.animation.AnimatedVisibility(
            visible = showBanner && bannerText != null,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.86f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center).padding(bottom = 88.dp)
        ) {
            val isTetris = (bannerText ?: "").contains("TETRIS")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isTetris) Color(0xFF7C3AED).copy(alpha = 0.96f) else Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, if (isTetris) Color(0xFFFFD54F) else PrimaryAccentStrong.copy(alpha = 0.55f)),
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                if (isTetris) listOf(Color(0xFFFFB74D), Color(0xFFFF6B6B), Color(0xFF7C3AED))
                                else listOf(PrimaryAccentStrong, SecondaryAccent)
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bannerText ?: "",
                        fontSize = if (isTetris) 20.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
        // 分数飘字：横幅上方淡出上移
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
                    color = SecondaryAccent.copy(alpha = alpha),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 148.dp)
                        .graphicsLayer(translationY = offsetY, alpha = alpha)
                )
            }
        }

        if (paused) {
            OverlayDialog(title = "已暂停", buttonText = "继续", onAction = { paused = false; engine.togglePause() })
        }
        if (gameOver) {
            OverlayDialog(
                title = if (mode == GameMode.ENDLESS) "游戏结束" else "关卡失败",
                subTitle = "得分 ${engine.score} · 消除 ${engine.linesCleared} 行",
                buttonText = "重新开始",
                onAction = { paused = false; gameOver = false; levelFail = false; levelClear = false; tickCount = 0L; restartKey++ }
            )
        }
        if (levelClear) {
            OverlayDialog(title = "关卡完成!", subTitle = "获得 $stars 星 · 得分 ${engine.score}", buttonText = "继续闯关", onAction = onExit)
        }
        if (levelFail) {
            OverlayDialog(title = "步数用尽", subTitle = "已达步数限制，再试一次吧", buttonText = "返回关卡", onAction = onExit)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BoardCardBackground,
            border = BorderStroke(1.dp, BoardBorder),
            modifier = Modifier.size(36.dp)
        ) {
            IconButton(onClick = onExit, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (mode == GameMode.ENDLESS) PrimaryAccentStrong else SecondaryAccent))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (mode == GameMode.ENDLESS) "经典无尽" else "关卡 $levelId",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = BoardCardBackground, border = BorderStroke(1.dp, BoardBorder)) {
                    Text(text = "Lv.$level", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(text = "$lines 行 · COMBO x$combo", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 1.dp))
        }
        val scoreScale by animateFloatAsState(targetValue = 1f, animationSpec = tween(120), label = "score")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BoardCardBackground,
            border = BorderStroke(1.dp, BoardBorder),
            modifier = Modifier.graphicsLayer(scaleX = scoreScale, scaleY = scoreScale)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
                Text(text = "SCORE", fontSize = 8.sp, color = TextMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text(text = "$score", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 0.3.sp)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = CircleShape, color = BoardCardBackground, border = BorderStroke(1.dp, BoardBorder), modifier = Modifier.size(36.dp)) {
            IconButton(onClick = onToggleSound, modifier = Modifier.fillMaxSize()) {
                Icon(if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = "音效", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = CircleShape, color = BoardCardBackground, border = BorderStroke(1.dp, BoardBorder), modifier = Modifier.size(36.dp)) {
            IconButton(onClick = onPause, modifier = Modifier.fillMaxSize()) {
                Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = "暂停", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SidePanel(title: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (title.isNotEmpty()) {
            Text(text = title, fontSize = 9.sp, color = TextMuted, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BoardCardBackground,
            border = BorderStroke(1.dp, BoardBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().height(if (title.isEmpty()) 40.dp else 54.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun ScorePanel(score: Int, linesCleared: Int, level: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BoardCardBackground,
        border = BorderStroke(1.dp, BoardBorder),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "分数", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.6.sp)
            Text(text = "$score", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.padding(top = 1.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp).height(1.dp).background(BoardBorder.copy(alpha = 0.7f)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "消除", fontSize = 9.sp, color = TextMuted)
                    Text(text = "$linesCleared", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(BoardBorder.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "等级", fontSize = 9.sp, color = TextMuted)
                    Text(text = "$level", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryAccent)
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
    if (config == null) return
    val progress = (linesCleared.toFloat() / config.goalLines.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BoardCardBackground,
        border = BorderStroke(1.dp, BoardBorder),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "目标 ${config.goalLines} 行", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(BoardBackground)) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(progress)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryAccentStrong, SecondaryAccent)))
                )
            }
            Text(text = "$linesCleared / ${config.goalLines}", fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
            if (config.moveLimit > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BoardBorder.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "剩余步数", fontSize = 9.sp, color = TextMuted)
                Text(text = "${maxOf(0, config.moveLimit - moveCount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (config.moveLimit - moveCount <= 3) DangerRed else TextPrimary)
            }
        }
    }
}

@Composable
private fun OverlayDialog(title: String, subTitle: String? = null, buttonText: String, onAction: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BoardCardBackground,
            border = BorderStroke(1.dp, BoardBorderGlow.copy(alpha = 0.6f)),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PrimaryAccentStrong.copy(alpha = 0.9f), SecondaryAccent.copy(alpha = 0.9f)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary, textAlign = TextAlign.Center)
                if (subTitle != null) {
                    Text(text = subTitle, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentStrong),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) { Text(text = buttonText, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ControlArea(onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onRotate: () -> Unit, onSoftDrop: () -> Unit) {
    ButtonControlArea(onMoveLeft = onMoveLeft, onMoveRight = onMoveRight, onRotate = onRotate, onSoftDrop = onSoftDrop)
}

@Composable
private fun ButtonControlArea(onMoveLeft: () -> Unit, onMoveRight: () -> Unit, onRotate: () -> Unit, onSoftDrop: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BoardCardBackground.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, BoardBorder.copy(alpha = 0.7f)),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoldMoveButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDesc = "左移", onMove = onMoveLeft)
            HoldMoveButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDesc = "右移", onMove = onMoveRight)
            RotateButton(onRotate)
            HoldSoftDropButton(onSoftDrop = onSoftDrop)
        }
    }
}

@Composable
private fun RotateButton(onRotate: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "rotScale")
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) ControlButtonBgPressed else ControlButtonBg,
        border = BorderStroke(1.dp, ControlButtonBorder),
        shadowElevation = if (pressed) 2.dp else 6.dp,
        modifier = Modifier.size(68.dp).graphicsLayer(scaleX = scale, scaleY = scale).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pressed = true
            onRotate()
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Filled.Refresh, contentDescription = "旋转", tint = TextPrimary, modifier = Modifier.size(26.dp))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
}

@Composable
private fun HoldMoveButton(icon: ImageVector, contentDesc: String, onMove: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onMove)
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "mvScale")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) ControlButtonBgPressed else ControlButtonBg,
        border = BorderStroke(1.dp, ControlButtonBorder),
        shadowElevation = if (pressed) 2.dp else 8.dp,
        modifier = Modifier.size(68.dp).graphicsLayer(scaleX = scale, scaleY = scale).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latest()
                    val earlyUp = withTimeoutOrNull(220) { waitForUpOrCancellation() }
                    if (earlyUp != null) { pressed = false; continue }
                    while (true) {
                        latest()
                        val up = withTimeoutOrNull(70) { waitForUpOrCancellation() }
                        if (up != null) { pressed = false; break }
                    }
                }
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDesc, tint = TextPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun HoldSoftDropButton(onSoftDrop: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onSoftDrop)
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "dropScale")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pressed) ControlPrimaryBgPressed else ControlPrimaryBg,
        border = BorderStroke(1.dp, PrimaryAccentStrong.copy(alpha = 0.5f)),
        shadowElevation = if (pressed) 2.dp else 8.dp,
        modifier = Modifier.size(68.dp).graphicsLayer(scaleX = scale, scaleY = scale).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latest()
                    val earlyUp = withTimeoutOrNull(120) { waitForUpOrCancellation() }
                    if (earlyUp != null) { pressed = false; continue }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    while (true) {
                        latest()
                        val up = withTimeoutOrNull(36) { waitForUpOrCancellation() }
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
