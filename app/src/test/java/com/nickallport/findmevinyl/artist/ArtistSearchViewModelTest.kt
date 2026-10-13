package com.nickallport.findmevinyl.artist

import com.nickallport.findmevinyl.musicbrainz.MusicBrainzApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertNull

class ArtistSearchViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var viewModel: ArtistSearchViewModel

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)

        viewModel = ArtistSearchViewModel(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a successful search returns display items with formatted descriptions`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "artists": [ { "id": "1", "name": "Genesis", "type": "Group", "country": "GB", "life-span": { "begin": "1967", "end": "1998" }, "disambiguation": "English rock band" } ] }"""
            )
        )

        val state = viewModel.search("Genesis")

        assertTrue(state is ArtistSearchUiState.Success)
        val results = (state as ArtistSearchUiState.Success).results
        assertEquals(1, results.size)
        assertEquals("Genesis", results[0].name)
        assertEquals("Band from GB, active 1967\u20131998 (English rock band)", results[0].description)
    }

    @Test
    fun `a server error returns an error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.search("Genesis")

        assertTrue(state is ArtistSearchUiState.Error)
    }

    @Test
    fun `a cancelled search does not report a result`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        var state: ArtistSearchUiState? = null
        val job = launch(Dispatchers.IO) {
            state = viewModel.search("Genesis")
        }
        server.takeRequest(5, TimeUnit.SECONDS)
        job.cancelAndJoin()

        assertNull(state)
    }
}
