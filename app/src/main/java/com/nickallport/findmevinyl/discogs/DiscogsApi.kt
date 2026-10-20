package com.nickallport.findmevinyl.discogs

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * The releases Discogs lists with a barcode. Discogs is asked only when MusicBrainz does not know the barcode. Its search
 * officially asks for a sign-in, but answered without one on 2026-10-20 (25 requests a minute, no key). It must be sent a
 * User-Agent that says what the app is.
 */
interface DiscogsApi {
    @GET("database/search")
    suspend fun searchByBarcode(
        @Query("barcode") barcode: String,
        @Query("type") type: String = "release",
        @Query("per_page") perPage: Int = 10,
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): DiscogsSearchResponse
}

object DiscogsApiFactory {
    private const val BASE_URL = "https://api.discogs.com/"

    fun create(): DiscogsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscogsApi::class.java)
}

data class DiscogsSearchResponse(
    val results: List<DiscogsResultDto>? = null
)

/** One release in a Discogs search: its title is "Artist - Album". */
data class DiscogsResultDto(
    val title: String? = null,
    /** A year, or "0" when Discogs does not know it. */
    val year: String? = null,
    val formats: List<DiscogsFormatDto>? = null,
    val label: List<String>? = null
)

data class DiscogsFormatDto(
    val name: String? = null,
    val qty: String? = null,
    val descriptions: List<String>? = null
)
