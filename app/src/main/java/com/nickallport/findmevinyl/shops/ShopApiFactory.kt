package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.nominatim.NominatimApi
import com.nickallport.findmevinyl.overpass.OverpassApi
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

    fun createOverpass(): OverpassApi {
        val client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }
}