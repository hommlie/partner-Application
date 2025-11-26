package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class UserAboutDetails(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UserAboutDetailsData?
)

data class UserAboutDetailsData(
    @SerializedName("personal_details")
    val personalDetails: PersonalDetails,

    @SerializedName("bank_details")
    val bankDetails: BankDetails,

    @SerializedName("contact_details")
    val contactDetails: ContactDetails
)

data class PersonalDetails(
    @SerializedName("salutation")
    val salutation: String,

    @SerializedName("first_name")
    val fullName: String?="",

    @SerializedName("name")
    val name: String?="",

    @SerializedName("nationality")
    val nationality: String,

    @SerializedName("gender")
    val gender: String,

    @SerializedName("marital_status")
    val maritalStatus: String,

    @SerializedName("date_of_birth")
    val dateOfBirth: String,

    @SerializedName("emp_photo")
    val profilePhoto: String?,
)

data class BankDetails(
    @SerializedName("account_type")
    val accountType: String,

    @SerializedName("bank_name")
    val bankName: String,

    @SerializedName("branch_name")
    val branchName: String,

    @SerializedName("account_holder_name")
    val accountHolderName: String,

    @SerializedName("account_number")
    val accountNumber: String,

    @SerializedName("ifsc_code")
    val ifscCode: String
)

data class ContactDetails(
    @SerializedName("personal_email")
    val personalEmail: String,

    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("alternate_email")
    val alternateEmail: String?,

    @SerializedName("alternate_mobile")
    val alternateMobile: String?
)
