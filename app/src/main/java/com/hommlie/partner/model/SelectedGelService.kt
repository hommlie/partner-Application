package com.hommlie.partner.model

data class SelectedGelService(
    val orderId: Int,
    var selectedDate: String? = null,
    var selectedTimeSlotId: Int? = null
)
