package com.nickallport.findmevinyl.shops

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Following a short Google Maps link to the page it stands for, without loading that page. */
class MapsLinkResolverTest {

    private lateinit var server: MockWebServer
    private lateinit var other: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        other = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown(); other.shutdown()
    }

    // The test servers stand in for Google's.
    private fun resolver(maxHops: Int = 5) = MapsLinkResolver(
        isAllowed = { it.port == server.port }, maxHops = maxHops
    )

    @Test
    fun `follows a redirect to the link it leads to`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/maps/place/Silver+Tentacle").toString()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>a big page</html>"))

        val final = resolver().resolve(server.url("/short").toString())

        assertEquals(server.url("/maps/place/Silver+Tentacle").toString(), final)
    }

    @Test
    fun `a link that does not redirect is its own answer`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals(server.url("/maps/place/X").toString(), resolver().resolve(server.url("/maps/place/X").toString()))
    }

    @Test
    fun `follows several redirects in a row`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "/two"))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/three"))
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals(server.url("/three").toString(), resolver().resolve(server.url("/one").toString()))
    }

    @Test
    fun `does not follow a redirect to somewhere that is not Google`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", other.url("/steal").toString()))

        val final = resolver().resolve(server.url("/short").toString())

        assertNull(final)
        assertEquals(0, other.requestCount)
    }

    @Test
    fun `gives up on a loop of redirects`() = runBlocking {
        repeat(10) { server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/again")) }

        assertNull(resolver(maxHops = 4).resolve(server.url("/again").toString()))
        assertEquals(true, server.requestCount <= 5)
    }

    @Test
    fun `an error from the server gives nothing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        assertNull(resolver().resolve(server.url("/missing").toString()))
    }

    @Test
    fun `a link to somewhere that is not allowed is not even asked`() = runBlocking {
        assertNull(resolver().resolve(other.url("/x").toString()))
        assertEquals(0, other.requestCount)
    }

    @Test
    fun `sends no cookies and identifies the app`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        resolver().resolve(server.url("/x").toString())

        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertNull(request.getHeader("Cookie"))
        assertEquals(true, request.getHeader("User-Agent")!!.startsWith("FindMeVinyl"))
    }
}
