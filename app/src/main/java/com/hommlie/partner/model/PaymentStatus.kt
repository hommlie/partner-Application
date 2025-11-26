package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class PaymentStatus(

    @SerializedName("payment_status")
    val payment_status : Int

)
