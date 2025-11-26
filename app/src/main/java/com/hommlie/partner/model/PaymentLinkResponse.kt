package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class PaymentLinkResponse(
    @SerializedName("order_id")
    val orderId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("qr_id")
    val qrId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("amount")
    val amount: Long,

    @SerializedName("upi_link")
    val upiLink: String,

    @SerializedName("qr_image_url")
    val qrImageUrl: String
)
