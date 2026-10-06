package io.github.dant3.kotest.robolectric.e2e

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.annotation.GraphicsMode

/** What a canvas leaves in a bitmap after filling it red: red only when the drawing really happens. */
private fun paintedPixel(): Int {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawColor(Color.RED)
    return bitmap.getPixel(0, 0)
}

@RobolectricTest
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeGraphicsModeTest :
    StringSpec({
        "@GraphicsMode(NATIVE) on the spec draws for real" {
            paintedPixel() shouldBe Color.RED
        }
    })

// Same @Config as the spec above, so a runner cached by @Config alone would hand one of them the other's mode.
@RobolectricTest
class DefaultGraphicsModeTest :
    StringSpec({
        "a spec without @GraphicsMode keeps the legacy mode, which draws nothing" {
            paintedPixel() shouldBe 0
        }
    })
