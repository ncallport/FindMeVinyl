package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.settings.Settings
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.ui.discography.AlbumUiModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Soundtrack albums are a category of their own, found from MusicBrainz's secondary type. */
@RunWith(RobolectricTestRunner::class)
class SoundtrackCategoryTest {

    private fun group(primary: String?, vararg secondary: String) =
        ReleaseGroupDto("id", "Title", primary, secondary.toList(), "1987")

    // The categories.

    @Test
    fun `an album with the secondary type Soundtrack is a soundtrack`() {
        assertEquals(DiscographyCategory.SOUNDTRACK, DiscographyCategorizer.categorize(group("Album", "Soundtrack")))
    }

    @Test
    fun `a soundtrack that is also a compilation or live is still a soundtrack`() {
        assertEquals(DiscographyCategory.SOUNDTRACK, DiscographyCategorizer.categorize(group("Album", "Soundtrack", "Compilation")))
        assertEquals(DiscographyCategory.SOUNDTRACK, DiscographyCategorizer.categorize(group("Album", "Live", "Soundtrack")))
    }

    @Test
    fun `a single or EP from a film stays a single or EP`() {
        assertEquals(DiscographyCategory.SINGLE, DiscographyCategorizer.categorize(group("Single", "Soundtrack")))
        assertEquals(DiscographyCategory.EP, DiscographyCategorizer.categorize(group("EP", "Soundtrack")))
    }

    @Test
    fun `other albums are categorised as before`() {
        assertEquals(DiscographyCategory.STUDIO_ALBUM, DiscographyCategorizer.categorize(group("Album")))
        assertEquals(DiscographyCategory.COMPILATION, DiscographyCategorizer.categorize(group("Album", "Compilation")))
        assertEquals(DiscographyCategory.LIVE_ALBUM, DiscographyCategorizer.categorize(group("Album", "Live")))
    }

    @Test
    fun `no setting hides soundtracks`() {
        val album = AlbumUiModel(id = "1", title = "Who's That Girl", owned = false, wanted = false)

        val result = DiscographyVisibility.apply(
            mapOf(DiscographyCategory.SOUNDTRACK to listOf(album)),
            Settings.ShowEverything.copy(hideLiveAlbums = true, hideCompilations = true, hideSingles = true, hideOtherReleases = true)
        )

        assertEquals(listOf(album), result.shown[DiscographyCategory.SOUNDTRACK])
        assertEquals(0, result.hiddenCount)
    }

    // Saved albums take the category MusicBrainz gives now, and say they have been checked.

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var api: MusicBrainzApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MusicBrainzApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun album(id: String, category: String = "STUDIO_ALBUM", owned: Boolean = false, wanted: Boolean = false, checked: Boolean = false) =
        AlbumEntity(id, "madonna", "Title $id", category, "1987", owned = owned, wanted = wanted, categoryChecked = checked)

    @Test
    fun `an album saved earlier takes its new category when its page is loaded again, keeping the flags`() = runBlocking {
        database.albumDao().insert(album("wtg", owned = true, wanted = true))
        server.enqueue(
            MockResponse().setBody(
                """{ "release-group-count": 1, "release-groups": [ { "id": "wtg", "title": "Who's That Girl", "primary-type": "Album", "secondary-types": ["Soundtrack"], "first-release-date": "1987-07-21" } ] }"""
            )
        )
        val viewModel = DiscographyViewModel(api, database.albumDao(), database.artistDao(), throttle = MusicBrainzThrottle(minIntervalMillis = 0))

        viewModel.loadDiscography("madonna")

        val saved = database.albumDao().getById("wtg")!!
        assertEquals("SOUNDTRACK", saved.category)
        assertTrue(saved.categoryChecked)
        assertTrue(saved.owned)
        assertTrue(saved.wanted)
    }

    @Test
    fun `an album added by hand keeps the kind the user gave it`() = runBlocking {
        database.albumDao().insert(album("custom/1", category = "LIVE_ALBUM", owned = true))

        assertEquals(0, database.albumDao().setCategories(listOf("custom/1"), "SOUNDTRACK"))

        assertEquals("LIVE_ALBUM", database.albumDao().getById("custom/1")!!.category)
    }

    // Finding the category of what is already saved.

    private fun checker() = CategoryChecker(api, database.albumDao(), MusicBrainzThrottle(minIntervalMillis = 0))

    private fun groups(vararg rows: String) = """{ "release-groups": [ ${rows.joinToString(",")} ] }"""

    private fun row(id: String, primary: String, vararg secondary: String) =
        """{ "id": "$id", "title": "T", "primary-type": "$primary", "secondary-types": [${secondary.joinToString(",") { "\"$it\"" }}], "first-release-date": "1987" }"""

    @Test
    fun `owned and wanted albums not checked yet are asked about together, and the soundtracks found`() = runBlocking {
        database.albumDao().insert(album("a", owned = true))
        database.albumDao().insert(album("b", wanted = true))
        database.albumDao().insert(album("c"))
        server.enqueue(MockResponse().setBody(groups(row("a", "Album", "Soundtrack"), row("b", "Album"))))

        checker().run()

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        val query = request.requestUrl!!.queryParameter("query")!!
        assertTrue(query, query.startsWith("rgid:(") && "a" in query && "b" in query && " OR " in query)
        assertFalse(query, query.contains("rgid:(a OR b OR c)"))
        assertEquals(1, server.requestCount)
        assertEquals("SOUNDTRACK", database.albumDao().getById("a")!!.category)
        assertEquals("STUDIO_ALBUM", database.albumDao().getById("b")!!.category)
        assertTrue(database.albumDao().getById("a")!!.categoryChecked)
        assertTrue(database.albumDao().getById("b")!!.categoryChecked)
        assertFalse("an album that is neither owned nor wanted is left", database.albumDao().getById("c")!!.categoryChecked)
    }

    @Test
    fun `an album MusicBrainz no longer lists is marked checked, so it is not asked about again`() = runBlocking {
        database.albumDao().insert(album("gone", owned = true))
        server.enqueue(MockResponse().setBody(groups()))

        checker().run()
        checker().run()

        assertTrue(database.albumDao().getById("gone")!!.categoryChecked)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a failed request changes nothing, so the next run asks again`() = runBlocking {
        database.albumDao().insert(album("a", owned = true))
        server.enqueue(MockResponse().setResponseCode(503))

        checker().run()

        assertFalse(database.albumDao().getById("a")!!.categoryChecked)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a custom album and one already checked are not asked about`() = runBlocking {
        database.albumDao().insert(album("custom/1", owned = true))
        database.albumDao().insert(album("done", owned = true, checked = true))

        checker().run()

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `many albums are asked about a batch at a time`() = runBlocking {
        (1..60).forEach { database.albumDao().insert(album("id$it", owned = true)) }
        server.enqueue(MockResponse().setBody(groups()))
        server.enqueue(MockResponse().setBody(groups()))
        server.enqueue(MockResponse().setBody(groups()))

        checker().run()

        assertEquals(3, server.requestCount)
        assertEquals(0, (1..60).count { !database.albumDao().getById("id$it")!!.categoryChecked })
    }
}
