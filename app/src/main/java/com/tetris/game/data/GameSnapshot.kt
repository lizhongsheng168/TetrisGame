package com.tetris.game.data

import com.tetris.game.engine.*
import org.json.JSONArray
import org.json.JSONObject

/** 断点续玩快照：JSON 序列化的对局状态，存于 DataStore */
object GameSnapshot {

    fun encode(
        engine: TetrisEngine,
        mode: GameMode,
        levelId: Int
    ): String {
        val o = JSONObject()
        o.put("mode", mode.name)
        o.put("levelId", levelId)
        o.put("score", engine.score)
        o.put("lines", engine.linesCleared)
        o.put("level", engine.level)
        o.put("combo", engine.combo)
        o.put("moveCount", engine.moveCount)
        o.put("gameState", engine.gameState.name)
        // board 20x10：null -> -99，-1 障碍保留，0..6 颜色
        val boardArr = JSONArray()
        for (r in 0 until BOARD_HEIGHT) {
            val row = JSONArray()
            for (c in 0 until BOARD_WIDTH) {
                val v = engine.board[r][c]
                row.put(if (v == null) -99 else v)
            }
            boardArr.put(row)
        }
        o.put("board", boardArr)
        // current
        engine.current?.let { p ->
            val cur = JSONObject()
            cur.put("type", p.type.name)
            cur.put("rot", p.rotation)
            cur.put("x", p.x)
            cur.put("y", p.y)
            o.put("current", cur)
        }
        // nextQueue
        val nextArr = JSONArray()
        engine.nextQueue.forEach { nextArr.put(it.name) }
        o.put("next", nextArr)
        o.put("hold", engine.holdPiece?.name ?: JSONObject.NULL)
        return o.toString()
    }

    fun decode(json: String, engine: TetrisEngine): Boolean {
        return try {
            val o = JSONObject(json)
            engine.score = o.optInt("score", 0)
            engine.linesCleared = o.optInt("lines", 0)
            engine.level = o.optInt("level", 1)
            engine.combo = o.optInt("combo", 0)
            engine.moveCount = o.optInt("moveCount", 0)
            val boardArr = o.optJSONArray("board") ?: return false
            for (r in 0 until BOARD_HEIGHT) {
                val row = boardArr.optJSONArray(r) ?: continue
                for (c in 0 until BOARD_WIDTH) {
                    val v = row.optInt(c, -99)
                    engine.board[r][c] = if (v == -99) null else v
                }
            }
            // current
            val cur = o.optJSONObject("current")
            engine.current = if (cur != null) {
                val type = TetrominoType.valueOf(cur.optString("type", "T"))
                Piece(type, cur.optInt("rot", 0), cur.optInt("x", 3), cur.optInt("y", -1))
            } else null
            // nextQueue
            engine.nextQueue.clear()
            val nextArr = o.optJSONArray("next")
            if (nextArr != null) {
                for (i in 0 until nextArr.length()) {
                    val n = nextArr.optString(i, "")
                    if (n.isNotBlank()) engine.nextQueue.addLast(TetrominoType.valueOf(n))
                }
            }
            // hold
            val holdRaw = if (o.isNull("hold")) null else o.optString("hold", null)
            engine.holdPiece = if (holdRaw.isNullOrBlank()) null else try { TetrominoType.valueOf(holdRaw) } catch (_: Exception) { null }
            engine.gameState = try { GameState.valueOf(o.optString("gameState", "PLAYING")) } catch (_: Exception) { GameState.PLAYING }
            // 确保至少 3 个 next
            while (engine.nextQueue.size < 3) engine.nextQueue.addLast(BagRandomizer().next())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun modeOf(json: String): GameMode? = try { GameMode.valueOf(JSONObject(json).optString("mode", "")) } catch (_: Exception) { null }
    fun levelIdOf(json: String): Int = try { JSONObject(json).optInt("levelId", 0) } catch (_: Exception) { 0 }
}
