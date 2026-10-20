package com.nickallport.findmevinyl.barcode

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A grey picture: [pixels] are the brightness of each point, 0 for black and 255 for white, row by row. */
class LuminanceImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height) { "The pixels do not fit the size." }
    }

    /** The brightness at a point between pixels, by bilinear mixing of the four around it. Points outside are the nearest edge. */
    fun at(x: Float, y: Float): Float {
        val cx = x.coerceIn(0f, (width - 1).toFloat())
        val cy = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = floor(cx).toInt()
        val y0 = floor(cy).toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val fx = cx - x0
        val fy = cy - y0
        val top = pixels[y0 * width + x0] * (1 - fx) + pixels[y0 * width + x1] * fx
        val bottom = pixels[y1 * width + x0] * (1 - fx) + pixels[y1 * width + x1] * fx
        return top * (1 - fy) + bottom * fy
    }
}

/**
 * Reads an EAN-13, UPC-A or EAN-8 barcode from a picture, with no library. Lines are drawn across the picture at many
 * angles, so a barcode turned, upside down or on its side is found. Each line is cut into bars and spaces at the points
 * where the brightness crosses the middle of its surroundings (which copes with uneven light, and measures a bar from
 * edge to edge, so a blurred bar is not thicker than it is), and the widths are matched against the tables of digits.
 * A number is returned only when its check digit is right, and when two lines agree on it, or no other number was found.
 */
object BarcodeDecoder {
    // The 7 modules of each digit of an EAN or UPC barcode, as a run of widths beginning with a space (left digits)
    // or a bar (right digits). L and G are the two codes of the left half, R of the right.
    private val L = listOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
    private val R = listOf("1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100")
    private val G = R.map { it.reversed() }

    // Which of L and G the six left digits of an EAN-13 use, which hides its first digit.
    private val PARITY = listOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

    private class Pattern(val digit: Int, val code: Char, val runs: FloatArray)

    private fun runsOf(bits: String): FloatArray {
        val runs = mutableListOf<Float>()
        var count = 1
        for (i in 1 until bits.length) {
            if (bits[i] == bits[i - 1]) count++ else { runs.add(count.toFloat()); count = 1 }
        }
        runs.add(count.toFloat())
        return runs.toFloatArray()
    }

    private val leftPatterns = L.mapIndexed { d, b -> Pattern(d, 'L', runsOf(b)) } + G.mapIndexed { d, b -> Pattern(d, 'G', runsOf(b)) }
    private val rightPatterns = R.mapIndexed { d, b -> Pattern(d, 'R', runsOf(b)) }

    /** The angles, in degrees, of the lines drawn across the picture. Those near upright and near sideways come first. */
    private val ANGLES = listOf(0, 90, 5, -5, 95, 85, 10, -10, 100, 80, 15, -15, 105, 75, 20, -20, 110, 70, 25, -25, 30, -30, 40, -40, 50, -50, 60, -60, 120, 130, 140, 150, 35, -35, 45, -45)
    private const val LINES_PER_ANGLE = 24
    private const val MAX_DIGIT_ERROR = 1.8f
    private const val MAX_SINGLE_READ_ERROR = 0.6f
    private const val MIN_CONTRAST = 40f

    /** The barcode in [image], or null when there is none that can be read. */
    fun decode(image: LuminanceImage): String? {
        val votes = linkedMapOf<String, Int>()
        var best: Read? = null
        for (angle in ANGLES) {
            for (samples in lines(image, angle)) {
                val runs = cut(samples) ?: continue
                for (read in listOfNotNull(decodeRuns(runs.first, runs.second), decodeRuns(runs.first.reversedArray(), runs.second xor (runs.first.size % 2 == 0)))) {
                    val count = (votes[read.code] ?: 0) + 1
                    votes[read.code] = count
                    if (count >= 2) return read.code
                    if (best == null || read.error < best.error) best = read
                }
            }
        }
        // Two lines agreeing is the proof. A number read once is taken only when the bars fitted the tables closely.
        return best?.takeIf { it.error <= MAX_SINGLE_READ_ERROR }?.code
    }

