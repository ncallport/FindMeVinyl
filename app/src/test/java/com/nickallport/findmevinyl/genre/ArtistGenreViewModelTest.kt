package com.nickallport.findmevinyl.genre

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import com.nickallport.findmevinyl.storage.AppDatabase
import com.nickallport.findmevinyl.storage.ArtistEntity
import com.nickallport.findmevinyl.storage.ArtistGenresEntity
import com.nickallport.findmevinyl.storage.ArtistGroupEntity
import com.nickallport.findmevinyl.storage.ArtistGroupMemberEntity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** The genres MusicBrainz lists for an artist, to choose from, and the genre the user chose. */
@RunWith(RobolectricTestRunner::class)
class ArtistGenreViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var viewModel: ArtistGenreViewModel

    private var now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val requests = mutableListOf<String>()
    private val userAgents = mutableListOf<String?>()

    // What MusicBrainz answers for each artist id. No entry means a 500.
    private val answers = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests.add(request.path!!)
                    userAgents.add(request.getHeader("User-Agent"))
                    val id = request.path!!.substringAfter("/artist/").substringBefore("?")
                    val body = answers[id] ?: return MockResponse().setResponseCode(500)
                    return MockResponse().setBody(body)
                }
            }
            start()
        }
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(MusicBrainzApi::class.java)
        viewModel = ArtistGenreViewModel(
            api, database.artistGenresDao(), database.artistDao(), database.artistGroupDao(),
            throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0), clock = { now }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun genres(vararg pairs: Pair<String?, Int?>): String {
        val list = pairs.joinToString(",") { (name, count) ->
            buildString {
                append("{")
                if (name != null) append("\"name\":\"$name\"")
                if (name != null && count != null) append(",")
                if (count != null) append("\"count\":$count")
                append("}")
            }
        }
        return """{ "id": "x", "name": "Artist", "genres": [ $list ] }"""
    }

    private fun group(id: String, vararg members: String) = runBlocking {
        database.artistGroupDao().insertGroup(ArtistGroupEntity(id, "G", 0))
        database.artistGroupDao().insertMembers(members.mapIndexed { i, m -> ArtistGroupMemberEntity(m, id, "Name of $m", i) })
    }

    // Offering the genres.

    @Test
    fun `offers the genres of an artist, the best voted first`() = runBlocking {
        answers["cave"] = genres("post-punk" to 11, "punk blues" to 4, "alternative rock" to 11, "rock" to 2)

        val options = viewModel.options("cave")

        assertEquals(
            listOf(GenreOption("alternative rock", 11), GenreOption("post-punk", 11), GenreOption("punk blues", 4), GenreOption("rock", 2)),
            options
        )
    }

    @Test
    fun `asks MusicBrainz for the genres of the artist, with the user agent of the app`() = runBlocking {
        answers["cave"] = genres("rock" to 1)

        viewModel.options("cave")

        assertEquals(listOf("/artist/cave?inc=genres&fmt=json"), requests)
        assertEquals(listOf(AppUserAgent.VALUE), userAgents)
    }

    @Test
    fun `names are kept in lower case, and one with no name is left out`() = runBlocking {
        answers["a"] = genres("Post-Punk" to 3, null to 5, "  " to 2, "rock" to null)

        assertEquals(listOf(GenreOption("post-punk", 3), GenreOption("rock", 0)), viewModel.options("a"))
    }

    @Test
    fun `an artist with no genres on MusicBrainz is an empty list, not a failure`() = runBlocking {
        answers["a"] = """{ "id": "a", "name": "Obscure" }"""

        assertEquals(emptyList<GenreOption>(), viewModel.options("a"))
    }

    @Test
    fun `at most fifteen are offered`() = runBlocking {
        answers["a"] = genres(*(1..20).map { "genre $it" to it }.toTypedArray())

        val options = viewModel.options("a")!!

        assertEquals(15, options.size)
        assertEquals("genre 20", options.first().name)
    }

    @Test
    fun `a failure with nothing kept is null`() = runBlocking {
        assertNull(viewModel.options("nobody"))
    }

    // Keeping what MusicBrainz said.

    @Test
    fun `the answer is kept, so the second time asks nothing`() = runBlocking {
        answers["cave"] = genres("rock" to 1)

        viewModel.options("cave")
        viewModel.options("cave")

        assertEquals(1, requests.size)
    }

    @Test
    fun `an empty answer is kept too`() = runBlocking {
        answers["a"] = """{ "id": "a" }"""

        viewModel.options("a")
        viewModel.options("a")

        assertEquals(1, requests.size)
    }

    @Test
    fun `after ninety days it is asked again`() = runBlocking {
        answers["cave"] = genres("rock" to 1)
        viewModel.options("cave")

        now += 91 * day
        viewModel.options("cave")

        assertEquals(2, requests.size)
    }

    @Test
    fun `when asking again fails, the old answer is used`() = runBlocking {
        database.artistGenresDao().save(ArtistGenresEntity("cave", """[{"name":"post-punk","count":11}]""", now - 100 * day))

        assertEquals(listOf(GenreOption("post-punk", 11)), viewModel.options("cave"))
    }

    @Test
    fun `a failure is not kept`() = runBlocking {
        viewModel.options("cave")
        answers["cave"] = genres("rock" to 1)

        assertEquals(listOf(GenreOption("rock", 1)), viewModel.options("cave"))
    }

    // A group.

    @Test
    fun `a group offers the genres of all its members, the votes added up`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = genres("rock" to 3, "singer-songwriter" to 2)
        answers["seeds"] = genres("rock" to 4, "post-punk" to 11)

        val options = viewModel.options("group/1")

        assertEquals(
            listOf(GenreOption("post-punk", 11), GenreOption("rock", 7), GenreOption("singer-songwriter", 2)),
            options
        )
    }

    @Test
    fun `the members are asked one after another, and the group itself is never asked about`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["cave"] = genres("rock" to 1)
        answers["seeds"] = genres("rock" to 1)

        viewModel.options("group/1")

        assertEquals(listOf("/artist/cave?inc=genres&fmt=json", "/artist/seeds?inc=genres&fmt=json"), requests)
    }

    @Test
    fun `a member that fails leaves the genres of the others`() = runBlocking {
        group("group/1", "cave", "seeds")
        answers["seeds"] = genres("post-punk" to 11)

        assertEquals(listOf(GenreOption("post-punk", 11)), viewModel.options("group/1"))
    }

    @Test
    fun `when every member fails and nothing is kept a group has no answer`() = runBlocking {
        group("group/1", "cave", "seeds")

        assertNull(viewModel.options("group/1"))
    }

    @Test
    fun `a group with no members offers an empty list`() = runBlocking {
        assertEquals(emptyList<GenreOption>(), viewModel.options("group/none"))
        assertEquals(0, requests.size)
    }

    // The genre the user chose.

    @Test
    fun `choosing a genre for an artist keeps it, and it can be read back`() = runBlocking {
        database.artistDao().insert(ArtistEntity("cave", "Nick Cave", null, null))

        viewModel.setGenre("cave", "post-punk")

        assertEquals("post-punk", viewModel.genreOf("cave"))
        assertEquals("post-punk", database.artistDao().getById("cave")!!.genre)
    }

    @Test
    fun `choosing a genre for a group keeps it with the group`() = runBlocking {
        group("group/1", "cave", "seeds")

        viewModel.setGenre("group/1", "post-punk")

        assertEquals("post-punk", viewModel.genreOf("group/1"))
        assertEquals("post-punk", database.artistGroupDao().getGroup("group/1")!!.genre)
    }

    @Test
    fun `the genre is tidied, trimmed, in lower case, one space between words, and cut to forty letters`() = runBlocking {
        database.artistDao().insert(ArtistEntity("a", "A", null, null))

        viewModel.setGenre("a", "  Post    Punk ")
        assertEquals("post punk", viewModel.genreOf("a"))

        viewModel.setGenre("a", "x".repeat(100))
        assertEquals(40, viewModel.genreOf("a")!!.length)
    }

    @Test
    fun `a blank genre, or null, takes the genre away`() = runBlocking {
        database.artistDao().insert(ArtistEntity("a", "A", null, null))
        viewModel.setGenre("a", "rock")

        viewModel.setGenre("a", "   ")
        assertNull(viewModel.genreOf("a"))

        viewModel.setGenre("a", "rock")
        viewModel.setGenre("a", null)
        assertNull(viewModel.genreOf("a"))
    }

    @Test
    fun `an artist or group that is not kept has no genre, and choosing one for it changes nothing`() = runBlocking {
        viewModel.setGenre("stranger", "rock")
        viewModel.setGenre("group/none", "rock")

        assertNull(viewModel.genreOf("stranger"))
        assertNull(viewModel.genreOf("group/none"))
    }
}
