package com.nickallport.findmevinyl.discography

import com.nickallport.findmevinyl.ui.discography.AlbumCopy
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** What the user records about their own copy of an album, and how it reaches the screens. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelCopyTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            throttle = MusicBrainzThrottle(minIntervalMillis = 0), clock = { 1_800_000_000_000L }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private suspend fun saveAlbum(formats: String? = "CD", owned: Boolean = true) =
        database.albumDao().insert(
            AlbumEntity(
                "1", "artist-1", "Olympia", "STUDIO_ALBUM", "2010-10-20", owned = owned, wanted = false,
                formats = formats, formatsCheckedAt = 1_800_000_000_000L
            )
        )

    private suspend fun formatsAndNotes() = database.albumDao().getCopies("1").map { it.format to it.note }

    @Test
    fun `an album comes with the copies the user recorded`() = runBlocking {
        saveAlbum()
        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "green cover"), AlbumCopy("CD")))

        val album = viewModel.loadAlbum("1")!!

        assertEquals(listOf("CD"), album.formats)
        assertEquals(listOf(AlbumCopy("LP", "green cover"), AlbumCopy("CD")), album.copies)
        assertEquals(listOf("LP", "CD"), album.userFormats)
    }

    @Test
    fun `an album with no copies has none`() = runBlocking {
        saveAlbum()

        val album = viewModel.loadAlbum("1")!!

        assertEquals(emptyList<AlbumCopy>(), album.copies)
        assertEquals(emptyList<String>(), album.userFormats)
    }

    @Test
    fun `the formats of the copies are each listed once, in the usual order`() = runBlocking {
        saveAlbum()
        viewModel.saveCopies("1", listOf(AlbumCopy("CD"), AlbumCopy("LP", "green"), AlbumCopy("LP", "blue")))

        assertEquals(listOf("LP", "CD"), viewModel.loadAlbum("1")!!.userFormats)
    }

    @Test
    fun `the discography list carries the copies too`() = runBlocking {
        saveAlbum()
        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "BMG vinyl")))

        val album = (viewModel.loadSaved("artist-1")).groupedAlbums.getValue(DiscographyCategory.STUDIO_ALBUM).single()

        assertEquals(listOf(AlbumCopy("LP", "BMG vinyl")), album.copies)
        assertEquals(listOf("LP"), album.userFormats)
    }

    @Test
    fun `saving copies stores them in order, with trimmed notes, several on one format`() = runBlocking {
        saveAlbum()

        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "  green cover "), AlbumCopy("CD", "from Mum"), AlbumCopy("LP", "blue")))

        assertEquals(listOf("LP" to "green cover", "CD" to "from Mum", "LP" to "blue"), formatsAndNotes())
        assertEquals("CD", database.albumDao().getById("1")!!.formats)
    }

    @Test
    fun `a blank description is not kept, and nor are copies on something that is not a format`() = runBlocking {
        saveAlbum()

        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "   "), AlbumCopy("nonsense", "x")))

        assertEquals(listOf("LP" to null), formatsAndNotes())
    }

    @Test
    fun `saving again replaces the copies kept before`() = runBlocking {
        saveAlbum()
        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "green"), AlbumCopy("CD")))

        viewModel.saveCopies("1", listOf(AlbumCopy("MC", "tape")))

        assertEquals(listOf("MC" to "tape"), formatsAndNotes())
    }

    @Test
    fun `saving no copies clears them`() = runBlocking {
        saveAlbum()
        viewModel.saveCopies("1", listOf(AlbumCopy("LP")))

        viewModel.saveCopies("1", emptyList())

        assertEquals(emptyList<Pair<String, String?>>(), formatsAndNotes())
    }

    @Test
    fun `saving the copies of one album leaves another album's copies alone`() = runBlocking {
        saveAlbum()
        database.albumDao().insert(AlbumEntity("2", "artist-1", "Other", "STUDIO_ALBUM", "2011", owned = true, wanted = false))
        viewModel.saveCopies("2", listOf(AlbumCopy("CD", "two")))

        viewModel.saveCopies("1", listOf(AlbumCopy("LP")))

        assertEquals(listOf(AlbumCopy("CD", "two")), viewModel.loadAlbum("2")!!.copies)
    }

    @Test
    fun `the description of a copy is cut to a sensible length`() = runBlocking {
        saveAlbum()

        viewModel.saveCopies("1", listOf(AlbumCopy("LP", "y".repeat(2000))))

        assertEquals(500, database.albumDao().getCopies("1").single().note!!.length)
    }

    @Test
    fun `no more than twenty copies are kept`() = runBlocking {
        saveAlbum()

        viewModel.saveCopies("1", List(25) { AlbumCopy("LP", "copy $it") })

        assertEquals(20, database.albumDao().getCopies("1").size)
    }

    // The pressing the user chose as theirs.

    private val portuguese = Pressing("release-9", "1989-05-01", "PT", "Charisma", "CHA 1", "12\" Vinyl")

    @Test
    fun `an album comes with the pressing the user chose`() = runBlocking {
        saveAlbum()
        database.albumDao().setMyPressing("1", "release-9", "1989-05-01, Portugal")

        val album = viewModel.loadAlbum("1")!!

        assertEquals("release-9", album.myPressingId)
        assertEquals("1989-05-01, Portugal", album.myPressingText)
    }

    @Test
    fun `choosing a pressing keeps its id and a line that describes it`() = runBlocking {
        saveAlbum()

        viewModel.saveMyPressing("1", portuguese)

        val saved = database.albumDao().getById("1")!!
        assertEquals("release-9", saved.myPressingId)
        assertEquals("1989-05-01, Portugal, Charisma, CHA 1, 12\" Vinyl", saved.myPressingText)
    }

    @Test
    fun `choosing no pressing takes it away again`() = runBlocking {
        saveAlbum()
        viewModel.saveMyPressing("1", portuguese)

        viewModel.saveMyPressing("1", null)

        assertNull(database.albumDao().getById("1")!!.myPressingId)
        assertNull(database.albumDao().getById("1")!!.myPressingText)
    }

    @Test
    fun `the discography list carries the chosen pressing too`() = runBlocking {
        saveAlbum()
        viewModel.saveMyPressing("1", portuguese)

        val album = viewModel.loadSaved("artist-1").groupedAlbums.getValue(DiscographyCategory.STUDIO_ALBUM).single()

        assertEquals("release-9", album.myPressingId)
    }

    // The format an album is wanted on.

    private suspend fun wantedAlbum(owned: Boolean = true, copyFormat: String? = null) {
        database.albumDao().insert(AlbumEntity("1", "artist-1", "Desire", "STUDIO_ALBUM", "1976", owned = owned, wanted = true))
        if (copyFormat != null) viewModel.saveCopies("1", listOf(AlbumCopy(copyFormat)))
    }

    @Test
    fun `an album can be wanted on a format, and that shows on the saved album`() = runBlocking {
        wantedAlbum(copyFormat = "CD")

        viewModel.saveWantFormat("1", "LP")

        assertEquals("LP", viewModel.loadAlbum("1")!!.wantFormat)
    }

    @Test
    fun `a format that is not a label is not kept`() = runBlocking {
        wantedAlbum()

        viewModel.saveWantFormat("1", "Betamax")

        assertNull(viewModel.loadAlbum("1")!!.wantFormat)
    }

    @Test
    fun `an album that is not wanted cannot be wanted on a format`() = runBlocking {
        database.albumDao().insert(AlbumEntity("1", "artist-1", "Desire", "STUDIO_ALBUM", "1976", owned = true, wanted = false))

        viewModel.saveWantFormat("1", "LP")

        assertNull(viewModel.loadAlbum("1")!!.wantFormat)
    }

    @Test
    fun `null goes back to wanting it on any format`() = runBlocking {
        wantedAlbum()
        viewModel.saveWantFormat("1", "LP")

        viewModel.saveWantFormat("1", null)

        assertNull(viewModel.loadAlbum("1")!!.wantFormat)
    }

    @Test
    fun `saying the copy is on the format it was wanted on takes the want off`() = runBlocking {
        wantedAlbum(copyFormat = "CD")
        viewModel.saveWantFormat("1", "LP")

        viewModel.saveCopies("1", listOf(AlbumCopy("CD"), AlbumCopy("LP")))

        val saved = viewModel.loadAlbum("1")!!
        assertEquals(false, saved.wanted)
        assertNull(saved.wantFormat)
        assertEquals(listOf("LP", "CD"), saved.userFormats)
    }

    @Test
    fun `saying the copy is on another format keeps the want`() = runBlocking {
        wantedAlbum(copyFormat = "CD")
        viewModel.saveWantFormat("1", "LP")

        viewModel.saveCopies("1", listOf(AlbumCopy("CD"), AlbumCopy("MC")))

        val saved = viewModel.loadAlbum("1")!!
        assertEquals(true, saved.wanted)
        assertEquals("LP", saved.wantFormat)
    }

    @Test
    fun `an album wanted on any format keeps its want when a format is added to the copy`() = runBlocking {
        wantedAlbum()

        viewModel.saveCopies("1", listOf(AlbumCopy("LP")))

        assertEquals(true, viewModel.loadAlbum("1")!!.wanted)
    }
}
