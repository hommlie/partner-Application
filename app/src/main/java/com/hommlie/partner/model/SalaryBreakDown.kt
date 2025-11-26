package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class SalaryBreakDown(

    @SerializedName("name")
    val name : String,

    @SerializedName("amount")
    val amount : String
)

data class BreakDown(

    @SerializedName("salary_breakdown")
    val salay_breakdown : List<SalaryBreakDown>,

    @SerializedName("deduction_breakdown")
    val deduction_breakdown : List<SalaryBreakDown>
)
