package com.nickallport.findmevinyl.artist

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtistPictureSourceTest {

    private val photo = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    private val web = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888)

    @Test
    fun `shows the photo the user chose, in place of the picture from the web`() = runBlocking {
        val source = ArtistPictureSource(userPhoto = { photo }, webPicture = { web })

        assertSame(photo, source.load("a"))
    }

    @Test
    fun `does not look for a picture on the web when the user has chosen a photo`() = runBlocking {
        val asked = mutableListOf<String>()
        val source = ArtistPictureSource(userPhoto = { photo }, webPicture = { id -> asked.add(id); web })

        source.load("a")

        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `shows the picture from the web when the user has not chosen a photo`() = runBlocking {
        val source = ArtistPictureSource(userPhoto = { null }, webPicture = { web })

        assertSame(web, source.load("a"))
    }

    @Test
    fun `gives nothing when there is neither`() = runBlocking {
        val source = ArtistPictureSource(userPhoto = { null }, webPicture = { null })

        assertNull(source.load("a"))
    }

    @Test
    fun `asks each source about the artist it was given`() = runBlocking {
        val userAsked = mutableListOf<String>()
        val webAsked = mutableListOf<String>()
        val source = ArtistPictureSource(
            userPhoto = { id -> userAsked.add(id); null },
            webPicture = { id -> webAsked.add(id); null }
        )

        source.load("artist-7")

        assertEquals(listOf("artist-7"), userAsked)
        assertEquals(listOf("artist-7"), webAsked)
    }
}
