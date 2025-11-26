package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SigninSignup(
    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: UserData
)

data class UserData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("emp_id")
    val empId: String,

    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("otp")
    val otp: Int
)
