package com.nickallport.findmevinyl.coverart

import android.graphics.Bitmap
import com.nickallport.findmevinyl.image.ImageLoader
import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Downloads cover art from the Cover Art Archive, by release group id. The work is done by an
 * [ImageLoader]: the cache, the limit on downloads at once, and what is remembered and what is retried.
 */
class CoverArtLoader(
    client: OkHttpClient,
    private val baseUrl: String = CoverArt.BASE_URL,
    userAgent: String = AppUserAgent.VALUE,
    maxParallel: Int = 4,
    maxCacheBytes: Int = ImageLoader.defaultCacheBytes()
) {
    private val images = ImageLoader(client, userAgent, maxParallel, maxCacheBytes)

    /** The cover for a release group, or null when there is none or it could not be loaded. */
    suspend fun load(releaseGroupId: String): Bitmap? = images.load(CoverArt.url(releaseGroupId, baseUrl))

    /** The 500px cover for the detail page. It is kept apart from the small one in the cache. */
    suspend fun loadLarge(releaseGroupId: String): Bitmap? =
        images.load(CoverArt.url(releaseGroupId, baseUrl, CoverArt.LARGE))

    companion object {
        /** The instance the app uses. It lives as long as the process, so its cache outlasts the activity. */
        val shared: CoverArtLoader by lazy {
            CoverArtLoader(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
        }
    }
}
