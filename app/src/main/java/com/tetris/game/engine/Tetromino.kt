package com.tetris.game.engine

/**
 * 7 种标准方块（Tetromino）定义。
 * 每种方块用旋转状态（0-3）下的 4x4 矩阵表示，1 表示占用格。
 */
enum class TetrominoType(val colorIndex: Int) {
    I(0), O(1), T(2), L(3), J(4), S(5), Z(6)
}

data class Piece(
    val type: TetrominoType,
    val rotation: Int = 0,
    val x: Int = 3,
    val y: Int = -1
) {
    /** 返回当前旋转状态下的形状矩阵（4x4） */
    fun shape(): Array<IntArray> = SHAPES[type]!![rotation]

    fun rotated(): Piece = copy(rotation = (rotation + 1) % 4)

    fun moved(dx: Int, dy: Int): Piece = copy(x = x + dx, y = y + dy)
}

/** 每个方块的 4 个旋转状态 */
val SHAPES: Map<TetrominoType, Array<Array<IntArray>>> = mapOf(
    // I 方块（横竖）
    TetrominoType.I to arrayOf(
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 1, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0)
        )
    ),
    // O 方块（方形，不旋转）
    TetrominoType.O to arrayOf(
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),
    // T 方块
    TetrominoType.T to arrayOf(
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),
    // L 方块
    TetrominoType.L to arrayOf(
        arrayOf(
            intArrayOf(0, 0, 1, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),
    // J 方块
    TetrominoType.J to arrayOf(
        arrayOf(
            intArrayOf(1, 0, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 1, 0),
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),
    // S 方块
    TetrominoType.S to arrayOf(
        arrayOf(
            intArrayOf(0, 1, 1, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(1, 0, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    ),
    // Z 方块
    TetrominoType.Z to arrayOf(
        arrayOf(
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 1, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(0, 1, 1, 0),
            intArrayOf(0, 0, 0, 0)
        ),
        arrayOf(
            intArrayOf(0, 1, 0, 0),
            intArrayOf(1, 1, 0, 0),
            intArrayOf(1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
    )
)

/** 7-bag 随机生成器：保证每个 bag 内 7 种方块各出现一次 */
class BagRandomizer {
    private var bag = mutableListOf<TetrominoType>()

    fun next(): TetrominoType {
        if (bag.isEmpty()) {
            bag = TetrominoType.entries.toMutableList()
            bag.shuffle()
        }
        return bag.removeAt(bag.size - 1)
    }
}
