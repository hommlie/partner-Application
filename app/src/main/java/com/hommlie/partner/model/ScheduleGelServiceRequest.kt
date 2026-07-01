package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class ScheduleGelServiceRequest(
    @SerializedName("services")
    val services: List<ScheduleService>
)

data class ScheduleService(
    @SerializedName("order_id")
    val orderId: Int,

    @SerializedName("desired_date")
    val desiredDate: String,

    @SerializedName("desired_timeslot")
    val desiredTimeslot: Int
)

data class UpdateFilledChemicalRequestBody(

    @SerializedName("visit_id")
    val visitId: Int = 0,

    @SerializedName("user_id")
    val userId: Int = 0,

    @SerializedName("chemicals")
    val filledList: List<UpdateFilledChemical>
)

data class UpdateFilledChemical(
    @SerializedName("assigned_inventory_id")
    val assignedInventoryId: Int,

    @SerializedName("used_qty")
    val usedQty: Double,

)
