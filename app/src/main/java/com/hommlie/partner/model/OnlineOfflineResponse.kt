package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class OnlineOfflineResponse(
    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: OnlineOfflineData?
)

data class OnlineOfflineData(
    @SerializedName("location")
    val location: String,

    @SerializedName("status_message")
    val status_message: String,

    @SerializedName("statusCode")
    val statusCode: Int,

    @SerializedName("attendance_id")
    val attendance_id:Int
)
