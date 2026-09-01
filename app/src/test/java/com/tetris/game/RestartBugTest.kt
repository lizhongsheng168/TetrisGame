package com.tetris.game
import com.tetris.game.engine.*
import org.junit.Test
import org.junit.Assert.*

class RestartBugTest {
    @Test fun restartThenOneHardDropShouldNotGameOver() {
        repeat(5) { trial ->
            var engine = TetrisEngine()
            engine.start()
            // play a bit to mimic real game
            repeat(8) { engine.hardDrop(); if(engine.gameState==GameState.GAME_OVER) return@repeat }
            println("trial $trial pre-restart boardCount=${engine.board.sumOf{row-> row.count{it!=null}}} state=${engine.gameState}")
            // restart
            engine = TetrisEngine()
            engine.start()
            println("  restart $trial boardCount=${engine.board.sumOf{row-> row.count{it!=null}}} state=${engine.gameState} current=${engine.current}")
            val before = engine.gameState
            engine.hardDrop()
            println("  after 1 hardDrop state=${engine.gameState} lines=${engine.linesCleared}")
            assertNotEquals("After restart one hardDrop should not be GAME_OVER (trial $trial) board=${engine.board.contentDeepToString()}", GameState.GAME_OVER, engine.gameState)
        }
    }
    @Test fun engineBoardEmptyAfterRestart() {
        var e = TetrisEngine()
        e.start()
        e.hardDrop()
        e.hardDrop()
        e = TetrisEngine()
        e.start()
        assertEquals(0, e.board.sumOf{row-> row.count{it!=null}})
        assertEquals(GameState.PLAYING, e.gameState)
    }
}
