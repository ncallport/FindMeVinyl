package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.overpass.OverpassApi
import com.nickallport.findmevinyl.photon.PhotonApi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ShopApiFactory {
    fun createNominatim(): NominatimApi {
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }

    fun createPhoton(): PhotonApi {
        return Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonApi::class.java)
    }

    // The first server is OpenStreetMap France's, which answered in a few seconds when overpass-api.de took
    // 30 to 45 seconds or failed. It gets a short wait, because a good answer is quick, and the slower
    // overpass-api.de is the fallback with a longer one.
    fun createOverpass(): OverpassApi =
        overpass("https://overpass.openstreetmap.fr/api/", readTimeoutSeconds = 15)

    fun createOverpassFallbacks(): List<OverpassApi> =
        listOf(overpass("https://overpass-api.de/api/", readTimeoutSeconds = 30))

    private fun overpass(baseUrl: String, readTimeoutSeconds: Long): OverpassApi {
        val client = OkHttpClient.Builder()
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }
}