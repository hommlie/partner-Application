package com.hommlie.partner.model

import java.time.LocalDate

data class DateModel(
    val display: String,
    val date: String,
    val day : String,
    var isSelected: Boolean = false
)
