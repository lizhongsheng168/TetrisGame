package com.tetris.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.audio.BgmLibrary
import com.tetris.game.audio.BgmManager
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.components.ParallaxBackground
import com.tetris.game.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    saveManager: SaveManager,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val soundEnabled by saveManager.soundEnabled.collectAsState(initial = true)
    val bgmEnabled by saveManager.bgmEnabled.collectAsState(initial = true)
    val hapticEnabled by saveManager.hapticEnabled.collectAsState(initial = true)
    val ghostEnabled by saveManager.ghostEnabled.collectAsState(initial = true)
    val autoPauseOnBg by saveManager.autoPauseOnBg.collectAsState(initial = true)
    val bgmTrack by saveManager.bgmTrack.collectAsState(initial = "energy")
    val sensitivity by saveManager.controlSensitivity.collectAsState(initial = 3)
    val btnScale by saveManager.controlBtnScale.collectAsState(initial = 100)
    val btnSpacing by saveManager.controlBtnSpacing.collectAsState(initial = 10)

    Box(modifier = Modifier.fillMaxSize()) {
        ParallaxBackground(theme = theme, driftEnabled = true)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 12.dp).padding(top = 12.dp).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder), modifier = Modifier.size(36.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = theme.textPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.CenterStart) {
                    Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("声音与震动", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

                SettingRow(
                    icon = Icons.Filled.VolumeUp,
                    title = "音效",
                    subtitle = "总开关 · 关闭后静音所有音效与背景音乐",
                    checked = soundEnabled,
                    onChecked = { scope.launch { saveManager.setSoundEnabled(it) } }
                )
                SettingRow(
                    icon = Icons.Filled.MusicNote,
                    title = "背景音乐",
                    subtitle = if (soundEnabled) "曲库循环 · 动感好听 · 随选即播" else "已随总开关静音",
                    checked = bgmEnabled && soundEnabled,
                    enabled = soundEnabled,
                    onChecked = { scope.launch { saveManager.setBgmEnabled(it) } }
                )
                // BGM 曲库：动感好听 6 选1，随选即切
                Surface(shape = RoundedCornerShape(16.dp), color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder), shadowElevation = 4.dp) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(theme.primaryAccentStrong.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = theme.primaryAccentStrong, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("曲库选曲", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                                Text("6 首动感循环 · 点选试听 · 自动保存", fontSize = 11.sp, color = theme.textSecondary)
                            }
                            if (!soundEnabled || !bgmEnabled) Text("已静音", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textMuted)
                        }
                        val canPick = soundEnabled
                        BgmLibrary.tracks.forEach { track ->
                            val selected = bgmTrack == track.id
                            val alpha = if (canPick) 1f else 0.52f
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) theme.primaryAccentStrong.copy(alpha = 0.14f) else theme.boardBackground.copy(alpha = 0.92f * alpha),
                                border = BorderStroke(1.dp, if (selected) theme.primaryAccentStrong.copy(alpha = 0.95f) else theme.boardBorder.copy(alpha = 0.6f * alpha)),
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) theme.primaryAccentStrong.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
                                        .let { m -> if (canPick) m.then(Modifier) else m }.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(
                                            if (selected) theme.primaryAccentStrong else theme.boardCardBackground
                                        ).let { m ->
                                            if (canPick) m.then(Modifier) else m
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (selected && bgmEnabled && soundEnabled) Icons.Filled.Equalizer else Icons.Filled.PlayArrow,
                                            contentDescription = "选曲 ${track.name}",
                                            tint = if (selected) androidx.compose.ui.graphics.Color.White else theme.textPrimary.copy(alpha = alpha),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(track.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary.copy(alpha = alpha))
                                            Spacer(Modifier.width(6.dp))
                                            Surface(shape = RoundedCornerShape(99.dp), color = theme.boardBorder.copy(alpha = 0.9f)) {
                                                Text("${track.bpm} BPM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            if (selected) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(99.dp), color = theme.primaryAccentStrong) {
                                                    Text("播放中", fontSize = 9.sp, fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(track.subtitle, fontSize = 11.sp, color = theme.textSecondary.copy(alpha = alpha), modifier = Modifier.padding(top = 2.dp))
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch { saveManager.setBgmTrack(track.id) }
                                            if (soundEnabled && bgmEnabled) BgmManager.start(track.id)
                                        },
                                        enabled = canPick,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) theme.primaryAccentStrong else theme.boardCardBackground,
                                            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else theme.textPrimary,
                                            disabledContainerColor = theme.boardBackground,
                                            disabledContentColor = theme.textMuted
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = if (!selected) BorderStroke(1.dp, theme.boardBorder) else null
                                    ) { Text(if (selected) "已选" else "选用", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        Text("提示：选曲后大厅与对局自动换轨并循环，无需重启。经典重燃为俄罗斯方块主题·动感重编。", fontSize = 11.sp, color = theme.textMuted, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                    }
                }
                SettingRow(
                    icon = Icons.Filled.Vibration,
                    title = "震动反馈",
                    subtitle = "消除、TETRIS 与长按的触感",
                    checked = hapticEnabled,
                    onChecked = { scope.launch { saveManager.setHapticEnabled(it) } }
                )

                Spacer(Modifier.height(4.dp))
                Text("游戏辅助", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))

                SettingRow(
                    icon = Icons.Filled.Visibility,
                    title = "幽灵块",
                    subtitle = "显示方块落点虚影，辅助预判",
                    checked = ghostEnabled,
                    onChecked = { scope.launch { saveManager.setGhostEnabled(it) } }
                )

                Spacer(Modifier.height(4.dp))
                Text("后台与通话", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))

                SettingRow(
                    icon = Icons.Filled.PauseCircle,
                    title = "切后台自动暂停",
                    subtitle = "来电 / 切后台时自动暂停，回前台继续",
                    checked = autoPauseOnBg,
                    onChecked = { scope.launch { saveManager.setAutoPauseOnBg(it) } }
                )

                Spacer(Modifier.height(4.dp))
                Text("操作手感", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))

                Surface(shape = RoundedCornerShape(16.dp), color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder), shadowElevation = 4.dp) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(theme.primaryAccentStrong.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Tune, contentDescription = null, tint = theme.primaryAccentStrong, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("灵敏度", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                                Text(when (sensitivity) { 1 -> "很慢 · 稳"; 2 -> "偏慢"; 3 -> "适中（默认）"; 4 -> "偏快"; else -> "很快 · 竞技" }, fontSize = 11.sp, color = theme.textSecondary)
                            }
                            Text("${sensitivity}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = theme.primaryAccentStrong)
                        }
                        Slider(
                            value = sensitivity.toFloat(), onValueChange = { v -> scope.launch { saveManager.setControlSensitivity(v.toInt()) } },
                            valueRange = 1f..5f, steps = 3,
                            colors = SliderDefaults.colors(activeTrackColor = theme.primaryAccentStrong, thumbColor = theme.primaryAccentStrong, inactiveTrackColor = theme.boardBackground)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("慢", fontSize = 11.sp, color = theme.textMuted); Text("快", fontSize = 11.sp, color = theme.textMuted) }
                        Text("左右长按连移与加速的反应速度，1最稳 5最快，默认3等同原来的 70ms/24ms 手感。", fontSize = 11.sp, color = theme.textSecondary, lineHeight = 15.sp)

                        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.boardBorder.copy(alpha = 0.5f)))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(theme.secondaryAccent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = theme.secondaryAccent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("按键大小", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                                Text("${btnScale}%", fontSize = 11.sp, color = theme.textSecondary)
                            }
                            Text(if (btnScale == 100) "默认" else "${btnScale}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary)
                        }
                        Slider(
                            value = btnScale.toFloat(), onValueChange = { v -> scope.launch { saveManager.setControlBtnScale(v.toInt()) } },
                            valueRange = 85f..115f, steps = 5,
                            colors = SliderDefaults.colors(activeTrackColor = theme.secondaryAccent, thumbColor = theme.secondaryAccent, inactiveTrackColor = theme.boardBackground)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(theme.primaryAccentStrong.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SpaceBar, contentDescription = null, tint = theme.primaryAccentStrong, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("按键间距", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                                Text("${btnSpacing} dp", fontSize = 11.sp, color = theme.textSecondary)
                            }
                            Text("${btnSpacing} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary)
                        }
                        Slider(
                            value = btnSpacing.toFloat(), onValueChange = { v -> scope.launch { saveManager.setControlBtnSpacing(v.toInt()) } },
                            valueRange = 6f..16f, steps = 9,
                            colors = SliderDefaults.colors(activeTrackColor = theme.primaryAccentStrong, thumbColor = theme.primaryAccentStrong, inactiveTrackColor = theme.boardBackground)
                        )

                        OutlinedButton(
                            onClick = { scope.launch { saveManager.setControlSensitivity(3); saveManager.setControlBtnScale(100); saveManager.setControlBtnSpacing(10) } },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("恢复默认", fontSize = 12.sp, color = theme.textPrimary) }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("帮助", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.boardCardBackground,
                    border = BorderStroke(1.dp, theme.boardBorder),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(theme.primaryAccentStrong.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = theme.primaryAccentStrong, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("新手引导", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                            Text("重新查看 旋转/加速/幽灵块 说明", fontSize = 11.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                        Button(
                            onClick = { scope.launch { saveManager.setTutorialDone(false) } },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("重看", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = theme.boardCardBackground, border = BorderStroke(1.dp, theme.boardBorder)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(theme.primaryAccentStrong, theme.secondaryAccent))), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("小提示", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                        }
                        Text("切后台自动暂停并静音BGM，回到前台保持暂停等你手动继续；对局进度在后台会被保存，即使被系统回收也能恢复。", fontSize = 12.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 8.dp), lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit
) {
    val theme = LocalAppTheme.current
    val alpha = if (enabled) 1f else 0.52f
    Surface(shape = RoundedCornerShape(16.dp), color = theme.boardCardBackground.copy(alpha = if (enabled) 1f else 0.72f), border = BorderStroke(1.dp, theme.boardBorder.copy(alpha = if (enabled) 1f else 0.5f)), shadowElevation = 4.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (checked) theme.primaryAccentStrong.copy(alpha = 0.18f * alpha) else theme.boardBackground), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = (if (checked) theme.primaryAccentStrong else theme.textMuted).copy(alpha = alpha), modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary.copy(alpha = alpha))
                Text(subtitle, fontSize = 11.sp, color = theme.textSecondary.copy(alpha = alpha), modifier = Modifier.padding(top = 2.dp))
            }
            Switch(
                checked = checked, onCheckedChange = onChecked, enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = theme.primaryAccentStrong,
                    checkedBorderColor = theme.primaryAccentStrong,
                    uncheckedThumbColor = theme.textMuted,
                    uncheckedTrackColor = theme.boardBackground,
                    uncheckedBorderColor = theme.boardBorder
                )
            )
        }
    }
}
