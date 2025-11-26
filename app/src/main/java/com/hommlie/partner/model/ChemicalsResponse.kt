package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class ChemicalsResponse(
    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<Chemical>?
)

data class Chemical(
    @SerializedName("id")
    val id: Int,

    @SerializedName("category")
    val category: String,

    @SerializedName("subCategory")
    val subCategory: String,

    @SerializedName("batch_number")
    val batch_number : String?=null,

    @SerializedName("quantity")
    val quantity: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("price")
    val price: String,

    @SerializedName("updated_at")
    val updatedAt :String,

    @SerializedName("created_at")
    val createdAt: String,

    var isSelected: Boolean = false,
    var isCheckboxVisible: Boolean = false
)

