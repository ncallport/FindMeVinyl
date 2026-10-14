package com.nickallport.findmevinyl.image

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.net.InetAddress
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// Native graphics decodes images for real, so bytes that are not an image give null as on a device.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class ImageLoaderTest {

    private lateinit var server: MockWebServer
    private lateinit var loader: ImageLoader

    // A 1x1 PNG.
    private val png = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    )

    private fun pngResponse() = MockResponse().setBody(Buffer().write(png))

    private fun url(path: String) = server.url(path).toString()

    // On a runner where localhost is both 127.0.0.1 and ::1, MockWebServer listens on only the first.
    // After a failed request OkHttp tries the other address first, and with retries off it gives up.
    // Resolving the host the way MockWebServer binds it keeps these tests deterministic.
    private fun newClient() = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                listOf(InetAddress.getByName(server.hostName))
        })
        .retryOnConnectionFailure(false)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun newLoader(maxParallel: Int = 4) =
        ImageLoader(client = newClient(), userAgent = "test-agent", maxParallel = maxParallel)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        loader = newLoader()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns the image at the address`() = runBlocking {
        server.enqueue(pngResponse())

        val bitmap = loader.load(url("/pictures/a.png"))

        assertNotNull(bitmap)
        assertEquals(1, bitmap!!.width)
        assertEquals("/pictures/a.png", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun `sends the user agent`() = runBlocking {
        server.enqueue(pngResponse())

        loader.load(url("/a.png"))

        assertEquals("test-agent", server.takeRequest(2, TimeUnit.SECONDS)?.getHeader("User-Agent"))
    }

    @Test
    fun `follows a redirect to the image, as Wikimedia does`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", url("/thumb/a.png")))
        server.enqueue(pngResponse())

        val bitmap = loader.load(url("/Special:FilePath/a.png"))

        assertNotNull(bitmap)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `returns null for a 404 and does not ask for that address again`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        assertNull(loader.load(url("/missing.png")))
        assertNull(loader.load(url("/missing.png")))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a 404 for one address does not stop another from loading`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(pngResponse())

        assertNull(loader.load(url("/missing.png")))
        assertNotNull(loader.load(url("/there.png")))

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `keeps a loaded image for its address and loads other addresses separately`() = runBlocking {
        server.enqueue(pngResponse())
        server.enqueue(pngResponse())

        assertNotNull(loader.load(url("/a.png")))
        assertNotNull(loader.load(url("/a.png")))
        assertEquals(1, server.requestCount)

        assertNotNull(loader.load(url("/b.png")))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `returns null for a server error but tries again next time`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(pngResponse())

        assertNull(loader.load(url("/a.png")))
        assertNotNull(loader.load(url("/a.png")))

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `returns null when the body is not an image`() = runBlocking {
        server.enqueue(MockResponse().setBody("not an image"))

        assertNull(loader.load(url("/a.png")))
    }

    @Test
    fun `runs no more downloads at once than the limit`() = runBlocking {
        val running = AtomicInteger(0)
        val peak = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val now = running.incrementAndGet()
                peak.accumulateAndGet(now) { a, b -> maxOf(a, b) }
                Thread.sleep(150)
                running.decrementAndGet()
                return pngResponse()
            }
        }
        val limited = newLoader(maxParallel = 2)

        val results = coroutineScope {
            (1..6).map { id -> async(Dispatchers.IO) { limited.load(url("/id$id.png")) } }.awaitAll()
        }

        assertTrue(results.all { it != null })
        assertEquals(2, peak.get())
    }

    @Test
    fun `cancelling a load rethrows and does not remember the address as missing`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                started.complete(Unit)
                return pngResponse().setHeadersDelay(5, TimeUnit.SECONDS)
            }
        }

        val job = launch(Dispatchers.IO) { loader.load(url("/a.png")) }
        withTimeout(2_000) { started.await() }
        job.cancelAndJoin()

        assertTrue(job.isCancelled)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = pngResponse()
        }
        assertNotNull(loader.load(url("/a.png")))
    }
}
