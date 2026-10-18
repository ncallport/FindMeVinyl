package com.nickallport.findmevinyl.discography

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AlbumEntity
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** The discography of a group of artists: every member's albums, together. */
@RunWith(RobolectricTestRunner::class)
class DiscographyViewModelGroupTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: DiscographyViewModel

    private val now = 1_800_000_000_000L
    private val requested = mutableListOf<String>()

    // What each member's release groups call for, by the artist id in the request. No entry means a 500.
    private val answers = mutableMapOf<String, String>()
    private val releaseAnswers = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val artist = request.requestUrl!!.queryParameter("artist").orEmpty()
                    requested.add(request.path!!.substringBefore('?').trim('/') + ":" + artist)
                    val body = if (request.path!!.startsWith("/release/")) releaseAnswers[artist] else answers[artist]
                    return if (body == null) MockResponse().setResponseCode(500) else MockResponse().setBody(body)
                }
            }
            start()
        }
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(MusicBrainzApi::class.java)
        viewModel = DiscographyViewModel(
            api, database.albumDao(), database.artistDao(),
            pageSize = 100, pageDelayMillis = 0,
            throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0), clock = { now },
            groupDao = database.artistGroupDao()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun group(id: String = "group/1", vararg members: String) = runBlocking {
        database.artistGroupDao().insertGroup(ArtistGroupEntity(id, "The Group", 0))
        database.artistGroupDao().insertMembers(members.mapIndexed { i, m -> ArtistGroupMemberEntity(m, id, "Name of $m", i) })
    }

    private fun page(vararg albums: Triple<String, String, String>): String {
        val groups = albums.joinToString(",") { (id, title, date) ->
            """{ "id": "$id", "title": "$title", "primary-type": "Album", "secondary-types": [], "first-release-date": "$date" }"""
        }
        return """{ "release-group-count": ${albums.size}, "release-groups": [ $groups ] }"""
    }

    private fun albums(state: DiscographyUiState) =
        (state as DiscographyUiState.Success).groupedAlbums.getValue(DiscographyCategory.STUDIO_ALBUM)

    private suspend fun saved(id: String, artist: String, date: String? = "1990") = database.albumDao().insert(
        AlbumEntity(id, artist, "Album $id", "STUDIO_ALBUM", date, owned = false, wanted = false)
    )

    @Test
    fun `the discography of a group is every member's albums in one list, by release date`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = page(Triple("a", "From Her to Eternity", "1984"), Triple("c", "Push the Sky Away", "2013"))
        answers["seeds"] = page(Triple("b", "Henry's Dream", "1992"))

        val state = viewModel.loadDiscography("group/1")

        assertEquals(listOf("a", "b", "c"), albums(state).map { it.id })
        assertFalse((state as DiscographyUiState.Success).loadingMore)
    }

    @Test
    fun `an album credited to both members is listed once`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = page(Triple("carnage", "Carnage", "2021"))
        answers["seeds"] = page(Triple("carnage", "Carnage", "2021"), Triple("x", "Other", "2000"))

        assertEquals(listOf("x", "carnage"), albums(viewModel.loadDiscography("group/1")).map { it.id })
    }

    @Test
    fun `the members are asked for one after another, in the order of the group`() = runBlocking {
        group("group/1", "cave", "seeds", "grinderman")
        answers["cave"] = page(Triple("a", "A", "1984"))
        answers["seeds"] = page(Triple("b", "B", "1992"))
        answers["grinderman"] = page(Triple("c", "C", "2007"))

        viewModel.loadDiscography("group/1")

        assertEquals(listOf("release-group:cave", "release-group:seeds", "release-group:grinderman"), requested)
    }

    @Test
    fun `only the last state says nothing more is coming, and each member adds to the list`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = page(Triple("a", "A", "1984"))
        answers["seeds"] = page(Triple("b", "B", "1992"))

        val states = viewModel.discography("group/1").toList().map { it as DiscographyUiState.Success }

        assertEquals(listOf(true, false), states.map { it.loadingMore })
        assertEquals(listOf(listOf("a"), listOf("a", "b")), states.map { s -> s.groupedAlbums.getValue(DiscographyCategory.STUDIO_ALBUM).map { it.id } })
    }

    @Test
    fun `albums already saved for any member show at once, before any request`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("old-cave", "cave"); saved("old-seeds", "seeds")
        answers["cave"] = page(Triple("old-cave", "Album old-cave", "1990"))
        answers["seeds"] = page(Triple("old-seeds", "Album old-seeds", "1990"))

        val first = viewModel.discography("group/1").toList().first() as DiscographyUiState.Success

        assertEquals(setOf("old-cave", "old-seeds"), albums(first).map { it.id }.toSet())
        assertTrue(first.loadingMore)
    }

    @Test
    fun `a member that fails leaves the others' albums and says so`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = page(Triple("a", "A", "1984"))
        // No answer for the Bad Seeds: a server error.

        val last = viewModel.discography("group/1").toList().last() as DiscographyUiState.Success

        assertEquals(listOf("a"), albums(last).map { it.id })
        assertFalse(last.loadingMore)
        assertTrue(last.loadError != null)
    }

    @Test
    fun `when every member fails and nothing is saved the discography is an error`() = runBlocking {
        group("group/1", "cave", "seeds")

        val last = viewModel.discography("group/1").toList().last()

        assertTrue(last is DiscographyUiState.Error)
    }

    @Test
    fun `the saved discography of a group is the saved albums of all its members, with no request`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("a", "cave", "1984"); saved("b", "seeds", "1980"); saved("z", "somebody else")

        val state = viewModel.loadSaved("group/1")

        assertEquals(listOf("b", "a"), albums(state).map { it.id })
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a group with no members has an empty discography and asks for nothing`() = runBlocking {
        val state = viewModel.loadDiscography("group/none")

        assertTrue((state as DiscographyUiState.Success).groupedAlbums.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `an ordinary artist is loaded as before`() = runBlocking {
        answers["solo"] = page(Triple("a", "A", "1984"))

        val state = viewModel.loadDiscography("solo")

        assertEquals(listOf("a"), albums(state).map { it.id })
        assertEquals(listOf("release-group:solo"), requested)
    }

    // Owning, wanting and saving.

    @Test
    fun `owning or wanting an album on a group page does not make the group a favourite artist`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("a", "cave")

        viewModel.toggleOwned("a", false, "group/1", "The Group")
        saved("b", "seeds")
        viewModel.toggleWanted("b", false, "group/1", "The Group")

        assertTrue(database.albumDao().getById("a")!!.owned)
        assertTrue(database.albumDao().getById("b")!!.wanted)
        assertNull(database.artistDao().getById("group/1"))
        assertEquals(emptyList<String>(), database.artistDao().getAllFavourites().map { it.id })
    }

    @Test
    fun `a group is always a favourite, and an unknown group is not`() = runBlocking {
        group("group/1", "cave", "seeds")

        assertTrue(viewModel.isFavourite("group/1"))
        assertFalse(viewModel.isFavourite("group/none"))
    }

    // Checking the formats of every album.

    private fun release(group: String, vararg formats: String) =
        """{ "id": "r-$group-${formats.contentHashCode()}", "release-group": { "id": "$group" },
              "media": [ ${formats.joinToString(",") { """{ "format": "${it.replace("\"", "\\\"")}" }""" }} ] }"""

    private fun releases(vararg releases: String) =
        """{ "release-count": ${releases.size}, "releases": [ ${releases.joinToString(",")} ] }"""

    @Test
    fun `checking the formats of a group checks each member and adds up what was saved`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("a", "cave"); saved("b", "seeds")
        releaseAnswers["cave"] = releases(release("a", "CD"))
        releaseAnswers["seeds"] = releases(release("b", "12\" Vinyl"))

        val result = viewModel.checkAllFormats("group/1")

        assertEquals(FormatCheckResult.Done(2), result)
        assertEquals("CD", database.albumDao().getById("a")!!.formats)
        assertEquals("12\" Vinyl", database.albumDao().getById("b")!!.formats)
    }

    @Test
    fun `a member whose albums are all checked already is not checked again`() = runBlocking {
        group("group/1", "cave", "seeds")
        database.albumDao().insert(
            AlbumEntity("a", "cave", "A", "STUDIO_ALBUM", "1990", false, false, formats = "CD", formatsCheckedAt = now, statuses = "Official")
        )
        saved("b", "seeds")
        releaseAnswers["seeds"] = releases(release("b", "CD"))

        val result = viewModel.checkAllFormats("group/1")

        assertEquals(FormatCheckResult.Done(1), result)
        assertEquals(listOf("release:seeds"), requested)
    }

    @Test
    fun `if checking one member fails the group check fails`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("a", "cave"); saved("b", "seeds")
        releaseAnswers["cave"] = releases(release("a", "CD"))
        // Nothing for the Bad Seeds.

        assertEquals(FormatCheckResult.Failed, viewModel.checkAllFormats("group/1"))
    }

    @Test
    fun `progress runs on across the members`() = runBlocking {
        group("group/1", "cave", "seeds")
        saved("a", "cave"); saved("b", "seeds")
        releaseAnswers["cave"] = releases(release("a", "CD"))
        releaseAnswers["seeds"] = releases(release("b", "CD"))
        val seen = mutableListOf<Pair<Int, Int>>()

        viewModel.checkAllFormats("group/1") { done, total -> seen.add(done to total) }

        assertEquals(listOf(1 to 1, 2 to 2), seen)
    }
}
