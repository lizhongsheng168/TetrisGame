package com.tetris.game

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.tetris.game.ui.screens.ButtonControlArea
import com.tetris.game.ui.screens.OverlayDialog
import com.tetris.game.ui.theme.LocalAppTheme
import com.tetris.game.ui.theme.themeOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w320dp-h640dp-xhdpi")
class LayoutShotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun narrowControlsKeepFiveLabelsOnOneRow() {
        rule.setContent {
            CompositionLocalProvider(LocalAppTheme provides themeOf("starry")) {
                Box(Modifier.requiredSize(320.dp, 220.dp)) {
                    ButtonControlArea(
                        hapticEnabled = false,
                        onMoveLeft = {},
                        onMoveRight = {},
                        onRotate = {},
                        onHold = {},
                        onSoftDrop = {},
                        onHardDrop = {}
                    )
                }
            }
        }
        val labels = listOf("左移", "右移", "旋转", "暂存", "下落")
        val bounds = labels.map { label ->
            rule.onNodeWithText(label, useUnmergedTree = true).assertExists().fetchSemanticsNode().boundsInRoot
        }
        val tops = bounds.map { it.top }
        assertTrue("五个标签应在同一行 $bounds", tops.max() - tops.min() < 8f)
        bounds.forEach { box ->
            assertTrue("标签不应折成两行: $box", box.height < 40f)
            assertTrue("标签应有宽度", box.width > 16f)
        }
        bounds.zipWithNext().forEach { (left, right) ->
            assertTrue("标签不应互相压住", left.right <= right.left + 2f)
        }
        saveShot("narrow-controls.png")
    }

    @Test fun failDialogFitsPriceAndButtonsOnANarrowPhone() {
        rule.setContent {
            CompositionLocalProvider(LocalAppTheme provides themeOf("starry")) {
                Box(Modifier.requiredSize(320.dp, 640.dp)) {
                    OverlayDialog(
                        title = "堆到顶了",
                        subTitle = "目标 20 行，已消除 0 行\n得分 0\n再试一次需要 30 金币，当前 80 金币",
                        buttonText = "30 金币再试一次",
                        secondaryText = "返回选关",
                        onAction = {},
                        onSecondary = {}
                    )
                }
            }
        }
        val price = rule.onNodeWithText("再试一次需要 30 金币，当前 80 金币", substring = true, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val action = rule.onNodeWithText("30 金币再试一次").fetchSemanticsNode().boundsInRoot
        val back = rule.onNodeWithText("返回选关").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText("堆到顶了", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("价格应在按钮上方 price=$price action=$action", price.bottom + 8f <= action.top)
        assertTrue("主按钮和返回之间要留出空隙 action=$action back=$back", action.bottom + 4f <= back.top)
        assertTrue("主按钮应铺开 action=$action price=$price", action.width > price.width * 0.85f)
        assertTrue("标题仍在价格上方 title=$title price=$price", title.bottom <= price.top)
        saveShot("narrow-fail-dialog.png")
    }

    private fun saveShot(name: String) {
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        assertTrue("窗口还没有尺寸", view.width > 0 && view.height > 0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        var varied = false
        val first = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        val stepX = (bitmap.width / 12).coerceAtLeast(1)
        val stepY = (bitmap.height / 12).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                if (bitmap.getPixel(x, y) != first) varied = true
            }
        }
        assertTrue("截图像素不应是一整片纯色", varied)
        val dir = File("/cursor/stores/self/media")
        dir.mkdirs()
        val out = File(dir, name)
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 2000)
    }
}
