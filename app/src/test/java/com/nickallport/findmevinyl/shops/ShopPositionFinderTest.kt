package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.Coordinates
import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.nominatim.NominatimResultDto
import com.nickallport.findmevinyl.nominatim.NominatimReverseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopPositionFinderTest {

    /** A geocoder that knows some places, and remembers what it was asked. */
    private class FakeNominatim(private val known: Map<String, Pair<String, String>>) : NominatimApi {
        val asked = mutableListOf<String>()

        override suspend fun search(query: String, format: String, limit: Int, addressDetails: Int, userAgent: String): List<NominatimResultDto> {
            asked.add(query)
            val hit = known[query] ?: return emptyList()
            return listOf(NominatimResultDto(hit.first, hit.second, query))
        }

        override suspend fun reverse(latitude: Double, longitude: Double, format: String, zoom: Int, addressDetails: Int, userAgent: String): NominatimReverseDto =
            NominatimReverseDto(null)
    }

    private fun finder(api: NominatimApi) = ShopPositionFinder(api, pauseMillis = 0)

    @Test
    fun `an address that is known gives its position, exactly`() = runBlocking {
        val api = FakeNominatim(mapOf("Rua do Telhal 6, 1150-346 Lisboa" to ("38.7222" to "-9.1432")))

        val found = finder(api).find("Rua do Telhal 6, 1150-346 Lisboa")!!

        assertEquals(Coordinates(38.7222, -9.1432), found.position)
        assertEquals(false, found.approximate)
        assertEquals(listOf("Rua do Telhal 6, 1150-346 Lisboa"), api.asked)
    }

    @Test
    fun `when the street is not known it falls back to the town and says the position is approximate`() = runBlocking {
        val api = FakeNominatim(mapOf("Caldas da Rainha" to ("39.4071857" to "-9.1346004")))

        val found = finder(api).find("R. Raul Proença 56a, 2500 Caldas da Rainha")!!

        assertEquals(Coordinates(39.4071857, -9.1346004), found.position)
        assertEquals(true, found.approximate)
        assertEquals("Caldas da Rainha", found.place)
    }

    @Test
    fun `the postcode is dropped from the part that names the town`() = runBlocking {
        val api = FakeNominatim(mapOf("Caldas da Rainha" to ("39.4" to "-9.1")))

        finder(api).find("R. Raul Proença 56a, 2500 Caldas da Rainha")

        assertEquals(true, "Caldas da Rainha" in api.asked)
        assertEquals(false, api.asked.any { it.startsWith("2500") })
    }

    @Test
    fun `a postcode written with a hyphen is dropped as well`() = runBlocking {
        val api = FakeNominatim(mapOf("Lisboa" to ("38.7" to "-9.1")))

        finder(api).find("Rua X 3, 1100-225 Lisboa")

        assertEquals(true, "Lisboa" in api.asked)
    }

    @Test
    fun `nothing known gives nothing`() = runBlocking {
        assertNull(finder(FakeNominatim(emptyMap())).find("Nowhere Street 1, Atlantis"))
    }

    @Test
    fun `an empty address is not asked about`() = runBlocking {
        val api = FakeNominatim(emptyMap())

        assertNull(finder(api).find("   "))
        assertEquals(emptyList<String>(), api.asked)
    }

    @Test
    fun `asks for as few things as it can`() = runBlocking {
        val api = FakeNominatim(emptyMap())

        finder(api).find("Rua X 3, 1100-225 Lisboa, Portugal")

        assertEquals(true, api.asked.size <= 3)
    }
}
