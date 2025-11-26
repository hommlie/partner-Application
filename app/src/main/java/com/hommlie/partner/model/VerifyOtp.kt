package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class VerifyOtp(
    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: VerifyData?
)

data class VerifyData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("emp_id")
    val empId: String,

    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("emp_name")
    val empName: String,

    @SerializedName("is_reg_form_submit")
    val is_reg_form_submit: Int,

    @SerializedName("is_verified")
    val is_verified: Int,

    @SerializedName("emp_email")
    val empEmail: String,

    @SerializedName("emp_address")
    val empAddress: String,

    @SerializedName("emp_photo")
    val empPhoto: String,

    @SerializedName("pan_no")
    val panNo: String,

    @SerializedName("designation")
    val designation: String?,

    @SerializedName("blood")
    val blood_group: String?,

    @SerializedName("dob")
    val dob: String?,

    @SerializedName("pan_image")
    val panImage: String,

    @SerializedName("aadhar_no")
    val aadharNo: String,

    @SerializedName("aadhar_image")
    val aadharImage: String
)
