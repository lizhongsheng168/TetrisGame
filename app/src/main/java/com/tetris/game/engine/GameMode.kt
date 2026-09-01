package com.tetris.game.engine

/** 游戏模式 */
enum class GameMode(val title: String) {
    ENDLESS("经典无尽"),
    ADVENTURE("冒险闯关")
}

/** 冒险关卡配置 */
data class LevelConfig(
    val id: Int,
    val name: String,
    val goalLines: Int,        // 目标消除行数
    val moveLimit: Int = 0,    // 步数限制（0=不限）
    val obstacleRows: List<List<Int?>> = emptyList()  // 障碍：null=空, -1=障碍块
)

/** 内置冒险关卡（V1.0：200 关，这里定义前 30 关 + 程序化生成其余） */
object LevelCatalog {
    fun getLevel(id: Int): LevelConfig {
        // 前 15 关手写定义（教学循序渐进）
        if (id in 1..15) {
            return handcraftedLevels[id - 1]
        }
        // 其余程序化生成：目标行数随关卡递增
        val goal = when {
            id <= 30 -> 6 + (id - 15)
            id <= 60 -> 15 + (id - 30) / 2
            id <= 100 -> 25 + (id - 60) / 3
            else -> 35 + (id - 100) / 4
        }
        // 高关卡加入障碍物（从第 50 关起）
        val obstacles = if (id >= 50 && id % 5 == 0) {
            generateObstacles(id)
        } else emptyList()
        return LevelConfig(
            id = id,
            name = "第 $id 关",
            goalLines = minOf(goal, 100),
            obstacleRows = obstacles
        )
    }

    private fun generateObstacles(id: Int): List<List<Int?>> {
        // 底部 1-2 行随机障碍
        val rows = mutableListOf<List<Int?>>()
        repeat((id / 30).coerceAtMost(2)) { rowIdx ->
            val row = (0 until 10).map { col ->
                if (col % 3 == rowIdx % 3) -1 else null
            }
            rows.add(row)
        }
        return rows
    }

    private val handcraftedLevels = listOf(
        LevelConfig(1, "新手入门", 3),
        LevelConfig(2, "初试身手", 5),
        LevelConfig(3, "小试牛刀", 8),
        LevelConfig(4, "渐入佳境", 10),
        LevelConfig(5, "节奏初现", 12),
        LevelConfig(6, "行云流水", 15),
        LevelConfig(7, "步步为营", 18),
        LevelConfig(8, "关卡挑战", 20),
        LevelConfig(9, "速度考验", 22, moveLimit = 60),
        LevelConfig(10, "极限冲刺", 25, moveLimit = 70),
        LevelConfig(11, "稳扎稳打", 15),
        LevelConfig(12, "厚积薄发", 18),
        LevelConfig(13, "一鼓作气", 20),
        LevelConfig(14, "乘胜追击", 22),
        LevelConfig(15, "巅峰对决", 25)
    )
}
