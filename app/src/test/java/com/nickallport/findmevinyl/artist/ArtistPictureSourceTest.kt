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

    // A group of artists.

    private val first = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    @Test
    fun `a group shows the photo the user chose for the group`() = runBlocking {
        val source = ArtistPictureSource(
            userPhoto = { id -> if (id == "group/1") photo else first },
            webPicture = { web },
            firstMemberOf = { "cave" }
        )

        assertSame(photo, source.load("group/1"))
    }

    @Test
    fun `a group with no photo of its own shows its first member's photo`() = runBlocking {
        val source = ArtistPictureSource(
            userPhoto = { id -> if (id == "cave") first else null },
            webPicture = { web },
            firstMemberOf = { "cave" }
        )

        assertSame(first, source.load("group/1"))
    }

    @Test
    fun `a group with no photos shows the web picture of its first member`() = runBlocking {
        val source = ArtistPictureSource(
            userPhoto = { null },
            webPicture = { id -> if (id == "cave") web else null },
            firstMemberOf = { "cave" }
        )

        assertSame(web, source.load("group/1"))
    }

    @Test
    fun `the web is never asked about the id of a group`() = runBlocking {
        val asked = mutableListOf<String>()
        val source = ArtistPictureSource(
            userPhoto = { null },
            webPicture = { id -> asked.add(id); null },
            firstMemberOf = { "cave" }
        )

        source.load("group/1")

        assertEquals(listOf("cave"), asked)
    }

    @Test
    fun `a group that cannot be found, or has no members, gives nothing`() = runBlocking {
        val asked = mutableListOf<String>()
        val source = ArtistPictureSource(
            userPhoto = { null }, webPicture = { id -> asked.add(id); web }, firstMemberOf = { null }
        )

        assertNull(source.load("group/none"))
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `an ordinary artist is looked up as before`() = runBlocking {
        val source = ArtistPictureSource(userPhoto = { null }, webPicture = { web }, firstMemberOf = { "never" })

        assertSame(web, source.load("1c70a3fc-fe03-4b23-ba9c-873c33dcd814"))
    }

    // A bigger picture for the header of an artist's page.

    @Test
    fun `a large picture asks the web for the large one, and not for the small one`() = runBlocking {
        val smallAsked = mutableListOf<String>()
        val source = ArtistPictureSource(
            userPhoto = { null }, webPicture = { id -> smallAsked.add(id); web }, largeWebPicture = { large }
        )

        assertSame(large, source.load("a", large = true))
        assertEquals(emptyList<String>(), smallAsked)
        assertSame(web, source.load("a"))
    }

    @Test
    fun `the photo the user chose is shown for a large picture too, and the web is not asked`() = runBlocking {
        val asked = mutableListOf<String>()
        val source = ArtistPictureSource(
            userPhoto = { photo }, webPicture = { web }, largeWebPicture = { id -> asked.add(id); large }
        )

        assertSame(photo, source.load("a", large = true))
        assertEquals(emptyList<String>(), asked)
    }

    @Test
    fun `without a large source a large picture is the usual one`() = runBlocking {
        val source = ArtistPictureSource(userPhoto = { null }, webPicture = { web })

        assertSame(web, source.load("a", large = true))
    }

    @Test
    fun `a group shows its first member's large picture`() = runBlocking {
        val source = ArtistPictureSource(
            userPhoto = { null }, webPicture = { web }, largeWebPicture = { id -> if (id == "first") large else null },
            firstMemberOf = { "first" }
        )

        assertSame(large, source.load("group/1", large = true))
    }

    private val large = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)

    @Test
    fun `Various Artists and Soundtracks show the photo the user chose, and never ask the web`() = runBlocking {
        val asked = mutableListOf<String>()
        val withPhoto = ArtistPictureSource(userPhoto = { photo }, webPicture = { id -> asked.add(id); web })
        val withoutPhoto = ArtistPictureSource(userPhoto = { null }, webPicture = { id -> asked.add(id); web })

        assertSame(photo, withPhoto.load(com.nickallport.findmevinyl.favourites.SpecialArtists.VARIOUS_ID))
        assertNull(withoutPhoto.load(com.nickallport.findmevinyl.favourites.SpecialArtists.VARIOUS_ID))
        assertNull(withoutPhoto.load(com.nickallport.findmevinyl.favourites.SpecialArtists.SOUNDTRACKS_ID, large = true))
        assertEquals(emptyList<String>(), asked)
    }
}
