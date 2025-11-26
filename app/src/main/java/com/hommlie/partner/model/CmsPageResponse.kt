package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class CmsPageResponse(
    @field:SerializedName("privacypolicy")
    val privacypolicy: String? = null,

    @field:SerializedName("termsconditions")
    val termsconditions: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: Int? = null

)
