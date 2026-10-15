package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Makes a photo the user chose small enough to keep: no longer than [DEFAULT_MAX_SIDE] pixels on its
 * longest side, as a JPEG. A phone photo can be 12 megapixels, and an artist's picture is shown at a
 * small size, so the original would only fill the phone and the memory.
 *
 * It keeps the shape of the picture (the screens crop it), turns it upright when the camera noted
 * that it was held sideways (a JPEG says so in its EXIF data, and decoding alone ignores that), and
 * gives a transparent picture a white background, since JPEG has no transparency.
 */
object PhotoScaler {
    const val DEFAULT_MAX_SIDE = 600
    private const val QUALITY = 90

    /** The scaled JPEG, or null when [bytes] are not a picture that can be read. */
    fun scale(bytes: ByteArray, maxSide: Int = DEFAULT_MAX_SIDE): ByteArray? {
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Decode at a fraction of the size when the picture is far bigger than needed.
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSide) sampleSize *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val upright = rotated(decoded, orientationOf(bytes))
        val scaled = shrunk(upright, maxSide)
        return jpegOnWhite(scaled)
    }

    private fun orientationOf(bytes: ByteArray): Int = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun rotated(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun shrunk(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val factor = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun jpegOnWhite(bitmap: Bitmap): ByteArray {
        val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        return ByteArrayOutputStream().also { flat.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }.toByteArray()
    }
}
