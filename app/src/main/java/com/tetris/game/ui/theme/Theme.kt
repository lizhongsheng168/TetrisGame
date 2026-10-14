package com.tetris.game.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeId(val id: String) {
    STARRY("starry"),
    CYBER("cyber"),
    CANDY("candy");

    companion object {
        fun from(id: String?): ThemeId = entries.find { it.id == id } ?: STARRY
    }
}

data class AppTheme(
    val id: ThemeId,
    val name: String,
    val subtitle: String,
    val blockColors: List<Color>,
    val gameBackground: Color,
    val gameBackgroundTop: Color,
    val gameBackgroundBottom: Color,
    val boardBackground: Color,
    val boardCardBackground: Color,
    val boardBorder: Color,
    val boardBorderGlow: Color,
    val cellEmpty: Color,
    val cellEmptyStroke: Color,
    val cellGhost: Color,
    val cellGhostBorder: Color,
    val obstacleColor: Color,
    val obstacleInner: Color,
    val primaryAccent: Color,
    val primaryAccentStrong: Color,
    val secondaryAccent: Color,
    val successGreen: Color,
    val dangerRed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val controlButtonBg: Color,
    val controlButtonBgPressed: Color,
    val controlButtonBorder: Color,
    val controlPrimaryBg: Color,
    val controlPrimaryBgPressed: Color,
)

private val StarryTheme = AppTheme(
    id = ThemeId.STARRY,
    name = "星夜深空",
    subtitle = "经典深蓝 · 幻彩方块",
    blockColors = listOf(
        Color(0xFF22D3EE), Color(0xFFFACC15), Color(0xFFC084FC),
        Color(0xFFFB923C), Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFF87171),
    ),
    gameBackground = Color(0xFF0F1B2D),
    gameBackgroundTop = Color(0xFF13233A),
    gameBackgroundBottom = Color(0xFF0A1426),
    boardBackground = Color(0xFF16263D),
    boardCardBackground = Color(0xFF1B2E4D),
    boardBorder = Color(0xFF2A3F5F),
    boardBorderGlow = Color(0xFF3A5A85),
    cellEmpty = Color(0xFF1E3352),
    cellEmptyStroke = Color(0xFF253A58),
    cellGhost = Color(0xFF3E5A80),
    cellGhostBorder = Color(0xFF5A7AAE),
    obstacleColor = Color(0xFF2F3E52),
    obstacleInner = Color(0xFF3A4E68),
    primaryAccent = Color(0xFF4DD0E1),
    primaryAccentStrong = Color(0xFF22D3EE),
    secondaryAccent = Color(0xFFFFB74D),
    successGreen = Color(0xFF81C784),
    dangerRed = Color(0xFFE57373),
    textPrimary = Color(0xFFECEFF1),
    textSecondary = Color(0xFF90A4AE),
    textMuted = Color(0xFF6B7F93),
    controlButtonBg = Color(0xFF1D3250),
    controlButtonBgPressed = Color(0xFF243A5C),
    controlButtonBorder = Color(0xFF2F4B73),
    controlPrimaryBg = Color(0xFF2ED3DD),
    controlPrimaryBgPressed = Color(0xFF1EC4D1),
)

