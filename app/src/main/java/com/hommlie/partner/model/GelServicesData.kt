package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class GelServicesData(
    @SerializedName("services")
    val services: List<Service>? = emptyList(),

    @SerializedName("timeslots")
    val timeslots: List<TimeSlot>? = emptyList()
)
data class Service(

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("product_name")
    val productName: String? = "",

    @SerializedName("gel_service_in")
    val gelServiceIn: Int? = 0,

    @SerializedName("expected_service_date")
    val expectedServiceDate: String? = "",

    @SerializedName("min_date")
    val minDate: String? = "",

    @SerializedName("max_date")
    val maxDate: String? = ""
)

data class TimeSlot(

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("label")
    val label: String? = "",

    @SerializedName("start_time")
    val startHour: String? = null,

    var isSelected: Boolean  = false,
    var isEnabled: Boolean = true,
)