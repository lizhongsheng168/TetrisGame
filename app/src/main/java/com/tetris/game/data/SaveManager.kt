package com.tetris.game.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tetris_save")

/** 本地存档：最高分、最大连击、关卡进度、金币等 */
class SaveManager(private val context: Context) {

    companion object {
        const val TRIAL_LIMIT = 3
    }

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val MAX_COMBO = intPreferencesKey("max_combo")
        val MAX_LEVEL = intPreferencesKey("max_level")
        val TOTAL_LINES = intPreferencesKey("total_lines")
        val TOTAL_GAMES = intPreferencesKey("total_games")
        val COINS = intPreferencesKey("coins")
        val GAME_COUNT = intPreferencesKey("game_count")
        val CURRENT_LEVEL = intPreferencesKey("current_level")
        val STAR_LEVELS = stringPreferencesKey("star_levels")  // "level:stars;level:stars"
        val THEME_ID = stringPreferencesKey("theme_id")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val BGM_ENABLED = booleanPreferencesKey("bgm_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val GHOST_ENABLED = booleanPreferencesKey("ghost_enabled")
        val GAME_SNAPSHOT = stringPreferencesKey("game_snapshot")
        val AUTO_PAUSE_ON_BG = booleanPreferencesKey("auto_pause_on_bg")
        val TRIAL_PLAYS = intPreferencesKey("trial_plays")
        val TUTORIAL_DONE = booleanPreferencesKey("tutorial_done")
        val CONTROL_SENSITIVITY = intPreferencesKey("control_sensitivity") // 1..5 越大越快，默认3
        val CONTROL_BTN_SCALE = intPreferencesKey("control_btn_scale") // 85..115 百分比
        val CONTROL_BTN_SPACING = intPreferencesKey("control_btn_spacing") // 6..16 dp
        val BGM_TRACK = stringPreferencesKey("bgm_track")
    }

