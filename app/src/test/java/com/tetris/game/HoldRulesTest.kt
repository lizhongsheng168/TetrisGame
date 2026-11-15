package com.tetris.game

import com.tetris.game.engine.GameState
import com.tetris.game.engine.TetrisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldRulesTest {
    @Test fun holdSwapsOnceUntilThePieceLocks() {
        val engine = TetrisEngine()
        engine.start()
        val first = engine.current!!.type
        val queued = engine.nextQueue.first()
        val queueSize = engine.nextQueue.size

        assertTrue(engine.hold())
        assertEquals(first, engine.holdPiece)
        assertEquals(queued, engine.current!!.type)
        assertEquals(queueSize, engine.nextQueue.size)
        assertTrue(engine.holdUsed)

        val parked = engine.holdPiece
        val showing = engine.current!!.type
        assertFalse("同一颗落下前不能再暂存", engine.hold())
        assertEquals(parked, engine.holdPiece)
        assertEquals(showing, engine.current!!.type)
        assertEquals(0, engine.moveCount)

        engine.hardDrop()
        assertEquals(GameState.PLAYING, engine.gameState)
        assertFalse(engine.holdUsed)
        assertTrue(engine.hold())
        assertEquals(first, engine.current!!.type)
    }
}
