package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class DynamicSingleResponseWithData<T>(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)
