package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class AdvanceRequests(

    @SerializedName("month")
    val month: Int? = null,

    @SerializedName("year")
    val year: Int? = null,

    @SerializedName("month_name")
    val monthName: String? = null,

    @SerializedName("total_amount")
    val totalAmount: String? = null,

    @SerializedName("approved_amount")
    val approvedAmount: String? = null,

    @SerializedName("rejected_amount")
    val rejectedAmount: String? = null,

    @SerializedName("pending_amount")
    val pendingAmount: String? = null,

    @SerializedName("advance_requests")
    val advanceRequests: List<AdvanceRequestList>? = null
)

data class AdvanceRequestList(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("requested_amount")
    val requestedAmount: String? = null,

    @SerializedName("reason")
    val reason: String? = null,

    @SerializedName("request_date")
    val requestDate: String? = null,

    @SerializedName("approved_amount")
    val approvedAmount: String? = null,

    @SerializedName("approved_by")
    val approvedBy: String? = null,

    @SerializedName("reject_reason")
    val reject_reason: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("attachment")
    val attachment: String? = null
)
