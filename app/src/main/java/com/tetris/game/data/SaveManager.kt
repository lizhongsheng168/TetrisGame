package com.tetris.game.data

import android.content.Context
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
