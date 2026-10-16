package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reading what Google Maps gives when a place is shared: its name, its address, and a link. */
class SharedPlaceParserTest {

    private val link = "https://maps.app.goo.gl/Q4Xy2CQNv29QYFyCA"

    @Test
    fun `reads the name, the address and the link from what the Maps app shares`() {
        val shared = "Silver Tentacle Merchandising\nR. Raul Proença 56a, 2500 Caldas da Rainha\n$link"

        val place = SharedPlaceParser.parse(shared)

        assertEquals("Silver Tentacle Merchandising", place.name)
        assertEquals("R. Raul Proença 56a, 2500 Caldas da Rainha", place.address)
        assertEquals(link, place.link)
        assertNull(place.position)
    }

    @Test
    fun `a bare link has a link and nothing else`() {
        val place = SharedPlaceParser.parse("  $link \n")

        assertEquals(link, place.link)
        assertNull(place.name)
        assertNull(place.address)
    }

    @Test
    fun `a name and a link with no address`() {
        val place = SharedPlaceParser.parse("Silver Tentacle\n$link")

        assertEquals("Silver Tentacle", place.name)
        assertNull(place.address)
    }

    @Test
    fun `a name on the same line as the link`() {
        val place = SharedPlaceParser.parse("Silver Tentacle $link")

        assertEquals("Silver Tentacle", place.name)
        assertEquals(link, place.link)
    }

    @Test
    fun `reads coordinates that are pasted`() {
        assertEquals(Coordinates(39.4036, -9.1349), SharedPlaceParser.parse("39.4036, -9.1349").position)
        assertEquals(Coordinates(39.4036, -9.1349), SharedPlaceParser.parse("39.4036,-9.1349").position)
        assertEquals(Coordinates(-33.8688, 151.2093), SharedPlaceParser.parse("-33.8688 , 151.2093").position)
    }

    @Test
    fun `a pair of coordinates is not taken for a name or an address`() {
        val place = SharedPlaceParser.parse("39.4036, -9.1349")

        assertNull(place.name)
        assertNull(place.address)
    }

    @Test
    fun `numbers in an address are not taken for coordinates`() {
        val place = SharedPlaceParser.parse("Shop\nR. Raul Proença 56a, 2500 Caldas da Rainha")

        assertNull(place.position)
    }

    @Test
    fun `coordinates off the map are refused`() {
        assertNull(SharedPlaceParser.parse("139.4036, -9.1349").position)
        assertNull(SharedPlaceParser.parse("39.4036, -209.1349").position)
    }

    @Test
    fun `empty text gives an empty place`() {
        val place = SharedPlaceParser.parse("   ")

        assertNull(place.name)
        assertNull(place.address)
        assertNull(place.link)
        assertNull(place.position)
    }

    // The address of a link to a place.

    private val placeUrl = "https://www.google.com/maps/place/Silver+Tentacle+Merchandising,+R.+Raul+Proen%C3%A7a+56a+2500,+" +
        "Caldas+da+Rainha/data=!4m2!3m1!1s0xd18b3af89d058fd:0x5c08b52c7542fd99!18m1!1e1?utm_source=mstt_1&entry=gps"

    @Test
    fun `reads the name and address from the address of a place link`() {
        val place = SharedPlaceParser.fromPlaceUrl(placeUrl)!!

        assertEquals("Silver Tentacle Merchandising", place.name)
        assertEquals("R. Raul Proença 56a 2500, Caldas da Rainha", place.address)
    }

    @Test
    fun `a place link with only a name has no address`() {
        val place = SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps/place/Silver+Tentacle/data=!4m2")!!

        assertEquals("Silver Tentacle", place.name)
        assertNull(place.address)
    }

    @Test
    fun `reads the position from a link that has one`() {
        assertEquals(
            Coordinates(39.4036, -9.1349),
            SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps/place/X/@39.4100,-9.1300,17z/data=!3d39.4036!4d-9.1349")!!.position
        )
        assertEquals(
            Coordinates(39.41, -9.13),
            SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps/place/X/@39.4100,-9.1300,17z/data=!4m2")!!.position
        )
        assertEquals(
            Coordinates(39.4, -9.1),
            SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps?q=39.4,-9.1")!!.position
        )
        assertEquals(
            Coordinates(39.4, -9.1),
            SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps/search/?api=1&query=39.4%2C-9.1")!!.position
        )
    }

    @Test
    fun `the place itself, not the middle of the map on screen, is the position when both are there`() {
        val place = SharedPlaceParser.fromPlaceUrl("https://www.google.com/maps/place/X/@39.5,-9.5,15z/data=!3d39.4036!4d-9.1349")!!

        assertEquals(Coordinates(39.4036, -9.1349), place.position)
    }

    @Test
    fun `a consent page for a link is followed to the link it carries`() {
        val consent = "https://consent.google.com/m?continue=" +
            java.net.URLEncoder.encode(placeUrl, "UTF-8") + "&gl=PT&m=0"

        val place = SharedPlaceParser.fromPlaceUrl(consent)!!

        assertEquals("Silver Tentacle Merchandising", place.name)
    }

    @Test
    fun `a link that is not to a place gives nothing`() {
        assertNull(SharedPlaceParser.fromPlaceUrl("https://www.google.com/search?q=silver+tentacle&kgmid=/g/11bxdyxlkp"))
        assertNull(SharedPlaceParser.fromPlaceUrl("https://example.com/maps/place/X"))
        assertNull(SharedPlaceParser.fromPlaceUrl("not a link"))
    }

    // Which links to follow.

    @Test
    fun `only Google Maps links are followed`() {
        assertTrue(SharedPlaceParser.isMapsLink("https://maps.app.goo.gl/Q4Xy2CQNv29QYFyCA"))
        assertTrue(SharedPlaceParser.isMapsLink("https://goo.gl/maps/abc"))
        assertTrue(SharedPlaceParser.isMapsLink("https://www.google.com/maps/place/X"))
        assertTrue(SharedPlaceParser.isMapsLink("https://maps.google.com/?q=x"))
        assertFalse(SharedPlaceParser.isMapsLink("http://maps.app.goo.gl/x"))
        assertFalse(SharedPlaceParser.isMapsLink("https://evil.example.com/maps.app.goo.gl"))
        assertFalse(SharedPlaceParser.isMapsLink("https://maps.app.goo.gl.evil.com/x"))
        assertFalse(SharedPlaceParser.isMapsLink("https://share.google/dagohLqvHbUG372IS"))
        assertFalse(SharedPlaceParser.isMapsLink("javascript:alert(1)"))
    }
}
