package com.hommlie.partner.model

data class TimeModel(
    val displaytime: String,
    val startHour: Int,
    val time : String,
    var isEnabled: Boolean = true,
    var isSelected: Boolean = false
)
