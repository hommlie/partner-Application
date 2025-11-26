package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SingleResponseForOrderThree(

    @SerializedName("status")
    val status : Int,

    @SerializedName("message")
    val message : String?,

    @SerializedName("onsite_updated_at")
    val onsite_updated_at : String?


)
