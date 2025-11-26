package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class PayslipResponse(
@SerializedName("employee")
val employee: Employee?,

@SerializedName("period")
val period: Period?,

@SerializedName("amounts")
val amounts: Amounts?,

@SerializedName("link")
val link: String?
)

data class Employee(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("code")
    val code: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("aadhaar")
    val aadhaar: String?,

    @SerializedName("pan")
    val pan: String?,

    @SerializedName("doj")
    val doj: String?, // Date of Joining

    @SerializedName("location")
    val location: String?
)

data class Period(
    @SerializedName("month")
    val month: String?,

    @SerializedName("pay_date")
    val payDate: String?, // Payment date

    @SerializedName("working_days")
    val workingDays: Int?
)

data class Amounts(
    @SerializedName("basic_da")
    val basicDa: Double?,

    @SerializedName("hra")
    val hra: Double?,

    @SerializedName("travel_allowance")
    val travelAllowance: Double?,

    @SerializedName("incentives")
    val incentives: Double?,

    @SerializedName("advance_paid")
    val advancePaid: Double?,

    @SerializedName("total_earnings")
    val totalEarnings: Double?,

    @SerializedName("total_deductions")
    val totalDeductions: Double?,

    @SerializedName("net_pay")
    val netPay: Double?,

    @SerializedName("amount_in_words")
    val amountInWords: String?
)
