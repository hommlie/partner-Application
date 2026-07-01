package com.hommlie.partner.model

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class NewOrder(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<NewOrderData>?
)

data class NewOrderData(
    @SerializedName("visit_id") val orderId: Int,
    @SerializedName("OnSiteQuestions") val OnSiteQuestions: Int,
    @SerializedName("OnCompletedQuestions") val OnCompletedQuestions: Int,
    @SerializedName("order_number") val orderNo: String,
    @SerializedName("full_name") val name: String,
    @SerializedName("payment_type") var payment_type: String,
    @SerializedName("payment_staus") var payment_status: String,
//    @SerializedName("product_name") val serviceName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("amount_to_collect") val price: String,
    @SerializedName("desired_date") val desiredDate: String,
    @SerializedName("desired_time") val desiredTime: String,
    @SerializedName("timeslot") val timeSlot: TimeSlot ?= null,
    @SerializedName("address") val address: String,
    @SerializedName("address_lat_lng") val address_lat_lng: String,
    @SerializedName("order_status") val orderStatus: String?,
    @SerializedName("emp_onsite_image") val emp_onsite_image :String?,
    @SerializedName("IsOnsiteQuestionsSubmitted") val IsOniteQuestionsSubmitted :Int?,
    @SerializedName("IsOnCompletedQuestionsSubmitted") val isoncompletedQuestionSubmtted :Int?,
    @SerializedName("order_mode") val orderMode: Int,
    @SerializedName("order_mode_label") val orderModeLabel: String,
    @SerializedName("onsite_updated_at") val onsite_updated_at: String,

    @SerializedName("services") val services: List<ServiceModel>

//    @SerializedName("is_openable") val is_Order_canProceed: String,
//    @SerializedName("quantity") val quantity: String,
//    @SerializedName("quantity_type") val quantityType: String,
)
data class ServiceModel(
    @SerializedName("id") val id: String,
    @SerializedName("product_name") val serviceName: String,
    @SerializedName("service_type") val service_type: String?=null,
    @SerializedName("duration") val duration: String?=null,
    @SerializedName("variation") val variation: String,
    @SerializedName("service_type_label") val attribute: String? =null,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("subcategory_name") val subcategoryName: String,
    var localImageUri: Uri? = null
//    @SerializedName("IsQuestionsSubmitted") val isQuestionsSubmitted: Int
)

