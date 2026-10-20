package com.nickallport.findmevinyl.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeNumberTest {

    @Test
    fun `a valid EAN-13 is kept as it is`() {
        assertEquals("5012985305020", BarcodeNumber.parse("5012985305020"))
    }

    @Test
    fun `spaces and dashes are taken out`() {
        assertEquals("5012985305020", BarcodeNumber.parse(" 5 012985 305020 "))
        assertEquals("5012985305020", BarcodeNumber.parse("5-012985-305020"))
    }

    @Test
    fun `a valid UPC-A of twelve digits is kept as twelve digits`() {
        assertEquals("724384746025", BarcodeNumber.parse("724384746025"))
    }

    @Test
    fun `a valid EAN-8 is accepted`() {
        assertEquals("96385074", BarcodeNumber.parse("96385074"))
    }

    @Test
    fun `a wrong check digit is refused`() {
        assertNull(BarcodeNumber.parse("5012985305021"))
        assertNull(BarcodeNumber.parse("724384746026"))
    }

    @Test
    fun `other lengths and letters are refused`() {
        assertNull(BarcodeNumber.parse("50129853050"))
        assertNull(BarcodeNumber.parse("50129853050200"))
        assertNull(BarcodeNumber.parse("501298530502A"))
        assertNull(BarcodeNumber.parse(""))
        assertNull(BarcodeNumber.parse("   "))
    }
}
