package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Follows a short Google Maps link (`maps.app.goo.gl/...`) to the link it stands for, which has the name and address
 * of the place in it. Each redirect is read from the answer's headers and the page itself is never loaded. Only Google
 * Maps addresses are asked for, and a redirect to anywhere else ends it with nothing. No cookies are sent, and the app
 * says who it is. [isAllowed] is what may be asked for, and [maxHops] how many redirects are followed.
 */
class MapsLinkResolver(
    private val isAllowed: (HttpUrl) -> Boolean = { SharedPlaceParser.isMapsLink(it.toString()) },
    private val maxHops: Int = 5,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    /** The address the link leads to, or null when it is not a Maps link, goes elsewhere, loops, or fails. */
    suspend fun resolve(url: String): String? = withContext(Dispatchers.IO) {
        try {
            var current = url.toHttpUrlOrNull() ?: return@withContext null
            repeat(maxHops + 1) {
                if (!isAllowed(current)) return@withContext null
                val request = Request.Builder().url(current).header("User-Agent", AppUserAgent.VALUE).build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code in REDIRECTS -> {
                            val next = response.header("Location")?.let { response.request.url.resolve(it) }
                                ?: return@withContext null
                            current = next
                        }
                        response.isSuccessful -> return@withContext current.toString()
                        else -> return@withContext null
                    }
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}
