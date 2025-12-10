package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SalaryBreakDown(
    val emp_name: String,
    val emp_id: String,
    val emp_phone: String,
    val uin_no: String?=null,
    val aadhar_no: String?=null,
    val selectedMonthWords: String,
    val cycleStart: String,
    val cycleEnd: String,
    val presentDays: Int?,
    val paidLeaves: Int?,
    val earnings: String,
    val extra: String,
    val grossSalary: String,
    val basic: String,
    val hra: String,
    val conveyance: String,
    val medicalAllowance: String,
    val groomingAllowance: String,
    val location: String,
    val travel_allowance: String,
    val advance: String,
    val cin_no: String?=null,
    val pay_date: String,
    val total_earnings: String,
    val total_deductions: String,
    val net_pay: String?
)

