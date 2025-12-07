package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class DailyPunchLogResponse(

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: DataLogs?=null

)
data class DataLogs(
    @SerializedName("is_active")
    val is_active: Int?,

    @SerializedName("punches")
    val data: List<PunchSession>? = null

)

data class PunchSession(
    @SerializedName("punch_in")
    val punchIn: String,

    @SerializedName("punch_out")
    val punchOut: String?
)