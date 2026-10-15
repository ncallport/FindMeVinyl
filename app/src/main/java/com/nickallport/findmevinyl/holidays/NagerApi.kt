package com.nickallport.findmevinyl.holidays

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Nager.Date, a free public service that lists the public holidays of a country for a year. Only a
 * country code and a year are sent. The user agent carries no contact address, because this service
 * does not ask for one.
 */
interface NagerApi {
    @GET("PublicHolidays/{year}/{country}")
    suspend fun publicHolidays(
        @Path("year") year: Int,
        @Path("country") countryCode: String,
        @Header("User-Agent") userAgent: String = NagerApiFactory.USER_AGENT
    ): List<NagerHolidayDto>
}

/** Everything can be missing. [counties] are ISO 3166-2 codes such as GB-SCT. Null means the whole country. */
data class NagerHolidayDto(
    val date: String? = null,
    val counties: List<String>? = null,
    val types: List<String>? = null
)

object NagerApiFactory {
    const val USER_AGENT = "FindMeVinyl/1.0"

    fun create(): NagerApi {
        return Retrofit.Builder()
            .baseUrl("https://date.nager.at/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NagerApi::class.java)
    }
}
