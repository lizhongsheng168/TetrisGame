package com.tetris.game
import com.tetris.game.engine.*
import org.junit.Test
import org.junit.Assert.*

class TetrisClearBugTest {
    @Test fun tetrisFourLinesShouldClearAll() {
        val engine = TetrisEngine()
        engine.start()
        // 构造：底部4行（16-19）每行0-8已填满，9列空缺，等待竖直I填补
        for (r in 16..19) {
            for (c in 0..8) engine.board[r][c] = 0
            engine.board[r][9] = null
        }
        // 确保上方无干扰
        for (r in 0..15) for (c in 0 until BOARD_WIDTH) engine.board[r][c] = null
        // 放置竖直I，使其恰好填满4行的第9列
        // I 竖直形态 rotation=1，方块在 shape 的 col=2
        engine.current = Piece(TetrominoType.I, rotation = 1, x = 7, y = 16)
        assertFalse("放置位置不应碰撞", engine.collides(engine.current!!))
        val beforeLines = engine.linesCleared
        val beforeScore = engine.score
        engine.hardDrop()
        println("After hardDrop: linesCleared=${engine.linesCleared} score=${engine.score} state=${engine.gameState}")
        // 打印棋盘底部5行
        for (r in 15..19) {
            print("row $r: ")
            for (c in 0 until BOARD_WIDTH) print(if (engine.board[r][c]==null) "." else "#")
            println()
        }
        assertEquals("应一次消除4行", 4, engine.linesCleared - beforeLines)
        assertEquals("四行Tetris应得800分*等级1=800", 800, engine.score - beforeScore)
        // 底部4行应已清空（无残留满行）
        for (r in 16..19) {
            var empty = true
            for (c in 0 until BOARD_WIDTH) if (engine.board[r][c] != null) empty = false
            // 允许顶部下落的方块残留，但不应残留满行
            var full = true
            for (c in 0 until BOARD_WIDTH) if (engine.board[r][c]==null) full=false
            assertFalse("第 $r 行不应仍为满行（BUG：消4行只消了2行）", full)
        }
    }

    @Test fun threeLinesShouldClearAll() {
        val engine = TetrisEngine()
        engine.start()
        for (r in 17..19) {
            for (c in 0..8) engine.board[r][c] = 0
            engine.board[r][9] = null
        }
        for (r in 0..16) for (c in 0 until BOARD_WIDTH) engine.board[r][c] = null
        // 用竖直I覆盖16-19，但16行原本是空的，所以只填满17-19三行，额外需要保证16行也被填满才能测3行
        // 改为：让17-19满，16行放一个横置方块补？简化：直接构造16-19中17-19三行满，16行故意留空1格让I补满3行中的某列
        // 更简单：构造底部3行全满（10列），直接模拟 lock 后触发 clearLines
        // 这里直接填满3行
        for (r in 17..19) for (c in 0 until BOARD_WIDTH) engine.board[r][c] = 0
        // 放一个方块在上空锁定触发消除
        engine.current = Piece(TetrominoType.O, rotation=0, x=4, y=14)
        // 将O方块硬降到顶部不会影响底部3行满的判断，但会触发clear
        // 改为直接让O锁定在非满行区域，不填满新行
        engine.hardDrop()
        println("Three lines test: linesCleared=${engine.linesCleared}")
        assertEquals(3, engine.linesCleared)
    }
}
