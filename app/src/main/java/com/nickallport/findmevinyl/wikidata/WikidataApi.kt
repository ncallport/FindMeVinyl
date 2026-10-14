package com.nickallport.findmevinyl.wikidata

import com.google.gson.JsonElement
import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WikidataApi {
    /** The image (property P18) of a Wikidata item, as the name of a file on Wikimedia Commons. */
    @GET("api.php")
    suspend fun getImageClaims(
        @Query("action") action: String = "wbgetclaims",
        @Query("entity") entityId: String,
        @Query("property") property: String = "P18",
        @Query("format") format: String = "json",
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): WikidataClaimsResponse
}

data class WikidataClaimsResponse(
    val claims: Map<String, List<WikidataClaimDto>>? = null
)

data class WikidataClaimDto(
    val rank: String? = null,
    val mainsnak: WikidataSnakDto? = null
) {
    /** The Commons file name when the claim has a value, such as "Genesis.JPG". Null otherwise. */
    fun fileName(): String? = mainsnak?.datavalue?.value
        ?.takeIf { it.isJsonPrimitive }
        ?.asString
}

data class WikidataSnakDto(
    val datavalue: WikidataDataValueDto? = null
)

/** The value is a string for an image, and other shapes for other properties, so it is read lazily. */
data class WikidataDataValueDto(
    val value: JsonElement? = null
)
