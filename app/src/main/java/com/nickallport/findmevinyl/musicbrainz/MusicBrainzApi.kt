package com.nickallport.findmevinyl.musicbrainz

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import com.nickallport.findmevinyl.BuildConfig

interface MusicBrainzApi {
    @GET("artist/")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzArtistSearchResponse

    @GET("release-group/")
    suspend fun getReleaseGroupsForArtist(
        @Query("artist") artistId: String,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseGroupSearchResponse

}

object AppUserAgent {
    val VALUE = "FindMeVinyl/1.0 (${BuildConfig.MUSICBRAINZ_CONTACT_EMAIL})"
}
