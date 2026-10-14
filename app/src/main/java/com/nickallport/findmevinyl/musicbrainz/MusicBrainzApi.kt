package com.nickallport.findmevinyl.musicbrainz

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
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
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseGroupSearchResponse

    /** The web links of one artist, such as its Wikidata page and image, for finding its picture. */
    @GET("artist/{id}")
    suspend fun getArtistLinks(
        @Path("id") artistId: String,
        @Query("inc") include: String = "url-rels",
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzArtistLinksResponse

    /** The releases of one release group with their media, which say whether it came on vinyl, CD and so on. */
    @GET("release/")
    suspend fun getReleasesForReleaseGroup(
        @Query("release-group") releaseGroupId: String,
        @Query("inc") include: String = "media",
        @Query("limit") limit: Int = 100,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseListResponse
}

object AppUserAgent {
    val VALUE = "FindMeVinyl/1.0 (${BuildConfig.MUSICBRAINZ_CONTACT_EMAIL})"
}
