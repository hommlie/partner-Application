package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class WorkZones(
    @SerializedName("status")
    val status : Int,

    @SerializedName("message")
    val message : String,

    @SerializedName("data")
    val workZonesData : List<WorkZonesData>,

)

data class WorkZonesData(
        @SerializedName("id")
        val id : Int,

        @SerializedName("name")
        val name : String,
    )