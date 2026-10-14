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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** The discography as built from the saved albums alone. No request may reach MusicBrainz. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelSavedTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            throttle = MusicBrainzThrottle(minIntervalMillis = 0), clock = { now }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun saved(
        id: String,
        category: String = "STUDIO_ALBUM",
        date: String? = null,
        title: String = "Album $id",
        artistId: String = "artist-1",
        owned: Boolean = false,
        wanted: Boolean = false,
        formats: String? = null,
        checkedAt: Long? = null
    ) = AlbumEntity(
        id = id, artistId = artistId, title = title, category = category, firstReleaseDate = date,
        owned = owned, wanted = wanted, formats = formats, formatsCheckedAt = checkedAt
    )

    private suspend fun save(vararg albums: AlbumEntity) = albums.forEach { database.albumDao().insert(it) }

    private fun grouped() = runBlocking {
        (viewModel.loadSaved("artist-1") as DiscographyUiState.Success).groupedAlbums
    }

    @Test
    fun `groups the saved albums by their stored category`() = runBlocking {
        save(
            saved("a", category = "STUDIO_ALBUM"),
            saved("b", category = "LIVE_ALBUM"),
            saved("c", category = "SINGLE"),
            saved("d", category = "STUDIO_ALBUM")
        )

        val groups = grouped()

        assertEquals(setOf(DiscographyCategory.STUDIO_ALBUM, DiscographyCategory.LIVE_ALBUM, DiscographyCategory.SINGLE), groups.keys)
        assertEquals(listOf("a", "d"), groups.getValue(DiscographyCategory.STUDIO_ALBUM).map { it.id })
    }

    @Test
    fun `sorts each category by release date, undated last, and then by title`() = runBlocking {
        save(
            saved("1", date = "1982-05-31", title = "Avalon"),
            saved("2", date = null, title = "Undated"),
            saved("3", date = "1972-06-16", title = "Roxy Music"),
            saved("4", date = "1982-05-31", title = "Another"),
            saved("5", date = "1975")
        )

        val titles = grouped().getValue(DiscographyCategory.STUDIO_ALBUM).map { it.title }

        assertEquals(listOf("Roxy Music", "Album 5", "Another", "Avalon", "Undated"), titles)
    }

    @Test
    fun `treats an empty release date as undated, which MusicBrainz sends for those albums`() = runBlocking {
        save(
            saved("1", date = "", title = "Empty"),
            saved("2", date = "1972-06-16", title = "Dated"),
            saved("3", date = null, title = "Null")
        )

        val titles = grouped().getValue(DiscographyCategory.STUDIO_ALBUM).map { it.title }

        assertEquals(listOf("Dated", "Empty", "Null"), titles)
    }

    @Test
    fun `carries the owned and wanted flags`() = runBlocking {
        save(saved("a", owned = true), saved("b", wanted = true), saved("c"))

        val albums = grouped().getValue(DiscographyCategory.STUDIO_ALBUM).associateBy { it.id }

        assertEquals(true, albums.getValue("a").owned)
        assertEquals(true, albums.getValue("b").wanted)
        assertEquals(false, albums.getValue("c").owned)
        assertEquals(false, albums.getValue("c").wanted)
    }

    @Test
    fun `turns the stored formats into labels for the category, and marks old ones stale`() = runBlocking {
        save(
            saved("album", formats = "12\" Vinyl|CD", checkedAt = now),
            saved("single", category = "SINGLE", formats = "12\" Vinyl", checkedAt = now - 31 * day),
            saved("never")
        )

        val groups = grouped()

        val album = groups.getValue(DiscographyCategory.STUDIO_ALBUM).first { it.id == "album" }
        assertEquals(listOf("LP", "CD"), album.formats)
        assertEquals(false, album.formatsStale)
        val single = groups.getValue(DiscographyCategory.SINGLE).single()
        assertEquals(listOf("12\""), single.formats)
        assertEquals(true, single.formatsStale)
        assertEquals(null, groups.getValue(DiscographyCategory.STUDIO_ALBUM).first { it.id == "never" }.formats)
    }

    @Test
    fun `leaves out the albums of other artists`() = runBlocking {
        save(saved("mine"), saved("theirs", artistId = "artist-2"))

        assertEquals(listOf("mine"), grouped().values.flatten().map { it.id })
    }

    @Test
    fun `gives an empty discography when nothing is saved`() = runBlocking {
        assertEquals(emptyMap<DiscographyCategory, List<Any>>(), grouped())
    }

    @Test
    fun `puts an album with an unknown stored category under other`() = runBlocking {
        save(saved("a", category = "SOMETHING_NEW"))

        assertEquals(listOf("a"), grouped().getValue(DiscographyCategory.OTHER).map { it.id })
    }

    @Test
    fun `sends no request to MusicBrainz`() = runBlocking {
        save(saved("a"))

        grouped()

        assertEquals(0, server.requestCount)
    }
}
