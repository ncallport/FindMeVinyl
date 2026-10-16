package com.nickallport.findmevinyl.shops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Opening hours as people write them, turned into the form OpenStreetMap uses, which the app reads. */
class FriendlyHoursTest {

    private fun osm(text: String) = FriendlyHours.toOsm(text)

    @Test
    fun `hours already in the OpenStreetMap form are kept as they are`() {
        assertEquals("Mo-Sa 11:00-19:30; Su,PH 15:00-19:30", osm("Mo-Sa 11:00-19:30; Su,PH 15:00-19:30"))
        assertEquals("Mo-Sa 11:00-12:30, 13:30-19:00", osm("  Mo-Sa 11:00-12:30, 13:30-19:00 "))
    }

    @Test
    fun `short day names and 24 hour times`() {
        assertEquals("Mo-Sa 10:00-19:00", osm("Mon-Sat 10:00-19:00"))
    }

    @Test
    fun `long day names, with to, and times with am and pm`() {
        assertEquals("Mo-Sa 10:00-19:00", osm("Monday to Saturday 10am-7pm"))
        assertEquals("Mo-Sa 10:00-19:00", osm("monday - saturday 10:00 am - 7:00 pm"))
    }

    @Test
    fun `hours with no minutes`() {
        assertEquals("Mo-Fr 10:00-18:00", osm("Mon-Fri 10-18"))
    }

    @Test
    fun `an end time that is smaller than the start is taken as the afternoon`() {
        assertEquals("Mo-Fr 09:00-17:00", osm("weekdays 9-5"))
        assertEquals("Mo-Sa 10:00-19:00", osm("Mon-Sat 10-7"))
    }

    @Test
    fun `several days with different hours, and a day that is shut`() {
        assertEquals("Mo-Fr 10:00-18:00; Sa 10:00-14:00; Su off", osm("Mon-Fri 10-18; Sat 10-14; Sun closed"))
    }

    @Test
    fun `rules on separate lines`() {
        assertEquals("Mo-Fr 10:00-18:00; Sa 10:00-14:00", osm("Mon-Fri 10-18\nSat 10-14"))
    }

    @Test
    fun `two spells of opening in a day`() {
        assertEquals("Tu-Sa 11:00-13:00, 14:30-19:30", osm("Tue-Sat 11:00-13:00, 14:30-19:30"))
        assertEquals("Tu-Sa 11:00-13:00, 14:30-19:30", osm("Tue-Sat 11-1pm and 2:30pm-7:30pm"))
    }

    @Test
    fun `daily, every day and no days at all mean every day`() {
        assertEquals("Mo-Su 09:00-17:00", osm("daily 9-17"))
        assertEquals("Mo-Su 09:00-17:00", osm("every day 9am-5pm"))
        assertEquals("Mo-Su 10:00-18:00", osm("10-18"))
    }

    @Test
    fun `weekdays and weekends`() {
        assertEquals("Mo-Fr 09:00-17:00", osm("weekdays 9-17"))
        assertEquals("Sa-Su 10:00-16:00", osm("weekends 10am - 4pm"))
    }

    @Test
    fun `a list of days`() {
        assertEquals("Mo,We,Fr 10:00-18:00", osm("Mon, Wed, Fri 10-6"))
        assertEquals("Mo,We 10:00-18:00", osm("Mon & Wed 10-6"))
    }

    @Test
    fun `minutes written with a dot`() {
        assertEquals("Mo-Fr 09:30-17:30", osm("Mon-Fri 9.30-17.30"))
    }

    @Test
    fun `a range of days that runs over the weekend`() {
        assertEquals("Sa-Mo 10:00-18:00", osm("Sat-Mon 10-18"))
    }

    @Test
    fun `everything is shut`() {
        assertEquals("Mo-Su off", osm("Mon-Sun closed"))
    }

    @Test
    fun `what the app reads comes out of it`() {
        assertNotNull(OpeningHours.parse(osm("Monday to Saturday 10am-7pm; Sunday closed")!!))
    }

    @Test
    fun `text that is not hours gives nothing`() {
        assertNull(osm("mornings"))
        assertNull(osm("by appointment"))
        assertNull(osm("Mon-Fri 25-30"))
        assertNull(osm("Mon-Fri 10-"))
        assertNull(osm("Funday 10-18"))
        assertNull(osm(""))
        assertNull(osm("   "))
    }

    @Test
    fun `one rule that cannot be read makes the whole text unreadable, not half of it`() {
        assertNull(osm("Mon-Fri 10-18; whenever"))
    }
}
