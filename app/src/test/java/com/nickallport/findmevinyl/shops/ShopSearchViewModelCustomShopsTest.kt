package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.overpass.OverpassApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Shops the user added are part of a search for the place they are in, as if OpenStreetMap had them. */
class ShopSearchViewModelCustomShopsTest {

    private lateinit var server: MockWebServer

    private val town = """[ { "lat": "39.4072", "lon": "-9.1346", "display_name": "Caldas da Rainha, Portugal" } ]"""
    private val found = """
        { "elements": [ { "type": "node", "id": 1, "lat": 39.41, "lon": -9.14,
            "tags": { "shop": "music", "name": "Found Records" } } ] }
    """.trimIndent()

    private fun added(id: String, name: String, lat: Double, lon: Double) =
        Shop("custom/$id", name, ShopCategory.RECORD_SHOP, lat, lon, null)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun search(custom: List<Shop>, radius: Int = 5000): ShopSearchUiState {
        val retrofit = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        val viewModel = ShopSearchViewModel(
            retrofit.create(NominatimApi::class.java), retrofit.create(OverpassApi::class.java),
            customShops = { custom }, retryDelayMillis = 0
        )
        server.enqueue(MockResponse().setBody(town))
        server.enqueue(MockResponse().setBody(found))
        return runBlocking { viewModel.searchTown("Caldas da Rainha", radiusMeters = radius) }
    }

    @Test
    fun `a shop the user added near the place is listed with the shops that were found, nearest first`() {
        val state = search(listOf(added("a", "Silver Tentacle", 39.4072, -9.1346))) as ShopSearchUiState.Success

        assertEquals(listOf("Silver Tentacle", "Found Records"), state.shops.map { it.name })
    }

    @Test
    fun `a shop the user added far from the place is left out`() {
        val state = search(listOf(added("a", "Louie Louie", 38.7, -9.1))) as ShopSearchUiState.Success

        assertEquals(listOf("Found Records"), state.shops.map { it.name })
    }

    @Test
    fun `the radius of the search decides how far`() {
        val nearish = added("a", "Nearish", 39.4272, -9.1346)

        assertEquals(2, (search(listOf(nearish), radius = 5000) as ShopSearchUiState.Success).shops.size)
        assertEquals(1, (search(listOf(nearish), radius = 1000) as ShopSearchUiState.Success).shops.size)
    }

    @Test
    fun `the same shop is not listed twice`() {
        val twice = listOf(added("a", "Silver Tentacle", 39.4072, -9.1346), added("a", "Silver Tentacle", 39.4072, -9.1346))

        val state = search(twice) as ShopSearchUiState.Success

        assertEquals(2, state.shops.size)
    }

    @Test
    fun `without any added shops the search is as before`() {
        val state = search(emptyList()) as ShopSearchUiState.Success

        assertEquals(listOf("Found Records"), state.shops.map { it.name })
    }
}
