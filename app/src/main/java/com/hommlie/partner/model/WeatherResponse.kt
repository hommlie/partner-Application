package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("currentWeather")
    val currentWeather: CurrentWeather?
)


data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("windSpeed") val windSpeed: Double?,
    @SerializedName("conditions") val conditions: String?
)