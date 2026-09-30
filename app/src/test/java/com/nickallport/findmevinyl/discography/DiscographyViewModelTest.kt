package com.nickallport.findmevinyl.discography

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

class DiscographyViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var viewModel: DiscographyViewModel

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)

        viewModel = DiscographyViewModel(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a successful fetch returns albums grouped and organized by category`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "release-groups": [
                    { "id": "1", "title": "Foxtrot", "primary-type": "Album", "secondary-types": [], "first-release-date": "1972-10-06" },
                    { "id": "2", "title": "Live 1976", "primary-type": "Album", "secondary-types": ["Live"], "first-release-date": "1976-01-01" }
                  ]
                }
                """.trimIndent()
            )
        )

        val state = viewModel.loadDiscography(artistId = "some-id")

        assertTrue(state is DiscographyUiState.Success)
        val grouped = (state as DiscographyUiState.Success).groupedAlbums
        assertEquals("Foxtrot", grouped[DiscographyCategory.STUDIO_ALBUM]?.get(0)?.title)
        assertEquals("Live 1976", grouped[DiscographyCategory.LIVE_ALBUM]?.get(0)?.title)
    }

    @Test
    fun `a server error returns an error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.loadDiscography(artistId = "some-id")

        assertTrue(state is DiscographyUiState.Error)
    }
}
