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

/**
 * 冒险星级。
 * 普通关：按「每次落子平均消行」计，单消是 1 星，带双消到 2 星，大量连消或一次消多行到 3 星。
 * 步数关：剩下的步数越多星越高，至少留下四分之一步数才是 3 星。
 */
fun adventureStars(linesCleared: Int, moveCount: Int, config: LevelConfig): Int {
    if (config.moveLimit > 0) {
        val remainRatio = (config.moveLimit - moveCount).coerceAtLeast(0).toFloat() / config.moveLimit.toFloat()
        return when {
            remainRatio >= 0.25f -> 3
            remainRatio >= 0.08f -> 2
            else -> 1
        }
    }
    val perLock = linesCleared.toFloat() / moveCount.coerceAtLeast(1).toFloat()
    return when {
        perLock >= 1.6f -> 3
        perLock >= 1.15f -> 2
        else -> 1
    }
}

/** 冒险失败或堆到顶后，原地再试一次的金币价格。无尽模式不收费。 */
const val ADVENTURE_RETRY_COST = 30

data class AdventureRetryQuote(
    val cost: Int,
    val balance: Int,
    val canAfford: Boolean,
    val shortfall: Int
)

/** 报价只读余额，不扣钱。余额不足时 canAfford 为 false。 */
fun adventureRetryQuote(balance: Int, cost: Int = ADVENTURE_RETRY_COST): AdventureRetryQuote {
    val safeBalance = balance.coerceAtLeast(0)
    val safeCost = cost.coerceAtLeast(1)
    val shortfall = (safeCost - safeBalance).coerceAtLeast(0)
    return AdventureRetryQuote(
        cost = safeCost,
        balance = safeBalance,
        canAfford = shortfall == 0,
        shortfall = shortfall
    )
}

/**
 * 只有再试一次真正开始时才扣费。
 * 余额够则返回扣完后的余额，不够返回 null，调用方不得重开。
 */
fun startAdventureRetry(balance: Int, cost: Int = ADVENTURE_RETRY_COST): Int? {
    val quote = adventureRetryQuote(balance, cost)
    if (!quote.canAfford) return null
    return quote.balance - quote.cost
}

fun adventureRetryPriceLine(balance: Int, cost: Int = ADVENTURE_RETRY_COST): String {
    val quote = adventureRetryQuote(balance, cost)
    return if (quote.canAfford) {
        "再试一次需要 ${quote.cost} 金币，当前 ${quote.balance} 金币"
    } else {
        "再试一次需要 ${quote.cost} 金币，当前只有 ${quote.balance} 金币，还差 ${quote.shortfall} 金币"
    }
}

fun adventureRetryButtonLabel(balance: Int, cost: Int = ADVENTURE_RETRY_COST): String {
    val quote = adventureRetryQuote(balance, cost)
    return if (quote.canAfford) "${quote.cost} 金币再试一次" else "金币不足"
}

fun adventureStarHint(config: LevelConfig?, compact: Boolean = false): String {
    val moveLimited = config != null && config.moveLimit > 0
    return when {
        moveLimited && compact -> "剩步越多星越高"
        moveLimited -> "步数剩得越多星越高"
        compact -> "连消越多星越高"
        else -> "一次消得越多星越高"
    }
}

/** 四个世界。前 15 关仍是手写教学，16 关起按所在世界生成，不再共用同一种棋盘。 */
data class AdventureWorld(
    val name: String,
    val range: IntRange,
    val rule: String
)

object AdventureWorlds {
    val all = listOf(
        AdventureWorld("世界一", 1..50, "前 15 关只练消行，第 16 关起底部留一道缺口"),
        AdventureWorld("世界二", 51..100, "步数用完前消满目标，剩得越多星越高"),
        AdventureWorld("世界三", 101..150, "底部叠着残垣，补上缺口整行才会消掉"),
        AdventureWorld("世界四", 151..200, "残垣和步数一起算，两边都要顾上")
    )

    fun of(levelId: Int): AdventureWorld = all.firstOrNull { levelId in it.range } ?: all.first()
}

/** 内置冒险关卡（V1.0：200 关，前 15 关手写，其余按世界规则生成） */
object LevelCatalog {
    fun getLevel(id: Int): LevelConfig {
        if (id in 1..15) return handcraftedLevels[id - 1]
        val world = AdventureWorlds.of(id)
        val goal = generatedGoal(id)
        return when (world.range.first) {
            1 -> LevelConfig(id, "第 $id 关", goal, obstacleRows = gapRow(id))
            51 -> LevelConfig(id, "第 $id 关", goal, moveLimit = moveBudget(goal, tight = false))
            101 -> LevelConfig(id, "第 $id 关", goal, obstacleRows = ruinRows(id, rows = 2))
            else -> LevelConfig(
                id,
                "第 $id 关",
                goal,
                moveLimit = moveBudget(goal, tight = true),
                obstacleRows = ruinRows(id, rows = 3)
            )
        }
    }

    private fun generatedGoal(id: Int): Int {
        val raw = when {
            id <= 50 -> 8 + (id - 16) / 3
            id <= 100 -> 12 + (id - 51) / 4
            id <= 150 -> 14 + (id - 101) / 5
            else -> 16 + (id - 151) / 5
        }
        return raw.coerceIn(8, 28)
    }

    /** 宽松大约每行 4 步，夹击大约每行 3 步。预算必须大于目标，否则还没消完就失败。 */
    private fun moveBudget(goal: Int, tight: Boolean): Int = goal * if (tight) 3 else 4

    /** 世界一：底部一行，只留一个会随关卡移动的缺口。 */
    private fun gapRow(id: Int): List<List<Int?>> {
        val hole = (id * 3) % BOARD_WIDTH
        return listOf(List(BOARD_WIDTH) { col -> if (col == hole) null else -1 })
    }

    /** 世界三、四：多行残垣，每行两个错开的缺口，避免整行在开局就是满的。 */
    private fun ruinRows(id: Int, rows: Int): List<List<Int?>> {
        val shift = id % BOARD_WIDTH
        return List(rows) { rowIdx ->
            val hole = (shift + rowIdx * 2) % BOARD_WIDTH
            val hole2 = (hole + 5) % BOARD_WIDTH
            List(BOARD_WIDTH) { col -> if (col == hole || col == hole2) null else -1 }
        }
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
