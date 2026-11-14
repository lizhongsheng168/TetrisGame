package com.tetris.game

import com.tetris.game.engine.BOARD_WIDTH
import com.tetris.game.engine.GameState
import com.tetris.game.engine.LevelCatalog
import com.tetris.game.engine.Piece
import com.tetris.game.engine.TetrisEngine
import com.tetris.game.engine.TetrominoType
import com.tetris.game.engine.AdventureWorlds
import com.tetris.game.engine.ADVENTURE_RETRY_COST
import com.tetris.game.engine.adventureRetryButtonLabel
import com.tetris.game.engine.adventureRetryPriceLine
import com.tetris.game.engine.adventureRetryQuote
import com.tetris.game.engine.adventureStars
import com.tetris.game.engine.startAdventureRetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdventureRulesTest {
    @Test fun singleClearsEarnOneStar() {
        val config = LevelCatalog.getLevel(1)
        assertEquals(1, adventureStars(linesCleared = 3, moveCount = 3, config))
    }

    @Test fun aTripleEarnsThreeStars() {
        val config = LevelCatalog.getLevel(1)
        assertEquals(3, adventureStars(linesCleared = 3, moveCount = 1, config))
    }

    @Test fun mixedDoublesEarnTwoStars() {
        val config = LevelCatalog.getLevel(6)
        // 15 lines across 12 locks ≈ 1.25 lines each
        assertEquals(2, adventureStars(linesCleared = 15, moveCount = 12, config))
    }

    @Test fun moveLimitStarsFollowRemainingBudget() {
        val config = LevelCatalog.getLevel(9)
        assertTrue(config.moveLimit > 0)
        assertEquals(3, adventureStars(linesCleared = 22, moveCount = 40, config))
        assertEquals(2, adventureStars(linesCleared = 22, moveCount = 52, config))
        assertEquals(1, adventureStars(linesCleared = 22, moveCount = 58, config))
    }

    @Test fun obstaclesSitOnTheBottomAndClearWhenTheGapIsFilled() {
        val row = (0 until BOARD_WIDTH).map { col -> if (col == BOARD_WIDTH - 1) null else -1 }
        val engine = TetrisEngine(levelGoal = 1, obstacleRows = listOf(row))
        assertEquals(-1, engine.board[19][0])
        assertNull(engine.board[19][9])
        assertNull("障碍不能堆在出生行", engine.board[0][0])
        engine.start()
        assertEquals(GameState.PLAYING, engine.gameState)
        engine.board[19][9] = 0
        engine.current = Piece(TetrominoType.O, rotation = 0, x = 0, y = 0)
        engine.hardDrop()
        assertTrue(engine.linesCleared >= 1)
        for (c in 0 until BOARD_WIDTH - 1) {
            assertFalse("补上缺口后障碍行应被消掉", engine.board[19][c] == -1)
        }
    }

    @Test fun retryCostsThirtyAndShowsThePriceBeforeCharging() {
        assertEquals(30, ADVENTURE_RETRY_COST)
        val quote = adventureRetryQuote(50)
        assertTrue(quote.canAfford)
        assertEquals(30, quote.cost)
        assertEquals(50, quote.balance)
        assertEquals("再试一次需要 30 金币，当前 50 金币", adventureRetryPriceLine(50))
        assertEquals("30 金币再试一次", adventureRetryButtonLabel(50))
        assertEquals(20, startAdventureRetry(50))
        assertEquals(50, quote.balance)
    }

    @Test fun retryRefusesWhenCoinsAreShortAndDoesNotCharge() {
        val quote = adventureRetryQuote(12)
        assertFalse(quote.canAfford)
        assertEquals(18, quote.shortfall)
        assertEquals("再试一次需要 30 金币，当前只有 12 金币，还差 18 金币", adventureRetryPriceLine(12))
        assertEquals("金币不足", adventureRetryButtonLabel(12))
        assertEquals(null, startAdventureRetry(12))
        assertEquals(null, startAdventureRetry(29))
        assertEquals(null, startAdventureRetry(-4))
        assertEquals(30, adventureRetryQuote(-4).shortfall)
    }

    @Test fun exactBalanceStartsRetryAndLeavesZero() {
        assertEquals(0, startAdventureRetry(30))
        assertTrue(adventureRetryQuote(30).canAfford)
    }

    @Test fun firstFifteenLevelsStayHandWritten() {
        val names = listOf(
            "新手入门", "初试身手", "小试牛刀", "渐入佳境", "节奏初现",
            "行云流水", "步步为营", "关卡挑战", "速度考验", "极限冲刺",
            "稳扎稳打", "厚积薄发", "一鼓作气", "乘胜追击", "巅峰对决"
        )
        names.forEachIndexed { index, name ->
            val level = LevelCatalog.getLevel(index + 1)
            assertEquals(name, level.name)
            assertTrue(level.obstacleRows.isEmpty())
        }
        assertEquals(3, LevelCatalog.getLevel(1).goalLines)
        assertEquals(0, LevelCatalog.getLevel(1).moveLimit)
        assertEquals(60, LevelCatalog.getLevel(9).moveLimit)
        assertEquals(70, LevelCatalog.getLevel(10).moveLimit)
        assertEquals(25, LevelCatalog.getLevel(15).goalLines)
    }

    @Test fun eachWorldShowsItsOwnRule() {
        val rules = AdventureWorlds.all.map { it.rule }
        assertEquals(4, rules.toSet().size)
        assertEquals("前 15 关只练消行，第 16 关起底部留一道缺口", AdventureWorlds.of(1).rule)
        assertEquals(AdventureWorlds.of(16).rule, AdventureWorlds.of(50).rule)
        assertEquals("步数用完前消满目标，剩得越多星越高", AdventureWorlds.of(51).rule)
        assertEquals("底部叠着残垣，补上缺口整行才会消掉", AdventureWorlds.of(101).rule)
        assertEquals("残垣和步数一起算，两边都要顾上", AdventureWorlds.of(200).rule)
    }

    @Test fun generatedLevelsUseADifferentBoardPerWorld() {
        val gap = LevelCatalog.getLevel(16)
        assertEquals(0, gap.moveLimit)
        assertEquals(1, gap.obstacleRows.size)
        assertEquals(1, gap.obstacleRows[0].count { it == null })
        assertTrue(gap.obstacleRows[0].any { it == -1 })
        assertTrue(gap.obstacleRows != LevelCatalog.getLevel(17).obstacleRows)

        val timed = LevelCatalog.getLevel(51)
        assertTrue(timed.moveLimit > timed.goalLines)
        assertTrue(timed.obstacleRows.isEmpty())

        val ruins = LevelCatalog.getLevel(101)
        assertEquals(0, ruins.moveLimit)
        assertEquals(2, ruins.obstacleRows.size)

        val squeeze = LevelCatalog.getLevel(151)
        assertTrue(squeeze.moveLimit > 0)
        assertEquals(squeeze.goalLines * 3, squeeze.moveLimit)
        assertEquals(3, squeeze.obstacleRows.size)

        val shapes = listOf(gap, timed, ruins, squeeze).map { level ->
            "${level.moveLimit > 0}:${level.obstacleRows.size}:${level.obstacleRows.firstOrNull()?.count { it == null } ?: 0}"
        }
        assertEquals(4, shapes.toSet().size)
    }

    @Test fun generatedGarbageAlwaysLeavesAGap() {
        for (id in 16..200) {
            val level = LevelCatalog.getLevel(id)
            assertTrue(level.goalLines in 8..28)
            for (row in level.obstacleRows) {
                assertEquals(BOARD_WIDTH, row.size)
                assertTrue("第 $id 关不能开局就是满行", row.any { it == null })
                assertTrue("第 $id 关的残垣要有障碍格", row.any { it == -1 })
            }
        }
    }
}
