package com.nickallport.findmevinyl.musicbrainz

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MusicBrainzApiFactory {
    private const val BASE_URL = "https://musicbrainz.org/ws/2/"

    fun create(): MusicBrainzApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)
    }
}
