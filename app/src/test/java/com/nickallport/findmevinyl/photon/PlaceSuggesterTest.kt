package com.nickallport.findmevinyl.photon

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PlaceSuggesterTest {

    private fun photon(vararg labels: String) = PhotonResponseDto(
        labels.map { PhotonFeatureDto(PhotonPropertiesDto(name = it), PhotonGeometryDto(listOf(-9.0, 39.0))) }
    )

    private class FakeApi(private val answer: () -> PhotonResponseDto) : PhotonApi {
        val queries = mutableListOf<String>()
        override suspend fun search(query: String, limit: Int, layers: List<String>, userAgent: String): PhotonResponseDto {
            queries.add(query)
            return answer()
        }
    }

    @Test
    fun `suggests places for what was typed`() = runBlocking {
        val suggester = PlaceSuggester(FakeApi { photon("Caldas da Rainha") })

        val result = suggester.suggest("Caldas")

        assertEquals(listOf("Caldas da Rainha"), result.map { it.label })
    }

    @Test
    fun `asks for nothing until there are three letters`() = runBlocking {
        val api = FakeApi { photon("x") }
        val suggester = PlaceSuggester(api)

        assertTrue(suggester.suggest("Ca").isEmpty())
        assertTrue(suggester.suggest("  ").isEmpty())
        assertTrue(suggester.suggest(" C a ").isEmpty())

        assertTrue(api.queries.isEmpty())
    }

    @Test
    fun `trims what was typed`() = runBlocking {
        val api = FakeApi { photon("Lisbon") }

        PlaceSuggester(api).suggest("  Lisbon ")

        assertEquals(listOf("Lisbon"), api.queries)
    }

    @Test
    fun `the same text is asked for once, whatever its case`() = runBlocking {
        val api = FakeApi { photon("Lisbon") }
        val suggester = PlaceSuggester(api)

        suggester.suggest("Lisbon")
        suggester.suggest("lisbon ")

        assertEquals(1, api.queries.size)
    }

    @Test
    fun `a failure gives no suggestions and is asked for again next time`() = runBlocking {
        var fail = true
        val api = FakeApi { if (fail) throw IOException("offline") else photon("Lisbon") }
        val suggester = PlaceSuggester(api)

        assertTrue(suggester.suggest("Lisbon").isEmpty())
        fail = false

        assertEquals(listOf("Lisbon"), suggester.suggest("Lisbon").map { it.label })
        assertEquals(2, api.queries.size)
    }

    @Test(expected = CancellationException::class)
    fun `a cancellation is not swallowed`() {
        runBlocking {
            PlaceSuggester(FakeApi { throw CancellationException("typing went on") }).suggest("Lisbon")
        }
    }
}
