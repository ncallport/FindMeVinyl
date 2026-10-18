package com.nickallport.findmevinyl.discography

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

    private suspend fun saveAlbum(userFormats: String? = null, userNote: String? = null, formats: String? = "CD") =
        database.albumDao().insert(
            AlbumEntity(
                "1", "artist-1", "Olympia", "STUDIO_ALBUM", "2010-10-20", owned = true, wanted = false,
                formats = formats, formatsCheckedAt = 1_800_000_000_000L, userFormats = userFormats, userNote = userNote
            )
        )

    @Test
    fun `an album comes with the formats and note the user recorded`() = runBlocking {
        saveAlbum(userFormats = "LP", userNote = "BMG vinyl")

        val album = viewModel.loadAlbum("1")!!

        assertEquals(listOf("CD"), album.formats)
        assertEquals(listOf("LP"), album.userFormats)
        assertEquals("BMG vinyl", album.note)
    }

    @Test
    fun `an album with no details of a copy has none`() = runBlocking {
        saveAlbum()

        val album = viewModel.loadAlbum("1")!!

        assertEquals(emptyList<String>(), album.userFormats)
        assertNull(album.note)
    }

    @Test
    fun `the discography list carries them too`() = runBlocking {
        saveAlbum(userFormats = "LP", userNote = "BMG vinyl")

        val album = (viewModel.loadSaved("artist-1")).groupedAlbums.getValue(DiscographyCategory.STUDIO_ALBUM).single()

        assertEquals(listOf("LP"), album.userFormats)
        assertEquals("BMG vinyl", album.note)
    }

    @Test
    fun `saving a copy stores the formats and the trimmed note`() = runBlocking {
        saveAlbum()

        viewModel.saveCopy("1", listOf("CD", "LP"), "  BMG vinyl, 2018  ")

        val saved = database.albumDao().getById("1")!!
        assertEquals("LP|CD", saved.userFormats)
        assertEquals("BMG vinyl, 2018", saved.userNote)
        assertEquals("CD", saved.formats)
    }

    @Test
    fun `a blank note is not kept, and nor are labels that are not formats`() = runBlocking {
        saveAlbum()

        viewModel.saveCopy("1", listOf("LP", "nonsense"), "   ")

        val saved = database.albumDao().getById("1")!!
        assertEquals("LP", saved.userFormats)
        assertNull(saved.userNote)
    }

    @Test
    fun `saving no formats and no note clears the copy details`() = runBlocking {
        saveAlbum(userFormats = "LP", userNote = "BMG vinyl")

        viewModel.saveCopy("1", emptyList(), "")

        val saved = database.albumDao().getById("1")!!
        assertNull(saved.userFormats)
        assertNull(saved.userNote)
    }

    @Test
    fun `the note is cut to a sensible length`() = runBlocking {
        saveAlbum()

        viewModel.saveCopy("1", emptyList(), "x".repeat(2000))

        assertEquals(500, database.albumDao().getById("1")!!.userNote!!.length)
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

    private suspend fun wantedAlbum(owned: Boolean = true, userFormats: String? = null) =
        database.albumDao().insert(
            AlbumEntity(
                "1", "artist-1", "Desire", "STUDIO_ALBUM", "1976", owned = owned, wanted = true,
                userFormats = userFormats
            )
        )

    @Test
    fun `an album can be wanted on a format, and that shows on the saved album`() = runBlocking {
        wantedAlbum(userFormats = "CD")

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
        wantedAlbum(userFormats = "CD")
        viewModel.saveWantFormat("1", "LP")

        viewModel.saveCopy("1", listOf("CD", "LP"), "")

        val saved = viewModel.loadAlbum("1")!!
        assertEquals(false, saved.wanted)
        assertNull(saved.wantFormat)
        assertEquals(listOf("LP", "CD"), saved.userFormats)
    }

    @Test
    fun `saying the copy is on another format keeps the want`() = runBlocking {
        wantedAlbum(userFormats = "CD")
        viewModel.saveWantFormat("1", "LP")

        viewModel.saveCopy("1", listOf("CD", "MC"), "")

        val saved = viewModel.loadAlbum("1")!!
        assertEquals(true, saved.wanted)
        assertEquals("LP", saved.wantFormat)
    }

    @Test
    fun `an album wanted on any format keeps its want when a format is added to the copy`() = runBlocking {
        wantedAlbum()

        viewModel.saveCopy("1", listOf("LP"), "")

        assertEquals(true, viewModel.loadAlbum("1")!!.wanted)
    }
}
