package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class PaymentLinkResponse(
    @SerializedName("visit_id")
    val orderId: String,

    @SerializedName("order_number")
    val order_number: String,

    @SerializedName("qr_id")
    val qrId: String,

    @SerializedName("qr_status")
    val status: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("upi_link")
    val upiLink: String?=null,

    @SerializedName("qr_image_url")
    val qrImageUrl: String
)
