package com.nickallport.findmevinyl.musicbrainz

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class MusicBrainzThrottleTest {

    private fun nowMillis() = System.nanoTime() / 1_000_000

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun `returns what the request returns`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0)

        assertEquals("done", throttle.run { "done" })
    }

    @Test
    fun `starts the first request at once`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 5_000)

        val startedAt = nowMillis()
        throttle.run { }

        assertTrue(nowMillis() - startedAt < 2_000)
    }

    @Test
    fun `starts each request at least the interval after the one before`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 150)
        val starts = Collections.synchronizedList(mutableListOf<Long>())

        (1..3).map { launch { throttle.run { starts.add(nowMillis()) } } }.joinAll()

        assertEquals(3, starts.size)
        assertTrue("gaps were ${starts.zipWithNext { a, b -> b - a }}", starts.zipWithNext { a, b -> b - a }.all { it >= 130 })
    }

    @Test
    fun `runs requests one at a time and in the order they were asked for`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0)
        val running = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val order = Collections.synchronizedList(mutableListOf<Int>())

        (1..4).map { id ->
            launch {
                throttle.run {
                    peak.accumulateAndGet(running.incrementAndGet()) { a, b -> maxOf(a, b) }
                    order.add(id)
                    delay(40)
                    running.decrementAndGet()
                }
            }
        }.joinAll()

        assertEquals(1, peak.get())
        assertEquals(listOf(1, 2, 3, 4), order)
    }

    @Test
    fun `a request cancelled while it waits is never made`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 600)
        val made = Collections.synchronizedList(mutableListOf<String>())

        val first = launch { throttle.run { made.add("first") } }
        first.join()
        val second = launch { throttle.run { made.add("second") } }
        delay(100)
        second.cancelAndJoin()
        delay(700)

        assertEquals(listOf("first"), made.toList())
    }

    @Test
    fun `a request that has started finishes even if its caller is cancelled`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0)
        val started = CompletableDeferred<Unit>()
        var finished = false

        val job = launch {
            throttle.run {
                started.complete(Unit)
                delay(200)
                finished = true
            }
        }
        withTimeout(2_000) { started.await() }
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue("the request should have run to the end", finished)
    }

    @Test
    fun `passes on the error a request fails with and carries on with the next`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0)

        try {
            throttle.run { throw IOException("no network") }
            fail("expected an IOException")
        } catch (e: IOException) {
            assertEquals("no network", e.message)
        }

        assertEquals("next", throttle.run { "next" })
    }

    @Test
    fun `pauses after MusicBrainz says it is busy`() = runBlocking {
        listOf(503, 429).forEach { code ->
            val throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 400)

            val failedAt = nowMillis()
            try {
                throttle.run { throw httpError(code) }
                fail("expected an HttpException")
            } catch (e: HttpException) {
                assertEquals(code, e.code())
            }
            var nextStartedAt = 0L
            throttle.run { nextStartedAt = nowMillis() }

            assertTrue("after $code the next started ${nextStartedAt - failedAt} ms later", nextStartedAt - failedAt >= 380)
        }
    }

    @Test
    fun `a turn that makes several requests waits for its next slot between them`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 200)
        val starts = Collections.synchronizedList(mutableListOf<Long>())

        throttle.run {
            starts.add(nowMillis())
            throttle.awaitNextSlot()
            starts.add(nowMillis())
            throttle.awaitNextSlot()
            starts.add(nowMillis())
        }

        val gaps = starts.zipWithNext { a, b -> b - a }
        assertTrue("gaps were $gaps", gaps.all { it >= 180 })
    }

    @Test
    fun `the next turn starts after the last request of a turn that made several`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 200)
        var lastInsideStart = 0L

        throttle.run {
            throttle.awaitNextSlot()
            lastInsideStart = nowMillis()
        }
        var nextStart = 0L
        throttle.run { nextStart = nowMillis() }

        assertTrue("next turn started ${nextStart - lastInsideStart} ms after the last request", nextStart - lastInsideStart >= 180)
    }

    @Test
    fun `waiting for the next slot is not needed when enough time has passed`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 100)

        throttle.run {
            delay(150)
            val before = nowMillis()
            throttle.awaitNextSlot()
            assertTrue(nowMillis() - before < 60)
        }
    }

    @Test
    fun `does not pause after other errors`() = runBlocking {
        val throttle = MusicBrainzThrottle(minIntervalMillis = 0, busyPauseMillis = 2_000)

        listOf<Throwable>(IOException("x"), httpError(404), httpError(500)).forEach { error ->
            val failedAt = nowMillis()
            try {
                throttle.run { throw error }
            } catch (e: Throwable) {
                // expected
            }
            throttle.run { }
            assertFalse("paused after $error", nowMillis() - failedAt >= 1_500)
        }
    }
}
