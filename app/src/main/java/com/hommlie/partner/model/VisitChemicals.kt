package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class VisitChemicals(

    @SerializedName("id")
    val id: Int,

    @SerializedName("inventory_id")
    val inventoryId: Int,

    @SerializedName("chemical_name")
    val chemicalName: String ?= null,

    @SerializedName("quantity")
    val quantity: String ?= null,

    @SerializedName("type")
    val unit: String ?= null,

)
