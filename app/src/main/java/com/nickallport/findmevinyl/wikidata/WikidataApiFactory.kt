package com.nickallport.findmevinyl.wikidata

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WikidataApiFactory {
    private const val BASE_URL = "https://www.wikidata.org/w/"

    fun create(): WikidataApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikidataApi::class.java)
    }
}
