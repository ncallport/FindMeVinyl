package com.nickallport.findmevinyl.overpass

import com.nickallport.findmevinyl.musicbrainz.AppUserAgent
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object OverpassQueryBuilder {
    private val shopTags = listOf("music", "record", "charity", "second_hand")

    fun shopsNear(latitude: Double, longitude: Double, radiusMeters: Int): String {
        val shopFilters = shopTags.joinToString("\n") { tag ->
            "  node[shop=$tag](around:$radiusMeters,$latitude,$longitude);"
        }

        return """
            [out:json];
            (
            $shopFilters
            );
            out center;
        """.trimIndent()
    }
}

interface OverpassApi {
    @POST("interpreter")
    suspend fun query(
        @Body body: RequestBody,
        @Header("User-Agent") userAgent: String = AppUserAgent.VALUE
    ): OverpassResponse
}
