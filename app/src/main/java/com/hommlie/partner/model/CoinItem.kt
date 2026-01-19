package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class CoinItem(

    @SerializedName("redeemHistory")
    val redeemedData: List<RedeemedData>?=null

)
data class RedeemedData(
    @SerializedName("id")
    val trackingId: Int,

    @SerializedName("emp_id")
    val empId: Int,

    @SerializedName("redeemable_item_id")
    val redeemableItemId: Int,

    @SerializedName("coins_redeemed")
    val coinsRedeemed: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("admin_note")
    val adminNote: String?,

    @SerializedName("gifted_reference")
    val giftedReference: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("item_name")
    val itemName: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("points_required")
    val pointsRequired: Int,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("item_image_full_url")
    val itemImageFullUrl: String,

    @SerializedName("gifted_reference_full_url")
    val giftedReferenceFullUrl: String?,

    @SerializedName("status_text")
    val statusLabel: String

)

