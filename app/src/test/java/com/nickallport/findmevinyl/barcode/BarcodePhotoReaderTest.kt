package com.nickallport.findmevinyl.barcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/** Reads a barcode from the bytes of a photo, as the camera app leaves them. A real picture format needs the native graphics. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BarcodePhotoReaderTest {

    private val l = listOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
    private val g = listOf("0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111")
    private val r = listOf("1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100")
    private val parity = listOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

    private fun modules(code: String): String {
        val d = code.map { it - '0' }
        val left = d.subList(1, 7).mapIndexed { i, x -> if (parity[d[0]][i] == 'L') l[x] else g[x] }.joinToString("")
        return "101" + left + "01010" + d.drop(7).joinToString("") { r[it] } + "101"
    }

    /** A photo of the barcode on a white sleeve, [width] pixels wide, as JPEG bytes (or PNG when [png]). */
    private fun photo(code: String, width: Int = 1600, height: Int = 1200, png: Boolean = false, turned: Boolean = false): ByteArray {
        val bits = modules(code)
        val module = width / 2f / bits.length
        val left = (width - bits.length * module) / 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(235, 235, 230))
        for (x in 0 until width) {
            val index = ((x - left) / module).toInt()
            if (x >= left && index in bits.indices && bits[index] == '1') {
                for (y in height / 3 until height * 2 / 3) bitmap.setPixel(x, y, Color.rgb(25, 25, 30))
            }
        }
        val shown = if (turned) {
            val matrix = android.graphics.Matrix().apply { postRotate(90f) }
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } else bitmap
        val out = ByteArrayOutputStream()
        shown.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }

    @Test
    fun `reads the barcode in a JPEG photo`() {
        assertEquals("5012985305020", BarcodePhotoReader.read(photo("5012985305020")))
    }

    @Test
    fun `reads the barcode in a PNG, a photo held on its side`() {
        assertEquals("5012985305020", BarcodePhotoReader.read(photo("5012985305020", png = true, turned = true)))
    }

    @Test
    fun `reads a large photo, as a camera takes it`() {
        assertEquals("4006381333931", BarcodePhotoReader.read(photo("4006381333931", width = 4000, height = 3000)))
    }

    @Test
    fun `a photo with no barcode gives nothing`() {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 190, 180)) }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)

        assertNull(BarcodePhotoReader.read(out.toByteArray()))
    }

    @Test
    fun `bytes that are not a picture give nothing`() {
        assertNull(BarcodePhotoReader.read(byteArrayOf(1, 2, 3, 4)))
        assertNull(BarcodePhotoReader.read(ByteArray(0)))
    }
}
