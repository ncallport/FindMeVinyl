package com.nickallport.findmevinyl.coverart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Downloads cover art from the Cover Art Archive. Loaded images are kept in memory, and so are the
 * albums the archive has no art for, so scrolling a list back and forth does not repeat requests.
 * Failures are not remembered, so they are tried again next time.
 */
class CoverArtLoader(
    private val client: OkHttpClient,
    private val baseUrl: String = CoverArt.BASE_URL,
    private val userAgent: String = AppUserAgent.VALUE,
    maxParallel: Int = 4,
    maxCacheBytes: Int = defaultCacheBytes()
) {
    private val semaphore = Semaphore(maxParallel)
    private val images = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = ConcurrentHashMap.newKeySet<String>()

    companion object {
        /** The instance the app uses. It lives as long as the process, so its cache outlasts the activity. */
        val shared: CoverArtLoader by lazy {
            CoverArtLoader(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
        }
    }

    /** The cover for a release group, or null when there is none or it could not be loaded. */
    suspend fun load(releaseGroupId: String): Bitmap? {
        images.get(releaseGroupId)?.let { return it }
        if (releaseGroupId in missing) return null
        return semaphore.withPermit { fetch(releaseGroupId) }
    }

    private suspend fun fetch(releaseGroupId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CoverArt.url(releaseGroupId, baseUrl))
                .header("User-Agent", userAgent)
                .build()
            client.newCall(request).await().use { response ->
                when {
                    response.code == 404 -> {
                        missing.add(releaseGroupId)
                        null
                    }
                    !response.isSuccessful -> null
                    else -> {
                        val bytes = response.body?.bytes() ?: return@use null
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?.also { images.put(releaseGroupId, it) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

private fun defaultCacheBytes(): Int =
    (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isCancelled) response.close() else continuation.resume(response)
        }
    })
}
