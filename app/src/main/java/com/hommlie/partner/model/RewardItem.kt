package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class RewardItem(

    @SerializedName("id")
    val id: Int,

    @SerializedName("item_name")
    val productName: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("points_required")
    val requiredCoin: Int,

    @SerializedName("stock_quantity")
    val stockQuantity: Int,

    @SerializedName("is_active")
    val isActive: Int,

    @SerializedName("expiry_date")
    val expiryDate: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("image_full_url")
    val imageRes: String,

    @SerializedName("is_expired")
    val isExpired: Int

)

