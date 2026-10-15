package com.tetris.game

import com.tetris.game.engine.BOARD_WIDTH
import com.tetris.game.engine.GameState
import com.tetris.game.engine.LevelCatalog
import com.tetris.game.engine.Piece
import com.tetris.game.engine.TetrisEngine
import com.tetris.game.engine.TetrominoType
import com.tetris.game.engine.adventureStars
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
}
