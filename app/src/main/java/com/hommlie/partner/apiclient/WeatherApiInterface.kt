package com.hommlie.partner.apiclient

import com.hommlie.partner.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiInterface {

    @GET("v1/weather:search")
    suspend fun getCurrentWeather(
        @Query("location") location: String, // "lat,lng"
        @Query("key") apiKey: String
    ): WeatherResponse

}