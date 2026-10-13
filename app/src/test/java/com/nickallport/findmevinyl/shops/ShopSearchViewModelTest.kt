package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.overpass.OverpassApi
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

class ShopSearchViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var viewModel: ShopSearchViewModel

    private val londonResponse =
        """[ { "lat": "51.5074", "lon": "-0.1278", "display_name": "London, UK" } ]"""

    private val twoShopsFarthestFirst = """
        {
          "elements": [
            { "type": "node", "id": 1, "lat": 51.56, "lon": -0.20,
              "tags": { "shop": "music", "name": "Far Records" } },
            { "type": "node", "id": 2, "lat": 51.508, "lon": -0.128,
              "tags": { "shop": "music", "name": "Near Records" } }
          ]
        }
    """.trimIndent()

    private val leiriaReverseResponse = """{ "display_name": "Leiria, Leiria, Portugal" }"""

    private val newportResponse = """
        [
          { "lat": "51.5842", "lon": "-2.9977", "display_name": "Newport, Wales, United Kingdom" },
          { "lat": "50.7000", "lon": "-1.2915", "display_name": "Newport, Isle of Wight, England, United Kingdom" },
          { "lat": "39.1000", "lon": "-84.4500", "display_name": "Newport, Kentucky, United States" }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        viewModel = ShopSearchViewModel(
            retrofit.create(NominatimApi::class.java),
            retrofit.create(OverpassApi::class.java),
            retryDelayMillis = 0
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns the shops nearest first with the geocoded centre`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Success)
        val success = state as ShopSearchUiState.Success
        assertEquals(listOf("Near Records", "Far Records"), success.shops.map { it.name })
        assertEquals(51.5074, success.centerLatitude, 0.0001)
        assertEquals(-0.1278, success.centerLongitude, 0.0001)
    }

    @Test
    fun `queries Overpass around the geocoded point`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        viewModel.searchTown("London", radiusMeters = 3000)

        val geocodeRequest = server.takeRequest()
        val overpassRequest = server.takeRequest()
        assertEquals("/search?q=London&format=json&limit=5", geocodeRequest.path)
        assertEquals("POST", overpassRequest.method)
        val body = overpassRequest.body.readUtf8()
        assertTrue(body.contains("51.5074"))
        assertTrue(body.contains("3000"))
    }

    @Test
    fun `reports an unknown town without querying Overpass`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        val state = viewModel.searchTown("Atlantis")

        assertEquals(ShopSearchUiState.TownNotFound("Atlantis"), state)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `returns an error when geocoding fails`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Error)
    }

    @Test
    fun `returns an error when the shop query fails`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Error)
    }

    @Test
    fun `retries when Overpass is busy and then succeeds`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setResponseCode(504))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Success)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `gives up with an error after three busy responses`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setResponseCode(504))
        server.enqueue(MockResponse().setResponseCode(504))
        server.enqueue(MockResponse().setResponseCode(504))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `does not retry an error that is not about load`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setResponseCode(400))

        val state = viewModel.searchTown("London")

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `includes the place name that Nominatim resolved`() = runBlocking {
        server.enqueue(MockResponse().setBody(londonResponse))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("London")

        assertEquals("London, UK", (state as ShopSearchUiState.Success).placeName)
    }

    @Test
    fun `falls back to the typed town when Nominatim gives no display name`() = runBlocking {
        server.enqueue(MockResponse().setBody("""[ { "lat": "51.5074", "lon": "-0.1278" } ]"""))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("London")

        assertEquals("London", (state as ShopSearchUiState.Success).placeName)
    }

    @Test
    fun `searching around a point returns the shops nearest first`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        server.enqueue(MockResponse().setBody(leiriaReverseResponse))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertTrue(state is ShopSearchUiState.Success)
        val success = state as ShopSearchUiState.Success
        assertEquals(listOf("Near Records", "Far Records"), success.shops.map { it.name })
        assertEquals(51.5074, success.centerLatitude, 0.0001)
    }

    @Test
    fun `searching around a point retries when Overpass is busy`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(504))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        server.enqueue(MockResponse().setBody(leiriaReverseResponse))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertTrue(state is ShopSearchUiState.Success)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `searching around a point returns an error when the shop query fails`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertTrue(state is ShopSearchUiState.Error)
    }

    @Test
    fun `names the place from a reverse lookup of the position`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        server.enqueue(MockResponse().setBody(leiriaReverseResponse))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertEquals("Leiria, Leiria, Portugal", (state as ShopSearchUiState.Success).placeName)
        assertEquals("POST", server.takeRequest().method)
        assertEquals(
            "/reverse?lat=51.5074&lon=-0.1278&format=json&zoom=10",
            server.takeRequest().path
        )
    }

    @Test
    fun `still shows the shops when the place lookup fails`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        server.enqueue(MockResponse().setResponseCode(500))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertTrue(state is ShopSearchUiState.Success)
        val success = state as ShopSearchUiState.Success
        assertEquals("Your location", success.placeName)
        assertEquals(2, success.shops.size)
    }

    @Test
    fun `falls back to Your location when Nominatim cannot name the position`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        server.enqueue(MockResponse().setBody("""{ "error": "Unable to geocode" }"""))

        val state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)

        assertEquals("Your location", (state as ShopSearchUiState.Success).placeName)
    }

    @Test
    fun `a cancelled town search does not report a result`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        var state: ShopSearchUiState? = null
        val job = launch(Dispatchers.IO) {
            state = viewModel.searchTown("London")
        }
        server.takeRequest(5, TimeUnit.SECONDS)
        job.cancelAndJoin()

        assertNull(state)
    }

    @Test
    fun `a cancelled search around a point does not report a result`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        var state: ShopSearchUiState? = null
        val job = launch(Dispatchers.IO) {
            state = viewModel.searchAround(latitude = 51.5074, longitude = -0.1278)
        }
        server.takeRequest(5, TimeUnit.SECONDS)
        job.cancelAndJoin()

        assertNull(state)
    }

    @Test
    fun `offers the other matches as alternatives`() = runBlocking {
        server.enqueue(MockResponse().setBody(newportResponse))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Newport")

        val success = state as ShopSearchUiState.Success
        assertEquals("Newport, Wales, United Kingdom", success.placeName)
        assertEquals(
            listOf(
                PlaceCandidate("Newport, Isle of Wight, England, United Kingdom", 50.7, -1.2915),
                PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45)
            ),
            success.otherPlaces
        )
    }

    @Test
    fun `leaves out alternatives with the same name as the chosen place`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "lat": "51.5842", "lon": "-2.9977", "display_name": "Newport, Wales, United Kingdom" },
                  { "lat": "51.5900", "lon": "-3.0000", "display_name": "Newport, Wales, United Kingdom" }
                ]
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Newport")

        assertEquals(
            emptyList<PlaceCandidate>(),
            (state as ShopSearchUiState.Success).otherPlaces
        )
    }

    @Test
    fun `skips alternatives that have no name or an unreadable position`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "lat": "51.5842", "lon": "-2.9977", "display_name": "Newport, Wales, United Kingdom" },
                  { "lat": "50.7", "lon": "-1.29" },
                  { "lat": "north", "lon": "-84.45", "display_name": "Newport, Kentucky, United States" }
                ]
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Newport")

        assertEquals(
            emptyList<PlaceCandidate>(),
            (state as ShopSearchUiState.Success).otherPlaces
        )
    }

    @Test
    fun `searching a chosen place uses its position without geocoding`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        val place = PlaceCandidate("Newport, Isle of Wight, England, United Kingdom", 50.7, -1.2915)

        val state = viewModel.searchPlace(place)

        val success = state as ShopSearchUiState.Success
        assertEquals("Newport, Isle of Wight, England, United Kingdom", success.placeName)
        assertEquals(50.7, success.centerLatitude, 0.0001)
        assertEquals(-1.2915, success.centerLongitude, 0.0001)
        assertEquals(2, success.shops.size)
        assertEquals(1, server.requestCount)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("50.7"))
        assertTrue(body.contains("-1.2915"))
    }

    @Test
    fun `carries the given alternatives into the result`() = runBlocking {
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        val place = PlaceCandidate("Newport, Isle of Wight, England, United Kingdom", 50.7, -1.2915)
        val others = listOf(PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977))

        val state = viewModel.searchPlace(place, otherPlaces = others)

        assertEquals(others, (state as ShopSearchUiState.Success).otherPlaces)
    }

    @Test
    fun `searching a chosen place retries when Overpass is busy`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(504))
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))
        val place = PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977)

        val state = viewModel.searchPlace(place)

        assertTrue(state is ShopSearchUiState.Success)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `searching a chosen place returns an error when the shop query fails`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val place = PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977)

        val state = viewModel.searchPlace(place)

        assertTrue(state is ShopSearchUiState.Error)
    }

    @Test
    fun `a cancelled search of a chosen place does not report a result`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val place = PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977)

        var state: ShopSearchUiState? = null
        val job = launch(Dispatchers.IO) {
            state = viewModel.searchPlace(place)
        }
        server.takeRequest(5, TimeUnit.SECONDS)
        job.cancelAndJoin()

        assertNull(state)
    }

    @Test
    fun `leaves out an alternative that is close to the chosen place`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "lat": "41.4901", "lon": "-71.3128", "display_name": "Newport, Rhode Island, United States" },
                  { "lat": "41.5000", "lon": "-71.3000", "display_name": "Newport, Newport County, Rhode Island, United States" },
                  { "lat": "39.1000", "lon": "-84.4500", "display_name": "Newport, Kentucky, United States" }
                ]
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Newport")

        assertEquals(
            listOf(PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45)),
            (state as ShopSearchUiState.Success).otherPlaces
        )
    }

    @Test
    fun `keeps only the first of several alternatives that are close together`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "lat": "41.4901", "lon": "-71.3128", "display_name": "Newport, Rhode Island, United States" },
                  { "lat": "51.5842", "lon": "-2.9977", "display_name": "Newport, Wales, United Kingdom" },
                  { "lat": "51.5900", "lon": "-3.0200", "display_name": "Newport, City of Newport, Wales, NP20, United Kingdom" },
                  { "lat": "39.1000", "lon": "-84.4500", "display_name": "Newport, Kentucky, United States" }
                ]
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Newport")

        assertEquals(
            listOf(
                PlaceCandidate("Newport, Wales, United Kingdom", 51.5842, -2.9977),
                PlaceCandidate("Newport, Kentucky, United States", 39.1, -84.45)
            ),
            (state as ShopSearchUiState.Success).otherPlaces
        )
    }

    @Test
    fun `keeps an alternative that is far enough away for the searches not to overlap`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  { "lat": "51.0000", "lon": "-2.0000", "display_name": "Alpha, England, United Kingdom" },
                  { "lat": "51.1000", "lon": "-2.0000", "display_name": "Beta, England, United Kingdom" }
                ]
                """.trimIndent()
            )
        )
        server.enqueue(MockResponse().setBody(twoShopsFarthestFirst))

        val state = viewModel.searchTown("Alpha")

        assertEquals(
            listOf(PlaceCandidate("Beta, England, United Kingdom", 51.1, -2.0)),
            (state as ShopSearchUiState.Success).otherPlaces
        )
    }
}