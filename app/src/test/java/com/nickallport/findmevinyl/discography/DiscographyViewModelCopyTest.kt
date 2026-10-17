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
}
