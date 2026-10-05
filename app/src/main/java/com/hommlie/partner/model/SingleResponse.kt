package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SingleResponse(

    @SerializedName("status")
    val status : Int,

    @SerializedName("message")
    val message : String?
)

data class UploadPhoto(

    @SerializedName("image_name")
    val imageName : String,

    @SerializedName("image_url")
    val imageUrl : String
)