    val highScore: Flow<Int> = context.dataStore.data.map { it[Keys.HIGH_SCORE] ?: 0 }
    val maxCombo: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_COMBO] ?: 0 }
    val maxLevel: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_LEVEL] ?: 0 }
    val totalLines: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_LINES] ?: 0 }
    val totalGames: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_GAMES] ?: 0 }
    val coins: Flow<Int> = context.dataStore.data.map { it[Keys.COINS] ?: 0 }
    val currentLevel: Flow<Int> = context.dataStore.data.map { it[Keys.CURRENT_LEVEL] ?: 1 }
    val starLevels: Flow<Map<Int, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.STAR_LEVELS] ?: ""
        parseStarLevels(raw)
    }.flowOn(Dispatchers.Default)
    val themeId: Flow<String> = context.dataStore.data.map { it[Keys.THEME_ID] ?: "starry" }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val bgmEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BGM_ENABLED] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTIC_ENABLED] ?: true }
    val ghostEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.GHOST_ENABLED] ?: true }

    suspend fun setThemeId(id: String) { context.dataStore.edit { it[Keys.THEME_ID] = id } }
    suspend fun setSoundEnabled(v: Boolean) { context.dataStore.edit { it[Keys.SOUND_ENABLED] = v } }
    suspend fun setBgmEnabled(v: Boolean) { context.dataStore.edit { it[Keys.BGM_ENABLED] = v } }
    suspend fun setHapticEnabled(v: Boolean) { context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = v } }
    suspend fun setGhostEnabled(v: Boolean) { context.dataStore.edit { it[Keys.GHOST_ENABLED] = v } }
    suspend fun setAutoPauseOnBg(v: Boolean) { context.dataStore.edit { it[Keys.AUTO_PAUSE_ON_BG] = v } }

    // 操作手感：灵敏度 + 按键布局微调
    val controlSensitivity: Flow<Int> = context.dataStore.data.map { (it[Keys.CONTROL_SENSITIVITY] ?: 3).coerceIn(1, 5) }
    val controlBtnScale: Flow<Int> = context.dataStore.data.map { (it[Keys.CONTROL_BTN_SCALE] ?: 100).coerceIn(85, 115) }
    val controlBtnSpacing: Flow<Int> = context.dataStore.data.map { (it[Keys.CONTROL_BTN_SPACING] ?: 10).coerceIn(6, 16) }
    suspend fun setControlSensitivity(v: Int) { context.dataStore.edit { it[Keys.CONTROL_SENSITIVITY] = v.coerceIn(1, 5) } }
    suspend fun setControlBtnScale(v: Int) { context.dataStore.edit { it[Keys.CONTROL_BTN_SCALE] = v.coerceIn(85, 115) } }
    suspend fun setControlBtnSpacing(v: Int) { context.dataStore.edit { it[Keys.CONTROL_BTN_SPACING] = v.coerceIn(6, 16) } }

    // BGM 曲库选曲
    val bgmTrack: Flow<String> = context.dataStore.data.map { it[Keys.BGM_TRACK] ?: "energy" }
    suspend fun setBgmTrack(id: String) { context.dataStore.edit { it[Keys.BGM_TRACK] = id } }

    // 试玩：3 局免费（未授权时）
    val trialPlays: Flow<Int> = context.dataStore.data.map { it[Keys.TRIAL_PLAYS] ?: 0 }
    suspend fun incrementTrialPlays() { context.dataStore.edit { it[Keys.TRIAL_PLAYS] = (it[Keys.TRIAL_PLAYS] ?: 0) + 1 } }

    // 新手引导：仅首局展示一次
    val tutorialDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.TUTORIAL_DONE] ?: false }
    suspend fun setTutorialDone(v: Boolean) { context.dataStore.edit { it[Keys.TUTORIAL_DONE] = v } }

    // 断点续玩
    val gameSnapshot: Flow<String?> = context.dataStore.data.map { it[Keys.GAME_SNAPSHOT] }
    val autoPauseOnBg: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_PAUSE_ON_BG] ?: true }
    suspend fun saveSnapshot(json: String?) {
        context.dataStore.edit { prefs ->
            if (json == null) prefs.remove(Keys.GAME_SNAPSHOT) else prefs[Keys.GAME_SNAPSHOT] = json
        }
    }
    suspend fun clearSnapshot() { context.dataStore.edit { it.remove(Keys.GAME_SNAPSHOT) } }

    suspend fun updateEndlessResult(score: Int, combo: Int, lines: Int) {
        context.dataStore.edit { prefs ->
            val oldHigh = prefs[Keys.HIGH_SCORE] ?: 0
            if (score > oldHigh) prefs[Keys.HIGH_SCORE] = score
            val oldCombo = prefs[Keys.MAX_COMBO] ?: 0
            if (combo > oldCombo) prefs[Keys.MAX_COMBO] = combo
            prefs[Keys.TOTAL_LINES] = (prefs[Keys.TOTAL_LINES] ?: 0) + lines
            prefs[Keys.TOTAL_GAMES] = (prefs[Keys.TOTAL_GAMES] ?: 0) + 1
        }
    }

    suspend fun completeAdventureLevel(levelId: Int, stars: Int, coinsReward: Int) {
        context.dataStore.edit { prefs ->
            val curLevel = prefs[Keys.CURRENT_LEVEL] ?: 1
            if (levelId >= curLevel) prefs[Keys.CURRENT_LEVEL] = minOf(levelId + 1, 200)
            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 0) + coinsReward
            val raw = prefs[Keys.STAR_LEVELS] ?: ""
            val map = parseStarLevels(raw).toMutableMap()
            val old = map[levelId] ?: 0
            if (stars > old) map[levelId] = stars
            prefs[Keys.STAR_LEVELS] = map.entries.joinToString(";") { (k, v) -> "$k:$v" }
        }
    }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 0) + amount
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var ok = false
        context.dataStore.edit { prefs ->
            val cur = prefs[Keys.COINS] ?: 0
            if (cur >= amount) {
                prefs[Keys.COINS] = cur - amount
                ok = true
            }
        }
        return ok
    }

    private fun parseStarLevels(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        raw.split(";").forEach { seg ->
            if (seg.isBlank()) return@forEach
            val idx = seg.indexOf(':')
            if (idx <= 0) return@forEach
            val lv = seg.substring(0, idx).toIntOrNull() ?: return@forEach
            val st = seg.substring(idx + 1).toIntOrNull() ?: return@forEach
            result[lv] = st
        }
        return result
    }
}
