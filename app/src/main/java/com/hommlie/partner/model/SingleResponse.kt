package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SingleResponse(

    @SerializedName("status")
    val status : Int,

    @SerializedName("message")
    val message : String?

)
