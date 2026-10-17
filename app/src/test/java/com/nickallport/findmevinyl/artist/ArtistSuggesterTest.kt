package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
import com.nickallport.findmevinyl.musicbrainz.MusicBrainzThrottle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ArtistSuggesterTest {

    private lateinit var server: MockWebServer
    private lateinit var suggester: ArtistSuggester

    private val joyDivision = """
        { "artists": [
          { "id": "a1", "name": "Joy Division", "type": "Group", "country": "GB", "disambiguation": "English post-punk band",
            "life-span": { "begin": "1976", "end": "1980" } },
          { "id": "a2", "name": "Joy", "type": "Group", "country": "AT",
            "life-span": { "begin": "1980" } },
          { "id": "89ad4ac3-39f7-470e-963a-56509c546377", "name": "Various Artists", "type": "Other",
            "disambiguation": "add compilations to this artist" },
          { "id": "125ec42a-7229-4250-afc5-e57cfe0f4a02", "name": "[unknown]", "type": "Other", "country": "XW",
            "disambiguation": "Special Purpose Artist - Do not add releases here, if possible." }
        ] }
    """.trimIndent()

    private fun suggesterFor(throttle: MusicBrainzThrottle = MusicBrainzThrottle(minIntervalMillis = 0)): ArtistSuggester {
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
        return ArtistSuggester(api, throttle)
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        suggester = suggesterFor()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun lastQuery(): Pair<String?, String?> {
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        return request.requestUrl!!.queryParameter("query") to request.requestUrl!!.queryParameter("limit")
    }

    @Test
    fun `asks for artists that start like the last word typed, five at most`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        suggester.suggest("Joy Divi")

        assertEquals("Joy Divi*" to "5", lastQuery())
    }

    @Test
    fun `a finished word, with a space after it, is not made a prefix`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        suggester.suggest("Joy ")

        assertEquals("Joy" to "5", lastQuery())
    }

    @Test
    fun `characters that mean something to the search are taken out`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        suggester.suggest("AC/DC (live) \"x\"")

        assertEquals("AC DC live x*" to "5", lastQuery())
    }

    @Test
    fun `sends the user agent of the app`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        suggester.suggest("Joy Divi")

        assertEquals(AppUserAgent.VALUE, server.takeRequest(5, TimeUnit.SECONDS)!!.getHeader("User-Agent"))
    }

    @Test
    fun `describes each artist and leaves out the special ones`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        val result = suggester.suggest("Joy")

        assertEquals(listOf("a1", "a2"), result.map { it.id })
        assertEquals(
            listOf("Band from GB (English post-punk band)", "Band from AT"),
            result.map { it.description }
        )
    }

    @Test
    fun `asks for nothing until there are three letters`() = runBlocking {
        assertTrue(suggester.suggest("Jo").isEmpty())
        assertTrue(suggester.suggest("  ").isEmpty())

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `the same text is asked for once, whatever its case`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))

        suggester.suggest("Joy Divi")
        suggester.suggest("joy divi")

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a busy answer gives no suggestions and is asked again next time`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(joyDivision))
        val suggester = suggesterFor(MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 0))

        assertTrue(suggester.suggest("Joy Divi").isEmpty())

        assertEquals(2, suggester.suggest("Joy Divi").size)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `typing on while it waits for its turn cancels it and sends nothing`() = runBlocking {
        server.enqueue(MockResponse().setBody(joyDivision))
        val slow = suggesterFor(MusicBrainzThrottle(minIntervalMillis = 60_000))
        slow.suggest("Joy Divi")

        val waiting = launch { slow.suggest("Joy Divis") }
        delay(200)
        waiting.cancel()
        waiting.join()

        assertTrue(waiting.isCancelled)
        assertEquals(1, server.requestCount)
    }
}
