package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class CheckVersionResponse(
    @field:SerializedName("status")
    val status : Int,

    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("version_code_partner")
    val versionCode : Int,
)
