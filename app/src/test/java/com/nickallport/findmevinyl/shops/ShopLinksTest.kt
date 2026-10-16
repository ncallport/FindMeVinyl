package com.nickallport.findmevinyl.shops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopLinksTest {

    @Test
    fun `a web address with a scheme is kept as it is`() {
        assertEquals("https://www.louielouie.biz/", WebLink.normalise("https://www.louielouie.biz/"))
        assertEquals("http://www.carbono.com.pt", WebLink.normalise("http://www.carbono.com.pt"))
    }

    @Test
    fun `a web address with no scheme gets https`() {
        assertEquals("https://louielouie.biz", WebLink.normalise("louielouie.biz"))
        assertEquals("https://www.carbono.com.pt/shop", WebLink.normalise("  www.carbono.com.pt/shop  "))
    }

    @Test
    fun `anything that is not a web address is refused`() {
        assertNull(WebLink.normalise("javascript:alert(1)"))
        assertNull(WebLink.normalise("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(WebLink.normalise("ftp://files.example.com"))
        assertNull(WebLink.normalise("file:///sdcard/x"))
        assertNull(WebLink.normalise("tel:+351213472232"))
        assertNull(WebLink.normalise("not a web address"))
        assertNull(WebLink.normalise(""))
        assertNull(WebLink.normalise("   "))
    }

    @Test
    fun `a phone number becomes a dial link with only digits and a leading plus`() {
        assertEquals("tel:+351213472232", PhoneLink.dialUri("+351 213 472 232"))
        assertEquals("tel:02071234567", PhoneLink.dialUri("(020) 7123 4567"))
        assertEquals("tel:+442071234567", PhoneLink.dialUri("+44-20-7123-4567"))
    }

    @Test
    fun `a phone number with letters, too few digits or nothing is refused`() {
        assertNull(PhoneLink.dialUri("call us"))
        assertNull(PhoneLink.dialUri("123"))
        assertNull(PhoneLink.dialUri(""))
    }

    @Test
    fun `only the first of several numbers is used`() {
        assertEquals("tel:+351213472232", PhoneLink.dialUri("+351 213 472 232; +351 213 423 757"))
    }

    @Test
    fun `an email address becomes a mail link, and one that is not an address is refused`() {
        assertEquals("mailto:louielouie.lisboa@gmail.com", EmailLink.mailUri(" louielouie.lisboa@gmail.com "))
        assertNull(EmailLink.mailUri("no address here"))
        assertNull(EmailLink.mailUri("a@b"))
        assertNull(EmailLink.mailUri("javascript:alert(1)@x.com\nBcc: x@y.com"))
    }
}
