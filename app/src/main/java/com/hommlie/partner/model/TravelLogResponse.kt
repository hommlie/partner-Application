package com.hommlie.partner.model

data class TravelLogResponse(
    val status: Int,
    val message: String,
    val data: TravelLogData?
)

data class TravelLogData(
    val date: String,
    val jobs: List<JobItem>?,
    val total_distance: Double,
    val start_location: Location
)

data class JobItem(
    val job_id: Int,
    val location_name: String,
    val latitude: Double,
    val longitude: Double,
    val start_time: String,
    val end_time: String,
    val distance_from_previous: Double
)

data class Location(
    val latitude: Double,
    val longitude: Double
)

