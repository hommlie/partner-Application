package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class ScheduleGelServiceRequest(
    @SerializedName("services")
    val services: List<ScheduleService>
)

data class ScheduleService(
    @SerializedName("order_id")
    val orderId: Int,

    @SerializedName("desired_date")
    val desiredDate: String,

    @SerializedName("desired_timeslot")
    val desiredTimeslot: Int
)
