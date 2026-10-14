package com.nickallport.findmevinyl.image

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
 * Downloads images by address. Loaded images are kept in memory, and so are the addresses that
 * answered 404, so scrolling a list back and forth does not repeat requests. Redirects are
 * followed, as Wikimedia and the Cover Art Archive both redirect to the file. Failures are not
 * remembered, so they are tried again next time.
 */
class ImageLoader(
    private val client: OkHttpClient,
    private val userAgent: String = AppUserAgent.VALUE,
    maxParallel: Int = 4,
    maxCacheBytes: Int = defaultCacheBytes()
) {
    private val semaphore = Semaphore(maxParallel)
    private val images = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = ConcurrentHashMap.newKeySet<String>()

    /** The image at [url], or null when there is none or it could not be loaded. */
    suspend fun load(url: String): Bitmap? {
        images.get(url)?.let { return it }
        if (url in missing) return null
        return semaphore.withPermit { fetch(url) }
    }

    private suspend fun fetch(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
            client.newCall(request).await().use { response ->
                when {
                    response.code == 404 -> {
                        missing.add(url)
                        null
                    }
                    !response.isSuccessful -> null
                    else -> {
                        val bytes = response.body?.bytes() ?: return@use null
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { images.put(url, it) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** A share of the app's memory for pictures that are shown small. */
        fun defaultCacheBytes(divisor: Int = 8): Int =
            (Runtime.getRuntime().maxMemory() / divisor).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        /**
         * The instance the app uses for artist pictures. It lives as long as the process, so its
         * cache outlasts the activity. The pictures are small, so it gets a small share of memory.
         */
        val shared: ImageLoader by lazy {
            ImageLoader(
                OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build(),
                maxCacheBytes = defaultCacheBytes(divisor = 32)
            )
        }
    }
}

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
