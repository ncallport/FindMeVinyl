package com.nickallport.findmevinyl.nominatim

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): List<NominatimResultDto>
}
