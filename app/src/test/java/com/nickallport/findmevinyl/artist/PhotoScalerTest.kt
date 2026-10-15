package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoScalerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun encoded(width: Int, height: Int, format: Bitmap.CompressFormat, color: Int = Color.RED): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { it.eraseColor(color) }
        return ByteArrayOutputStream().also { bitmap.compress(format, 95, it) }.toByteArray()
    }

    private fun jpeg(width: Int, height: Int) = encoded(width, height, Bitmap.CompressFormat.JPEG)

    private fun sizeOf(bytes: ByteArray): Pair<Int, Int> {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
        return bitmap.width to bitmap.height
    }

    @Test
    fun `shrinks a big picture so its longest side is the limit, keeping its shape`() {
        val scaled = PhotoScaler.scale(jpeg(2000, 1000), maxSide = 600)!!

        assertEquals(600 to 300, sizeOf(scaled))
    }

    @Test
    fun `shrinks a tall picture the same way`() {
        val scaled = PhotoScaler.scale(jpeg(1000, 3000), maxSide = 600)!!

        assertEquals(200 to 600, sizeOf(scaled))
    }

    @Test
    fun `leaves a picture that is already small alone, and never makes it bigger`() {
        val scaled = PhotoScaler.scale(jpeg(200, 100), maxSide = 600)!!

        assertEquals(200 to 100, sizeOf(scaled))
    }

    @Test
    fun `the result is a JPEG`() {
        val scaled = PhotoScaler.scale(encoded(300, 300, Bitmap.CompressFormat.PNG), maxSide = 600)!!

        assertEquals(0xFF.toByte(), scaled[0])
        assertEquals(0xD8.toByte(), scaled[1])
    }

    @Test
    fun `a big picture becomes much smaller in bytes`() {
        val original = jpeg(3000, 2000)

        val scaled = PhotoScaler.scale(original, maxSide = 600)!!

        assertTrue("${scaled.size} bytes is not smaller than ${original.size}", scaled.size < original.size)
    }

    @Test
    fun `a transparent picture gets a white background, not a black one`() {
        val png = encoded(10, 10, Bitmap.CompressFormat.PNG, color = Color.TRANSPARENT)

        val scaled = PhotoScaler.scale(png, maxSide = 600)!!

        val pixel = BitmapFactory.decodeByteArray(scaled, 0, scaled.size).getPixel(5, 5)
        assertTrue("red is ${Color.red(pixel)}", Color.red(pixel) > 240)
        assertTrue("green is ${Color.green(pixel)}", Color.green(pixel) > 240)
        assertTrue("blue is ${Color.blue(pixel)}", Color.blue(pixel) > 240)
    }

    @Test
    fun `turns a picture upright when the camera noted it was held sideways`() {
        val file = folder.newFile("sideways.jpg")
        file.writeBytes(jpeg(200, 100))
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val scaled = PhotoScaler.scale(file.readBytes(), maxSide = 600)!!

        assertEquals(100 to 200, sizeOf(scaled))
    }

    @Test
    fun `a picture with no orientation note is left as it is`() {
        val scaled = PhotoScaler.scale(jpeg(200, 100), maxSide = 600)!!

        assertEquals(200 to 100, sizeOf(scaled))
    }

    @Test
    fun `gives nothing for bytes that are not a picture`() {
        assertNull(PhotoScaler.scale("not a picture".toByteArray(), maxSide = 600))
        assertNull(PhotoScaler.scale(ByteArray(0), maxSide = 600))
    }

    @Test
    fun `gives nothing for a picture that is cut off`() {
        val whole = jpeg(400, 400)

        assertNull(PhotoScaler.scale(whole.copyOf(20), maxSide = 600))
    }

    @Test
    fun `the default limit keeps a photo big enough for a header and small enough to store`() {
        val scaled = PhotoScaler.scale(jpeg(4000, 3000))!!

        val (width, height) = sizeOf(scaled)
        assertTrue("longest side is ${maxOf(width, height)}", maxOf(width, height) in 400..1000)
        assertNotNull(scaled)
    }
}
