package com.tetris.game.engine

import kotlin.math.min

/** 游戏状态 */
enum class GameState {
    READY, PLAYING, PAUSED, GAME_OVER, LEVEL_CLEAR, LEVEL_FAIL
}

/** 一次消除事件 */
data class ClearEvent(
    val lines: Int,
    val isTetris: Boolean,
    val isCombo: Boolean,
    val combo: Int,
    val tSpin: Boolean
)

/** 棋盘宽高 */
const val BOARD_WIDTH = 10
const val BOARD_HEIGHT = 20

/** 核心游戏引擎：负责规则、物理、消除、计分 */
class TetrisEngine(
    val levelGoal: Int = 0,      // 冒险关卡目标行数，0 表示无尽模式
    val moveLimit: Int = 0,      // 冒险关卡步数限制，0 表示不限
    val obstacleRows: List<List<Int?>> = emptyList()  // 障碍物行（可选）
) {
    val board = Array(BOARD_HEIGHT) { arrayOfNulls<Int>(BOARD_WIDTH) } // -1 表示障碍
    var current: Piece? = null
    var holdPiece: TetrominoType? = null
    var holdUsed = false
    val nextQueue = ArrayDeque<TetrominoType>()
    var score = 0
    var linesCleared = 0
    var level = 1
    var combo = 0
    var moveCount = 0
    var gameState = GameState.READY
    var lastClearEvent: ClearEvent? = null
    var lastClearedRows: List<Int> = emptyList()
        private set
    var clearSeq: Long = 0L
        private set
    // 消除行快照：用于爆破特效逐格飞散
    var clearedSnapshot: List<List<Int>> = emptyList()
        private set

    private val randomizer = BagRandomizer()

    init {
        // 初始化障碍物
        obstacleRows.forEachIndexed { rowIdx, rowColors ->
            if (rowIdx < BOARD_HEIGHT) {
                for (col in rowColors.indices) {
                    if (col < BOARD_WIDTH) {
                        val c = rowColors[col]
                        if (c == -1) board[rowIdx][col] = -1
                        else if (c != null) board[rowIdx][col] = c
                    }
                }
            }
        }
        for (i in 0 until 3) nextQueue.addLast(randomizer.next())
    }

    /** 开始游戏 */
    fun start() {
        gameState = GameState.PLAYING
        spawnPiece()
    }

    private fun spawnPiece(): Boolean {
        if (nextQueue.isEmpty()) nextQueue.addLast(randomizer.next())
        val type = nextQueue.removeFirst()
        nextQueue.addLast(randomizer.next())
        current = Piece(type, 0, 3, -1)
        holdUsed = false
        // 触顶判定：生成位置即碰撞
        if (collides(current!!)) {
            gameState = GameState.GAME_OVER
            return false
        }
        return true
    }

    /** 碰撞检测 */
    fun collides(piece: Piece): Boolean {
        val shape = piece.shape()
        for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] == 0) continue
                val bx = piece.x + c
                val by = piece.y + r
                if (bx < 0 || bx >= BOARD_WIDTH || by >= BOARD_HEIGHT) return true
                if (by >= 0) {
                    val cell = board[by][bx]
                    if (cell != null) return true
                }
            }
        }
        return false
    }

    /** 尝试移动 */
    fun movePiece(dx: Int, dy: Int): Boolean {
        val p = current ?: return false
        val moved = p.moved(dx, dy)
        if (!collides(moved)) {
            current = moved
            return true
        }
        return false
    }

    // 复用墙踢数组，避免每帧 listOf 分配
    private val wallKicks = intArrayOf(0, -1, 1, -2, 2)

    /** 尝试旋转（带墙踢 Wall Kick） */
    fun rotatePiece(): Boolean {
        val p = current ?: return false
        val rotated = p.rotated()
        for (k in wallKicks) {
            val candidate = rotated.copy(x = rotated.x + k)
            if (!collides(candidate)) {
                current = candidate
                return true
            }
        }
        return false
    }

    /** 软降（一格） */
    fun softDrop(): Boolean {
        return movePiece(0, 1)
    }

    /** 硬降（极速落地） */
    fun hardDrop(): Int {
        var dist = 0
        while (movePiece(0, 1)) dist++
        lockPiece()
        return dist
    }

    /** Hold 存块 */
    fun hold(): Boolean {
        if (holdUsed) return false
        val p = current ?: return false
        val type = p.type
        if (holdPiece == null) {
            holdPiece = type
            holdUsed = true
            // 从队列补一个新方块
            val nextType = if (nextQueue.isNotEmpty()) nextQueue.removeFirst() else randomizer.next()
            nextQueue.addLast(randomizer.next())
            current = Piece(nextType, 0, 3, -1)
            if (collides(current!!)) {
                gameState = GameState.GAME_OVER
                return false
            }
        } else {
            val oldHold = holdPiece!!
            holdPiece = type
            holdUsed = true
            current = Piece(oldHold, 0, 3, -1)
            if (collides(current!!)) {
                gameState = GameState.GAME_OVER
                return false
            }
        }
        return true
    }

    /** 锁定方块并消除 */
    private fun lockPiece() {
        val p = current ?: return
        val shape = p.shape()
        for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] == 0) continue
                val bx = p.x + c
                val by = p.y + r
                if (by in 0 until BOARD_HEIGHT) {
                    board[by][bx] = p.type.colorIndex
                }
            }
        }
        current = null
        moveCount++
        clearLines()
        if (gameState == GameState.PLAYING) {
            spawnPiece()
        }
    }

    /** 消除完整行 — 修复自顶向下原地拷贝导致连续满行漏消的 BUG（消4行只消2行） */
    private fun clearLines() {
        // 先收集所有满行索引，供特效层使用
        val fullRows = mutableListOf<Int>()
        for (r in 0 until BOARD_HEIGHT) {
            var full = true
            for (c in 0 until BOARD_WIDTH) {
                val cell = board[r][c]
                if (cell == null || cell == -1) { full = false; break }
            }
            if (full) fullRows.add(r)
        }
        val cleared = fullRows.size
        if (cleared > 0) {
            lastClearedRows = fullRows.toList()
            clearedSnapshot = fullRows.map { r -> (0 until BOARD_WIDTH).map { c -> board[r][c] ?: 0 } }
            clearSeq++
        } else {
            lastClearedRows = emptyList()
            clearedSnapshot = emptyList()
        }
        // 写指针法：read 从底向上扫描，write 指向待写入位置；满行直接跳过（不拷贝），非满行下沉到 write
        var writeRow = BOARD_HEIGHT - 1
        for (readRow in BOARD_HEIGHT - 1 downTo 0) {
            var isFull = false
            for (rr in fullRows) if (rr == readRow) { isFull = true; break }
            if (isFull) {
                // 跳过满行
            } else {
                if (writeRow != readRow) {
                    for (c in 0 until BOARD_WIDTH) board[writeRow][c] = board[readRow][c]
                }
                writeRow--
            }
        }
        // 顶部剩余行置空
        for (r in writeRow downTo 0) {
            for (c in 0 until BOARD_WIDTH) board[r][c] = null
        }
        if (cleared > 0) {
            linesCleared += cleared
            // 计分：单行100，双行300，三行500，四行800
            val baseScore = when (cleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                else -> 800
            }
            val levelBonus = level
            combo = combo + 1
            val comboBonus = if (combo > 1) (combo - 1) * 50 else 0
            score += baseScore * levelBonus + comboBonus
            lastClearEvent = ClearEvent(
                lines = cleared,
                isTetris = cleared == 4,
                isCombo = combo > 1,
                combo = combo,
                tSpin = false
            )
            // 升级
            level = 1 + linesCleared / 10
            // 冒险模式检查通关
            if (levelGoal > 0 && linesCleared >= levelGoal) {
                gameState = GameState.LEVEL_CLEAR
            }
        } else {
            combo = 0
            lastClearEvent = null
        }
        // 步数限制检查
        if (moveLimit > 0 && moveCount >= moveLimit) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.LEVEL_FAIL
            }
        }
    }

    /** 获取当前下落速度（毫秒/格） */
    fun fallInterval(): Long {
        // 等级越高越快：起点 800ms，每级减 50ms，最低 80ms
        return maxOf(80L, 800L - (level - 1) * 50L)
    }

    /** 预览落点：返回硬降后的 y 偏移距离 */
    fun ghostOffset(): Int {
        val p = current ?: return 0
        var dist = 0
        var test = p
        while (!collides(test.moved(0, 1))) {
            test = test.moved(0, 1)
            dist++
        }
        return dist
    }

    /** 暂停下落：外部逻辑通过 fallInterval 控制 */
    fun togglePause() {
        if (gameState == GameState.PLAYING) gameState = GameState.PAUSED
        else if (gameState == GameState.PAUSED) gameState = GameState.PLAYING
    }
}
