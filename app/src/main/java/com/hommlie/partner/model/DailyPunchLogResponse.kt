package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class DailyPunchLogResponse(

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<PunchSession>?

)

data class PunchSession(
    @SerializedName("punch_in")
    val punchIn: String,

    @SerializedName("punch_out")
    val punchOut: String?
)