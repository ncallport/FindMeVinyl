package com.nickallport.findmevinyl.musicbrainz

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Lets requests through to MusicBrainz one at a time, in order, each starting at least
 * [minIntervalMillis] after the one before started. MusicBrainz allows about one request a second.
 *
 * A request waits for its turn inside [run]. Cancelling it while it waits costs nothing, because
 * nothing has been sent. Once its turn comes the request runs to the end even if the caller is
 * cancelled, so whatever it does with the answer, such as saving it, is not cut off halfway.
 *
 * After MusicBrainz answers 429 or 503, nothing starts for [busyPauseMillis].
 */
class MusicBrainzThrottle(
    private val minIntervalMillis: Long = 1_100,
    private val busyPauseMillis: Long = 5_000,
    private val isBusy: (Throwable) -> Boolean = { it is HttpException && it.code() in setOf(429, 503) }
) {
    private val turn = Mutex()
    private var lastStartNanos: Long? = null
    private var pausedUntilNanos: Long? = null

    suspend fun <T> run(request: suspend () -> T): T = turn.withLock {
        waitForTurn()
        lastStartNanos = System.nanoTime()
        withContext(NonCancellable) {
            try {
                request()
            } catch (e: Throwable) {
                if (isBusy(e)) pausedUntilNanos = System.nanoTime() + busyPauseMillis * NANOS_PER_MILLI
                throw e
            }
        }
    }

    /**
     * For a request block that makes more than one request: waits until the next one may start,
     * [minIntervalMillis] after the previous one started. Call it before every request after the
     * first, inside [run].
     */
    suspend fun awaitNextSlot() {
        waitForTurn()
        lastStartNanos = System.nanoTime()
    }

    private suspend fun waitForTurn() {
        val now = System.nanoTime()
        val afterInterval = lastStartNanos?.plus(minIntervalMillis * NANOS_PER_MILLI) ?: now
        val earliest = maxOf(afterInterval, pausedUntilNanos ?: now)
        val waitMillis = (earliest - now) / NANOS_PER_MILLI
        if (waitMillis > 0) delay(waitMillis)
    }

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000L

        /** The instance the app uses, so everything that asks MusicBrainz for formats shares one queue. */
        val shared: MusicBrainzThrottle by lazy { MusicBrainzThrottle() }
    }
}
