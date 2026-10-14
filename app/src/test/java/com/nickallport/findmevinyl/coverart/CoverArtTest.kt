package com.nickallport.findmevinyl.coverart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CoverArtTest {

    @Test
    fun `builds the 250px front cover url for a release group`() {
        assertEquals(
            "https://coverartarchive.org/release-group/aa86c6bb-d1c7-3a03-bcb4-3c6f0a469328/front-250",
            CoverArt.url("aa86c6bb-d1c7-3a03-bcb4-3c6f0a469328")
        )
    }

    @Test
    fun `the loader the app uses is one instance, so its cache outlives the activity`() {
        assertSame(CoverArtLoader.shared, CoverArtLoader.shared)
    }

    @Test
    fun `can ask for a larger front cover`() {
        assertEquals(
            "https://coverartarchive.org/release-group/abc/front-500",
            CoverArt.url("abc", size = 500)
        )
    }

    @Test
    fun `the large cover loader asks for the 500px cover`() {
        assertEquals(
            "http://localhost:1234/release-group/abc/front-500",
            CoverArt.url("abc", baseUrl = "http://localhost:1234", size = CoverArt.LARGE)
        )
    }

    @Test
    fun `uses the base url it is given`() {
        assertEquals(
            "http://localhost:1234/release-group/abc/front-250",
            CoverArt.url("abc", baseUrl = "http://localhost:1234")
        )
    }
}
