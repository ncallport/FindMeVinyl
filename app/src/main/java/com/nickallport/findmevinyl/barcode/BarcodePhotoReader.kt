package com.nickallport.findmevinyl.barcode

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Reads the barcode in a photo, given the bytes of the file the camera app left. The photo is shrunk first, because a
 * camera's twelve megapixels would be slow to scan and the bars need no more than a few pixels each: about 1200 pixels on
 * the long side, and, when no barcode is found, 2400 for a barcode that takes only a small part of the photo. The decoder
 * does not mind how the photo is turned, so the orientation note in the file is not read.
 */
object BarcodePhotoReader {
    private val SIZES = listOf(1200, 2400)

    /** The barcode number, or null when the bytes are not a picture or hold no barcode that can be read. */
    fun read(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)

        var previousSample = 0
        for (target in SIZES) {
            var sample = 1
            while (longSide / (sample * 2) >= target) sample *= 2
            // The next size is the same picture when the photo is no bigger than the first.
            if (sample == previousSample) continue
            previousSample = sample
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: continue
            val code = try {
                BarcodeDecoder.decode(luminanceOf(bitmap))
            } finally {
                bitmap.recycle()
            }
            if (code != null) return code
        }
        return null
    }

    private fun luminanceOf(bitmap: Bitmap): LuminanceImage {
        val w = bitmap.width
        val h = bitmap.height
        val colours = IntArray(w * h)
        bitmap.getPixels(colours, 0, w, 0, 0, w, h)
        return LuminanceImage(w, h, IntArray(w * h) {
            val c = colours[it]
            // The usual weights of red, green and blue for how bright they look.
            (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
        })
    }
}
