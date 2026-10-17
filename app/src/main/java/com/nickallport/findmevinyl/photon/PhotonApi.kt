package com.nickallport.findmevinyl.photon

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Photon (komoot's search for OpenStreetMap places), used only to suggest places while the user types.
 * Nominatim's policy forbids search-as-you-type, and Photon is made for it.
 */
interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        // Towns and districts only: the "locality" layer brought industrial estates and farms.
        @Query("layer") layers: List<String> = listOf("city", "district"),
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): PhotonResponseDto
}
