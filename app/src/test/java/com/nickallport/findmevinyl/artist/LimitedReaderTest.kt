package com.nickallport.findmevinyl.artist

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class LimitedReaderTest {

    @Test
    fun `reads everything when it is under the limit`() {
        val bytes = ByteArray(100) { it.toByte() }

        assertArrayEquals(bytes, LimitedReader.read(ByteArrayInputStream(bytes), maxBytes = 1_000))
    }

    @Test
    fun `reads a stream that is exactly the limit`() {
        val bytes = ByteArray(1_000) { 7 }

        assertEquals(1_000, LimitedReader.read(ByteArrayInputStream(bytes), maxBytes = 1_000)?.size)
    }

    @Test
    fun `gives nothing for a stream that is one byte over the limit`() {
        assertNull(LimitedReader.read(ByteArrayInputStream(ByteArray(1_001)), maxBytes = 1_000))
    }

    @Test
    fun `an empty stream is an empty array`() {
        assertEquals(0, LimitedReader.read(ByteArrayInputStream(ByteArray(0)), maxBytes = 1_000)?.size)
    }

    @Test
    fun `stops reading soon after the limit, instead of reading a huge stream to the end`() {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int {
                served++
                return 1
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                served += length
                return length
            }
        }

        assertNull(LimitedReader.read(endless, maxBytes = 10_000))

        assertEquals(true, served < 100_000)
    }

    @Test
    fun `gives nothing when the stream cannot be read`() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("gone")
        }

        assertNull(LimitedReader.read(broken, maxBytes = 1_000))
    }
}