private val CyberTheme = AppTheme(
    id = ThemeId.CYBER,
    name = "赛博都市",
    subtitle = "霓虹紫 · 电光幻影",
    blockColors = listOf(
        Color(0xFF00E5FF), Color(0xFFFFEA00), Color(0xFFD500F9),
        Color(0xFFFF6D00), Color(0xFF2979FF), Color(0xFF00E676), Color(0xFFFF1744),
    ),
    gameBackground = Color(0xFF0D0B1E),
    gameBackgroundTop = Color(0xFF1A1033),
    gameBackgroundBottom = Color(0xFF0B0A14),
    boardBackground = Color(0xFF1C1235),
    boardCardBackground = Color(0xFF251843),
    boardBorder = Color(0xFF3B2A5F),
    boardBorderGlow = Color(0xFF7C3AED),
    cellEmpty = Color(0xFF2A1D4A),
    cellEmptyStroke = Color(0xFF3A2D5E),
    cellGhost = Color(0xFF5A3FB0),
    cellGhostBorder = Color(0xFF8B6FE8),
    obstacleColor = Color(0xFF2E1F4A),
    obstacleInner = Color(0xFF3E2E5E),
    primaryAccent = Color(0xFFE040FB),
    primaryAccentStrong = Color(0xFFD500F9),
    secondaryAccent = Color(0xFF00E5FF),
    successGreen = Color(0xFF69F0AE),
    dangerRed = Color(0xFFFF5252),
    textPrimary = Color(0xFFF3E5F5),
    textSecondary = Color(0xFFB39DDB),
    textMuted = Color(0xFF7C6AA0),
    controlButtonBg = Color(0xFF2A1A4A),
    controlButtonBgPressed = Color(0xFF3A2480),
    controlButtonBorder = Color(0xFF4A2E8A),
    controlPrimaryBg = Color(0xFF7C3AED),
    controlPrimaryBgPressed = Color(0xFF6D28D9),
)

private val CandyTheme = AppTheme(
    id = ThemeId.CANDY,
    name = "糖果王国",
    subtitle = "甜蜜粉彩 · 治愈软萌",
    blockColors = listOf(
        Color(0xFF7DD3C8), // I - 薄荷青 更柔
        Color(0xFFFFD93D), // O - 蜂蜜黄
        Color(0xFFCE93D8), // T - 薰衣草紫
        Color(0xFFFF8C42), // L - 蜜桃橙
        Color(0xFF6EC1E4), // J - 天空蓝
        Color(0xFF88D498), // S - 苹果绿
        Color(0xFFFF6B9D), // Z - 草莓粉
    ),
    gameBackground = Color(0xFF1E1C2E),
    gameBackgroundTop = Color(0xFF2A2440),
    gameBackgroundBottom = Color(0xFF161424),
    boardBackground = Color(0xFF2B2545),
    boardCardBackground = Color(0xFF332E58),
    boardBorder = Color(0xFF4A3F76),
    boardBorderGlow = Color(0xFFB794F6),
    cellEmpty = Color(0xFF3A3460),
    cellEmptyStroke = Color(0xFF4B4570),
    cellGhost = Color(0xFF6B5B95),
    cellGhostBorder = Color(0xFF9B8EC4),
    obstacleColor = Color(0xFF3A2E4A),
    obstacleInner = Color(0xFF4E3E60),
    primaryAccent = Color(0xFFFF8A9D),
    primaryAccentStrong = Color(0xFFFF6B9D),
    secondaryAccent = Color(0xFFFFD93D),
    successGreen = Color(0xFF88D498),
    dangerRed = Color(0xFFFF6B6B),
    textPrimary = Color(0xFFFCE4EC),
    textSecondary = Color(0xFFCE93D8),
    textMuted = Color(0xFF9B8EC4),
    controlButtonBg = Color(0xFF332E58),
    controlButtonBgPressed = Color(0xFF40386E),
    controlButtonBorder = Color(0xFF5A4F8A),
    controlPrimaryBg = Color(0xFFFF6B9D),
    controlPrimaryBgPressed = Color(0xFFFF5277),
)

val AppThemes: List<AppTheme> = listOf(StarryTheme, CyberTheme, CandyTheme)

fun themeOf(id: ThemeId): AppTheme = when (id) {
    ThemeId.STARRY -> StarryTheme
    ThemeId.CYBER -> CyberTheme
    ThemeId.CANDY -> CandyTheme
}

fun themeOf(raw: String?): AppTheme = themeOf(ThemeId.from(raw))

val LocalAppTheme = compositionLocalOf { StarryTheme }
