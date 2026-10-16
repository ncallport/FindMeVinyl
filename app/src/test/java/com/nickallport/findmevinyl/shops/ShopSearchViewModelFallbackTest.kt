package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.overpass.OverpassApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** The shop search asks a second Overpass server when the first one fails or is too slow. */
class ShopSearchViewModelFallbackTest {

    private lateinit var geocoder: MockWebServer
    private lateinit var primary: MockWebServer
    private lateinit var fallback: MockWebServer
    private lateinit var viewModel: ShopSearchViewModel

    private val londonResponse = """[ { "lat": "51.5074", "lon": "-0.1278", "display_name": "London, UK" } ]"""

    private val oneShop = """
        { "elements": [ { "type": "node", "id": 1, "lat": 51.508, "lon": -0.128,
            "tags": { "shop": "music", "name": "Near Records" } } ] }
    """.trimIndent()

    private fun overpassApi(server: MockWebServer, readTimeoutMillis: Long = 5_000): OverpassApi =
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)

    @Before
    fun setUp() {
        geocoder = MockWebServer().apply { start() }
        primary = MockWebServer().apply { start() }
        fallback = MockWebServer().apply { start() }
        viewModel = viewModel()
    }

    private fun viewModel(primaryTimeout: Long = 5_000, fallbackTimeout: Long = 5_000) = ShopSearchViewModel(
        Retrofit.Builder().baseUrl(geocoder.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            .create(NominatimApi::class.java),
        overpassApi(primary, primaryTimeout),
        fallbackOverpassApis = listOf(overpassApi(fallback, fallbackTimeout)),
        retryDelayMillis = 0
    )

    @After
    fun tearDown() {
        geocoder.shutdown(); primary.shutdown(); fallback.shutdown()
    }

    private fun search() = runBlocking {
        geocoder.enqueue(MockResponse().setBody(londonResponse))
        viewModel.searchTown("London")
    }

    @Test
    fun `the first server answers and the second is never asked`() {
        primary.enqueue(MockResponse().setBody(oneShop))

        val state = search()

        assertTrue(state is ShopSearchUiState.Success)
        assertEquals(1, primary.requestCount)
        assertEquals(0, fallback.requestCount)
    }

    @Test
    fun `a bad gateway from the first server is answered by the second, with the same query`() {
        primary.enqueue(MockResponse().setResponseCode(502))
        fallback.enqueue(MockResponse().setBody(oneShop))

        val state = search()

        assertEquals(listOf("Near Records"), (state as ShopSearchUiState.Success).shops.map { it.name })
        assertEquals(1, primary.requestCount)
        assertEquals(1, fallback.requestCount)
        assertEquals(
            primary.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8(),
            fallback.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8()
        )
    }

    @Test
    fun `a first server that is too slow is given up on, and the second answers`() {
        viewModel = viewModel(primaryTimeout = 300)
        primary.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        fallback.enqueue(MockResponse().setBody(oneShop))

        val state = search()

        assertTrue(state is ShopSearchUiState.Success)
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `a first server that refuses the app is not a reason to stop`() {
        primary.enqueue(MockResponse().setResponseCode(403))
        fallback.enqueue(MockResponse().setBody(oneShop))

        assertTrue(search() is ShopSearchUiState.Success)
    }

    @Test
    fun `both servers busy means asking both again, up to three rounds, and then an error`() {
        repeat(3) { primary.enqueue(MockResponse().setResponseCode(502)) }
        repeat(3) { fallback.enqueue(MockResponse().setResponseCode(504)) }

        val state = search()

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(3, primary.requestCount)
        assertEquals(3, fallback.requestCount)
    }

    @Test
    fun `busy first server, busy second one, then the first recovers in the next round`() {
        primary.enqueue(MockResponse().setResponseCode(502))
        primary.enqueue(MockResponse().setBody(oneShop))
        fallback.enqueue(MockResponse().setResponseCode(503))

        assertTrue(search() is ShopSearchUiState.Success)
        assertEquals(2, primary.requestCount)
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `two slow servers are asked once each, because another round would take as long again`() {
        viewModel = viewModel(primaryTimeout = 200, fallbackTimeout = 200)
        primary.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        fallback.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val state = search()

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(1, primary.requestCount)
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `an error that is not about load is tried on the second server once, and then reported`() {
        primary.enqueue(MockResponse().setResponseCode(400))
        fallback.enqueue(MockResponse().setResponseCode(400))

        val state = search()

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(1, primary.requestCount)
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `leaving the search while the first server is still thinking does not start asking the second`() {
        primary.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        geocoder.enqueue(MockResponse().setBody(londonResponse))

        var state: ShopSearchUiState? = null
        runBlocking {
            val job = launch(Dispatchers.IO) { state = viewModel.searchTown("London") }
            primary.takeRequest(5, TimeUnit.SECONDS)
            job.cancelAndJoin()
        }

        assertNull(state)
        assertEquals(0, fallback.requestCount)
    }

    @Test
    fun `without a second server it behaves as before`() = runBlocking {
        val single = ShopSearchViewModel(
            Retrofit.Builder().baseUrl(geocoder.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
                .create(NominatimApi::class.java),
            overpassApi(primary),
            retryDelayMillis = 0
        )
        geocoder.enqueue(MockResponse().setBody(londonResponse))
        primary.enqueue(MockResponse().setResponseCode(502))
        primary.enqueue(MockResponse().setBody(oneShop))

        assertTrue(single.searchTown("London") is ShopSearchUiState.Success)
        assertEquals(2, primary.requestCount)
    }

    private val timedOutWithNothing = """
        { "elements": [], "remark": "runtime error: Query timed out in \"query\" at line 4 after 26 seconds." }
    """.trimIndent()

    @Test
    fun `an answer that says the query timed out is not taken for no shops, and the second server is asked`() {
        primary.enqueue(MockResponse().setBody(timedOutWithNothing))
        fallback.enqueue(MockResponse().setBody(oneShop))

        val state = search()

        assertEquals(listOf("Near Records"), (state as ShopSearchUiState.Success).shops.map { it.name })
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `when both servers say the query failed it is an error, not an empty list, and nobody is asked again`() {
        primary.enqueue(MockResponse().setBody(timedOutWithNothing))
        fallback.enqueue(MockResponse().setBody(timedOutWithNothing))

        val state = search()

        assertTrue(state is ShopSearchUiState.Error)
        assertEquals(1, primary.requestCount)
        assertEquals(1, fallback.requestCount)
    }

    @Test
    fun `a remark that is only a remark does not stop the shops being used`() {
        primary.enqueue(
            MockResponse().setBody(
                """{ "elements": [ { "type": "node", "id": 1, "lat": 51.508, "lon": -0.128,
                    "tags": { "shop": "music", "name": "Near Records" } } ],
                    "remark": "runtime remark: this is only a warning" }"""
            )
        )

        val state = search()

        assertEquals(listOf("Near Records"), (state as ShopSearchUiState.Success).shops.map { it.name })
        assertEquals(0, fallback.requestCount)
    }

    @Test
    fun `an answer with no elements at all is an empty list of shops`() {
        primary.enqueue(MockResponse().setBody("{}"))

        val state = search()

        assertEquals(emptyList<Shop>(), (state as ShopSearchUiState.Success).shops)
    }
}
