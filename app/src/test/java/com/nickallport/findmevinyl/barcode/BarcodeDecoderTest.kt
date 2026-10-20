package com.nickallport.findmevinyl.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reads barcodes from pictures drawn by the test itself, with its own encoder written from the EAN-13 and EAN-8
 * tables, so the decoder is not checked against its own tables. The pictures are turned, blurred, noisy and unevenly lit,
 * as a phone's photo of a sleeve would be.
 */
class BarcodeDecoderTest {

    // The encoder: the 7 modules of each digit, written out from the standard tables.
    private val l = listOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
    private val g = listOf("0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111")
    private val r = listOf("1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100")
    private val parity = listOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

    private fun modules(digits: String): String {
        val d = digits.map { it - '0' }
        return if (digits.length == 8) {
            "101" + d.take(4).joinToString("") { l[it] } + "01010" + d.drop(4).joinToString("") { r[it] } + "101"
        } else {
            val first = d[0]
            val left = d.subList(1, 7).mapIndexed { i, x -> if (parity[first][i] == 'L') l[x] else g[x] }.joinToString("")
            "101" + left + "01010" + d.drop(7).joinToString("") { r[it] } + "101"
        }
    }

    /** A picture of the barcode: [module] pixels to a module, [angle] degrees of turn, and the kinds of spoiling asked for. */
    private fun render(
        code: String,
        module: Float = 4f,
        barHeight: Int = 120,
        quietModules: Int = 12,
        angle: Double = 0.0,
        noise: Double = 0.0,
        blur: Int = 0,
        uneven: Boolean = false,
        seed: Long = 7
    ): LuminanceImage {
        val bits = modules(code)
        val srcW = ((bits.length + 2 * quietModules) * module).toInt()
        val srcH = barHeight + 40
        val diag = sqrt((srcW * srcW + srcH * srcH).toDouble()).toInt() + 4
        val w = if (angle == 0.0) srcW else diag
        val h = if (angle == 0.0) srcH else diag
        val rad = angle * PI / 180
        val random = Random(seed)
        fun source(sx: Double, sy: Double): Double {
            if (sx < 0 || sy < 0 || sx >= srcW || sy >= srcH) return 255.0
            if (sy < 20 || sy >= 20 + barHeight) return 255.0
            val index = (sx / module).toInt() - quietModules
            return if (index in bits.indices && bits[index] == '1') 20.0 else 255.0
        }
        var pixels = DoubleArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            // Turn about the middle of the picture, four samples across each pixel so edges are not hard.
            var sum = 0.0
            for (k in 0 until 4) {
                val dx = x + (k + 0.5) / 4 - w / 2.0
                val dy = y + 0.5 - h / 2.0
                val sx = dx * cos(rad) + dy * sin(rad) + srcW / 2.0
                val sy = -dx * sin(rad) + dy * cos(rad) + srcH / 2.0
                sum += source(sx, sy)
            }
            var v = sum / 4
            if (uneven) v *= 0.45 + 0.55 * x / w
            pixels[y * w + x] = v
        }
        repeat(blur) {
            val next = DoubleArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                var s = 0.0
                var n = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val xx = x + dx
                    val yy = y + dy
                    if (xx in 0 until w && yy in 0 until h) { s += pixels[yy * w + xx]; n++ }
                }
                next[y * w + x] = s / n
            }
            pixels = next
        }
        return LuminanceImage(w, h, IntArray(w * h) { (pixels[it] + random.nextGaussian() * noise).toInt().coerceIn(0, 255) })
    }

    private val ean13 = "5012985305020"
    private val upcA = "724384746025"
    private val ean8 = "96385074"

    @Test
    fun `reads a clean EAN-13`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13)))
    }

    @Test
    fun `reads a clean UPC-A as its twelve digits`() {
        assertEquals(upcA, BarcodeDecoder.decode(render("0$upcA")))
    }

    @Test
    fun `reads an EAN-13 whose first digit is not zero, whatever its parity pattern`() {
        for (code in listOf("4006381333931", "9780201379624", "8717418000011", "3017620422003", "5099902988726")) {
            assertEquals(code, BarcodeDecoder.decode(render(code)))
        }
    }

    @Test
    fun `reads an EAN-8`() {
        assertEquals(ean8, BarcodeDecoder.decode(render(ean8)))
    }

    @Test
    fun `reads a barcode held upside down`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, angle = 180.0)))
    }

    @Test
    fun `reads a barcode turned on its side`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, angle = 90.0)))
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, angle = -90.0)))
    }

    @Test
    fun `reads a barcode photographed at a slant`() {
        for (angle in listOf(5.0, -8.0, 15.0, -22.0, 30.0, -40.0)) {
            assertEquals("angle $angle", ean13, BarcodeDecoder.decode(render(ean13, angle = angle)))
        }
    }

    @Test
    fun `reads a barcode turned a little off the side`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, angle = 80.0)))
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, angle = 105.0)))
    }

    @Test
    fun `reads a small barcode`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, module = 2.5f, barHeight = 60)))
    }

    @Test
    fun `reads a large barcode in a big picture`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, module = 12f, barHeight = 400)))
    }

    @Test
    fun `reads a blurred barcode`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, module = 5f, blur = 2)))
    }

    @Test
    fun `reads a noisy barcode`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, noise = 18.0)))
    }

    @Test
    fun `reads a barcode lit unevenly`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, uneven = true)))
    }

    @Test
    fun `reads a slanted, blurred, noisy, unevenly lit barcode`() {
        assertEquals(ean13, BarcodeDecoder.decode(render(ean13, module = 5f, angle = 12.0, blur = 1, noise = 10.0, uneven = true)))
    }

    @Test
    fun `reads the barcode among other marks on the sleeve`() {
        val base = render(ean13)
        val random = Random(3)
        // Dark blocks above and below the barcode, like text and a logo.
        val pixels = base.pixels.copyOf()
        for (i in 0 until 40) {
            val x = random.nextInt(base.width - 20)
            val y = if (i % 2 == 0) random.nextInt(8) else base.height - 1 - random.nextInt(8)
            for (dy in 0 until 3) for (dx in 0 until 12) if (y + dy < base.height) pixels[(y + dy) * base.width + x + dx] = 30
        }
        assertEquals(ean13, BarcodeDecoder.decode(LuminanceImage(base.width, base.height, pixels)))
    }

    @Test
    fun `a wrong check digit is not returned`() {
        assertNull(BarcodeDecoder.decode(render("5012985305021")))
    }

    @Test
    fun `a blank picture has no barcode`() {
        assertNull(BarcodeDecoder.decode(LuminanceImage(300, 200, IntArray(300 * 200) { 240 })))
    }

    @Test
    fun `a picture of noise has no barcode`() {
        val random = Random(11)
        assertNull(BarcodeDecoder.decode(LuminanceImage(400, 300, IntArray(400 * 300) { random.nextInt(256) })))
    }

    @Test
    fun `stripes that are not a barcode are not read`() {
        val w = 400
        val h = 200
        val pixels = IntArray(w * h) { if ((it % w / 7) % 2 == 0) 20 else 240 }
        assertNull(BarcodeDecoder.decode(LuminanceImage(w, h, pixels)))
    }

    @Test
    fun `the same picture gives the same answer on every try`() {
        val image = render(ean13, angle = 10.0, noise = 8.0)
        val first = BarcodeDecoder.decode(image)
        assertEquals(ean13, first)
        assertEquals(first, BarcodeDecoder.decode(image))
    }

    @Test
    fun `reads nearly every barcode under mixed spoiling, and never a wrong one`() {
        val codes = listOf(ean13, "4006381333931", "9780201379624", "3017620422003", "5099902988726", "0724384746025", ean8)
        val random = Random(99)
        var read = 0
        val total = 60
        repeat(total) { n ->
            val code = codes[n % codes.size]
            val image = render(
                code, module = 3f + random.nextFloat() * 6f, angle = random.nextDouble() * 360 - 180,
                blur = random.nextInt(3), noise = random.nextDouble() * 20, uneven = random.nextBoolean(), seed = n.toLong()
            )
            val result = BarcodeDecoder.decode(image)
            val expected = if (code.length == 13 && code.startsWith("0")) code.substring(1) else code
            if (result != null) assertEquals("wrong number for $code at try $n", expected, result)
            if (result != null) read++
        }
        assertEquals("read $read of $total", true, read >= 54)
    }
}
