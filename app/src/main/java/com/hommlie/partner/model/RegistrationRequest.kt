package com.hommlie.partner.model

import okhttp3.MultipartBody

data class RegistrationRequest(
    val name: String,
    val dob: String,
    val age: Int,
    val gender: String,
    val email: String,
    val workZone: String,
    val expInYear: Float,
    val profilePhoto: MultipartBody.Part?,
    val document: MultipartBody.Part?
)

