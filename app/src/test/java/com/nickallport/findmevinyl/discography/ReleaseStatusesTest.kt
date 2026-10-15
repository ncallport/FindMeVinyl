package com.nickallport.findmevinyl.discography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseStatusesTest {

    @Test
    fun `maps MusicBrainz statuses to the ones the settings know`() {
        assertEquals(ReleaseStatuses.OFFICIAL, ReleaseStatuses.token("Official"))
        assertEquals(ReleaseStatuses.PROMOTION, ReleaseStatuses.token("Promotion"))
        assertEquals(ReleaseStatuses.BOOTLEG, ReleaseStatuses.token("Bootleg"))
    }

    @Test
    fun `withdrawn, cancelled, pseudo-release and a missing status are all other`() {
        assertEquals(ReleaseStatuses.OTHER, ReleaseStatuses.token("Withdrawn"))
        assertEquals(ReleaseStatuses.OTHER, ReleaseStatuses.token("Cancelled"))
        assertEquals(ReleaseStatuses.OTHER, ReleaseStatuses.token("Pseudo-Release"))
        assertEquals(ReleaseStatuses.OTHER, ReleaseStatuses.token(null))
    }

    @Test
    fun `encodes each status once, sorted, joined with a bar`() {
        assertEquals("Bootleg|Official", ReleaseStatuses.encode(listOf("Official", "Bootleg", "Official")))
        assertEquals("", ReleaseStatuses.encode(emptyList()))
    }

    @Test
    fun `decodes what was encoded`() {
        assertEquals(listOf("Bootleg", "Official"), ReleaseStatuses.decode("Bootleg|Official"))
        assertTrue(ReleaseStatuses.decode("").isEmpty())
    }
}