    /** A number read from one line, with how far the widths were from the tables, on average for each digit. */
    private class Read(val code: String, val error: Float)

    // Every line across the picture at one angle, each as the brightness at a point every pixel.
    private fun lines(image: LuminanceImage, angleDegrees: Int): Sequence<FloatArray> = sequence {
        val radians = angleDegrees * PI / 180
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        val nx = -dy
        val ny = dx
        val cx = (image.width - 1) / 2f
        val cy = (image.height - 1) / 2f
        val extent = abs(nx) * cx + abs(ny) * cy
        for (i in 1..LINES_PER_ANGLE) {
            val offset = -extent + 2 * extent * i / (LINES_PER_ANGLE + 1)
            val px = cx + nx * offset
            val py = cy + ny * offset
            // The stretch of the line inside the picture.
            var tMin = -1e9f
            var tMax = 1e9f
            fun clip(p: Float, d: Float, limit: Float) {
                if (abs(d) < 1e-6f) {
                    if (p < 0 || p > limit) { tMin = 1f; tMax = 0f }
                } else {
                    val a = (0 - p) / d
                    val b = (limit - p) / d
                    tMin = max(tMin, min(a, b))
                    tMax = min(tMax, max(a, b))
                }
            }
            clip(px, dx, (image.width - 1).toFloat())
            clip(py, dy, (image.height - 1).toFloat())
            if (tMax - tMin < 30f) continue
            val count = (tMax - tMin).toInt()
            yield(FloatArray(count) { image.at(px + dx * (tMin + it), py + dy * (tMin + it)) })
        }
    }

    /**
     * Cuts a line into the widths of its bars and spaces, in pixels, and says whether the first is a bar. The first and
     * last stretch are left out because the picture ends in them. Null when the line has too little contrast to hold a barcode.
     */
    private fun cut(samples: FloatArray): Pair<FloatArray, Boolean>? {
        val n = samples.size
        val window = max(40, n / 6)
        val lows = slidingExtreme(samples, window, wantMax = false)
        val highs = slidingExtreme(samples, window, wantMax = true)

        val edges = mutableListOf<Float>()
        var black = false
        var firstEdgeToBlack = false
        var previousDifference = 0f
        var lastCrossing = 0f
        for (i in 0 until n) {
            val range = highs[i] - lows[i]
            val threshold = (highs[i] + lows[i]) / 2
            val difference = samples[i] - threshold
            // Where the line last crossed the threshold, between two points, which is where an edge is put.
            if (i > 0 && (previousDifference < 0) != (difference < 0)) {
                lastCrossing = i - 1 + previousDifference / (previousDifference - difference)
            }
            previousDifference = difference
            if (range < MIN_CONTRAST) {
                // Too flat to hold bars: a bar that was open ends here.
                if (black) { edges.add(i.toFloat()); black = false }
                continue
            }
            val margin = range * 0.1f
            val flips = if (black) difference > margin else difference < -margin
            if (flips) {
                if (edges.isEmpty()) firstEdgeToBlack = !black
                edges.add(lastCrossing)
                black = !black
            }
        }
        if (edges.size < 20) return null
        val runs = FloatArray(edges.size - 1) { edges[it + 1] - edges[it] }
        // The stretch after the first edge begins with a bar when that edge went to black.
        return runs to firstEdgeToBlack
    }

    private fun slidingExtreme(values: FloatArray, window: Int, wantMax: Boolean): FloatArray {
        val n = values.size
        val result = FloatArray(n)
        val deque = IntArray(n)
        var head = 0
        var tail = 0
        val half = window / 2
        var next = 0
        for (i in 0 until n) {
            val last = min(n - 1, i + half)
            while (next <= last) {
                while (tail > head && (if (wantMax) values[deque[tail - 1]] <= values[next] else values[deque[tail - 1]] >= values[next])) tail--
                deque[tail++] = next
                next++
            }
            while (deque[head] < i - half) head++
            result[i] = values[deque[head]]
        }
        return result
    }

