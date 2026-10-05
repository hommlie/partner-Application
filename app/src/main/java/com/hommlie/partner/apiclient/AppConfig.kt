package com.hommlie.partner.apiclient

object AppConfig {

    private enum class Environment {
        DEV,
        PROD
    }

    private val CURRENT_ENV = Environment.PROD

    private data class Config(
        val baseUrl: String,
        val imageBaseUrl : String,
        val weatherBaseUrl : String,
        val appName: String
    )

    private val config = when (CURRENT_ENV) {

        Environment.DEV -> Config(
            baseUrl = "https://peru-ape-316708.hostingersite.com/public/api/",
            imageBaseUrl = "hhttps://peru-ape-316708.hostingersite.com/public/storage/app/public/images/question_answers/temp/",
            weatherBaseUrl = "https://weather.googleapis.com/",
            appName = "Hommlie Partner Dev"
        )

        Environment.PROD -> Config(
            baseUrl = "https://www.hommlie.com/panel/public/api/",
            imageBaseUrl = "https://www.hommlie.com/panel/public/storage/app/public/images/question_answers/temp/",
            weatherBaseUrl = "https://weather.googleapis.com/",
            appName = "Hommlie Partner"
        )
    }

    val BASE_URL get() = config.baseUrl
    val WEATHER_BASE_URL get() = config.weatherBaseUrl

    val APP_NAME get() = config.appName
    val IMAGE_BASE_URL get() = config.imageBaseUrl

    val IS_PRODUCTION get() = CURRENT_ENV == Environment.PROD

    val IS_DEBUG get() = !IS_PRODUCTION
}