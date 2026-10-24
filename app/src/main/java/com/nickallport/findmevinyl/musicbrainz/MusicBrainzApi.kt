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
        /** How many artists to ask for. MusicBrainz sends 25 when this is left out. */
        @Query("limit") limit: Int? = null,
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

    /**
     * One release (one pressing) with its tracks, label and catalogue number, and the genres of its
     * release group. The `inc` list is sent as it is, because MusicBrainz reads the plus signs as spaces.
     */
    @GET("release/{id}")
    suspend fun getReleaseDetail(
        @Path("id") releaseId: String,
        @Query("inc", encoded = true) include: String = "recordings+labels+release-groups+genres",
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseDetailResponse

    /**
     * A page of every release the artist is credited on, each with the release group it belongs to and
     * its media. All of an artist's albums can be checked for their formats from these pages, a hundred
     * releases a request, instead of one request for each album. The `inc` list is sent as it is.
     */
    @GET("release/")
    suspend fun getReleasesForArtist(
        @Query("artist") artistId: String,
        @Query("inc", encoded = true) include: String = "release-groups+media",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzArtistReleasesResponse

    /**
     * The releases that carry a barcode: [query] is `barcode:` and the number. Each comes with its artist credit, its
     * release group and its media, so one request says who the album is by and what format it is on.
     */
    @GET("release/")
    suspend fun searchReleasesByBarcode(
        @Query("query") query: String,
        @Query("limit") limit: Int = 25,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzBarcodeSearchResponse

    /** Release groups matching a search such as `releasegroup:"The Singles" AND arid:{artist id}`. */
    @GET("release-group/")
    suspend fun searchReleaseGroups(
        @Query("query") query: String,
        @Query("limit") limit: Int = 5,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseGroupSearchResponse

    /**
     * One release of a release group with its tracks and who is credited on each, to find the artists on a compilation.
     * [status] limits it to official releases when given. The `inc` list is sent as it is, because MusicBrainz reads
     * the plus sign as a space.
     */
    @GET("release/")
    suspend fun getReleaseTrackArtists(
        @Query("release-group") releaseGroupId: String,
        @Query("status") status: String? = null,
        @Query("inc", encoded = true) include: String = "recordings+artist-credits",
        @Query("limit") limit: Int = 1,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzTrackArtistsResponse

    /** One release group, with its type and first release date, which a search result leaves out. */
    @GET("release-group/{id}")
    suspend fun getReleaseGroup(
        @Path("id") releaseGroupId: String,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseGroupDto

    /** The rating of one release group: an average out of 5 and how many people voted. */
    @GET("release-group/{id}")
    suspend fun getReleaseGroupRating(
        @Path("id") releaseGroupId: String,
        @Query("inc") include: String = "ratings",
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseGroupRatingResponse

    /** The web links of one artist, such as its Wikidata page and image, for finding its picture. */
    @GET("artist/{id}")
    suspend fun getArtistLinks(
        @Path("id") artistId: String,
        @Query("inc") include: String = "url-rels",
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzArtistLinksResponse

    /** The genres of one artist, each with the number of votes for it. */
    @GET("artist/{id}")
    suspend fun getArtistGenres(
        @Path("id") artistId: String,
        @Query("inc") include: String = "genres",
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzArtistGenresResponse

    /**
     * The releases of one release group with their media, which say whether it came on vinyl, CD and so on.
     * `media+labels` adds each release's label and catalogue number. The `inc` list is sent as it is,
     * because MusicBrainz reads the plus signs as spaces.
     */
    @GET("release/")
    suspend fun getReleasesForReleaseGroup(
        @Query("release-group") releaseGroupId: String,
        @Query("inc", encoded = true) include: String = "media",
        @Query("limit") limit: Int = 100,
        @Query("fmt") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): MusicBrainzReleaseListResponse
}

object AppUserAgent {
    val VALUE = "FindMeVinyl/1.0 (${BuildConfig.MUSICBRAINZ_CONTACT_EMAIL})"
}