    /** Looks for a barcode among the bars and spaces of one line. [firstIsBar] says whether the first width is a bar. */
    private fun decodeRuns(runs: FloatArray, firstIsBar: Boolean): Read? {
        for (start in runs.indices) {
            // A barcode begins with a bar, after a stretch of white.
            if ((start % 2 == 0) != firstIsBar) continue
            decodeAt(runs, start, 13)?.let { return it }
            decodeAt(runs, start, 8)?.let { return it }
        }
        return null
    }

    private fun decodeAt(runs: FloatArray, s: Int, length: Int): Read? {
        val half = length / 2
        val total = 3 + 4 * half + 5 + 4 * half + 3
        if (s + total > runs.size) return null
        val unit = (runs[s] + runs[s + 1] + runs[s + 2]) / 3
        // The white before the first bar is a run unless the line began in it, which the picture's edge cut off.
        if (unit < 1f || !isGuard(runs, s, 3, unit) || (s > 0 && runs[s - 1] < unit * 3)) return null

        var at = s + 3
        val left = StringBuilder()
        val digits = IntArray(length)
        var errorSum = 0f
        var parity = ""
        for (i in 0 until half) {
            val (digit, code, error) = matchDigit(runs, at, if (length == 13) leftPatterns else leftPatterns.filter { it.code == 'L' }) ?: return null
            errorSum += error
            digits[(if (length == 13) 1 else 0) + i] = digit
            parity += code
            left.append(digit)
            at += 4
        }
        if (!isGuard(runs, at, 5, unit)) return null
        at += 5
        for (i in 0 until half) {
            val (digit, _, error) = matchDigit(runs, at, rightPatterns) ?: return null
            errorSum += error
            digits[(if (length == 13) 1 else 0) + half + i] = digit
            at += 4
        }
        if (!isGuard(runs, at, 3, unit)) return null

        if (length == 13) {
            val first = PARITY.indexOf(parity)
            if (first < 0) return null
            digits[0] = first
        }
        val text = digits.joinToString("")
        // A UPC-A is an EAN-13 with a first digit of 0, and is given as its twelve digits.
        val result = if (length == 13 && text.startsWith("0")) text.substring(1) else text
        return BarcodeNumber.parse(result)?.let { Read(it, errorSum / (2 * half)) }
    }

    private fun isGuard(runs: FloatArray, from: Int, count: Int, unit: Float): Boolean =
        (0 until count).all { runs[from + it] in unit * 0.4f..unit * 2.2f }

    // The digit whose bars and spaces the four widths at [at] fit best, with the code it came from, or null when none fits.
    private fun matchDigit(runs: FloatArray, at: Int, patterns: List<Pattern>): Triple<Int, Char, Float>? {
        val total = runs[at] + runs[at + 1] + runs[at + 2] + runs[at + 3]
        if (total <= 0f) return null
        val module = total / 7
        val a = runs[at] / module
        val b = runs[at + 1] / module
        val c = runs[at + 2] / module
        val d = runs[at + 3] / module
        var best: Pattern? = null
        var bestError = Float.MAX_VALUE
        for (p in patterns) {
            val e = p.runs
            // The distance from one edge to the next of the same kind does not change when blur fattens the bars,
            // so it counts most. The widths themselves count a little, to tell the codes apart where those agree.
            val edgeToEdge = abs((a + b) - (e[0] + e[1])) + abs((b + c) - (e[1] + e[2])) + abs((c + d) - (e[2] + e[3]))
            val widths = abs(a - e[0]) + abs(b - e[1]) + abs(c - e[2]) + abs(d - e[3])
            val error = edgeToEdge + 0.35f * widths
            if (error < bestError) { bestError = error; best = p }
        }
        return best?.takeIf { bestError <= MAX_DIGIT_ERROR }?.let { Triple(it.digit, it.code, bestError) }
    }
}
