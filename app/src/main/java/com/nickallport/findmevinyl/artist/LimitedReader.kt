package com.nickallport.findmevinyl.artist

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads a stream into memory, but gives up on one that is bigger than a limit instead of filling the phone. */
object LimitedReader {

    /** All the bytes, or null when there are more than [maxBytes] or the stream cannot be read. */
    fun read(input: InputStream, maxBytes: Int): ByteArray? {
        return try {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } catch (e: java.io.IOException) {
            null
        }
    }
}
