package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class CoinItem(
    @SerializedName("tracking_id")
    val trackingId: String,

    @SerializedName("coins_redeemed")
    val coinsRedeemed: String,

    @SerializedName("converted_amount")
    val convertedAmount: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("status_label")
    val statusLabel: String,

    @SerializedName("admin_note")
    val adminNote: String,

    @SerializedName("created_at")
    val createdAt: String
)

