package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class JobSummary(
    @SerializedName("total_jobs")
    val totalJobs: Int,

    @SerializedName("pending_jobs")
    val pendingJobs: Int,

    @SerializedName("completed_jobs")
    val completedJobs: Int,

    @SerializedName("absence_in_this_month")
    val absenceInThisMonth: Int,

    @SerializedName("half_day_in_this_month")
    val halfDayInThisMonth: Int,

    @SerializedName("km_travelled_todays")
    val kmTravelledTodays: Int,

    @SerializedName("date")
    val date: String
)

