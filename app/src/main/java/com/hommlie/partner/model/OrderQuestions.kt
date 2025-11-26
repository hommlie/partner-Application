package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class OrderQuestions(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val serviceData: ServiceData
)

data class ServiceData(
    @SerializedName("id") val id: String,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("desired_date") val desiredDate: String,
    @SerializedName("desired_time") val desiredTime: String,
    @SerializedName("price") val price: String,
    @SerializedName("address") val address: String,
    @SerializedName("order_status") val orderStatus: String,
    @SerializedName("order_ques_count") val orderCount: Int,
    @SerializedName("order_question") val orderQuestions: List<OrderQuestion> ?= null
)

data class OrderQuestion(
    @SerializedName("state") val state: String,
    @SerializedName("question") val questions: List<Questions>
)

data class Questions(
    @SerializedName("id") val id: Int,
    @SerializedName("label") val label: String,
    @SerializedName("type") val type: String,
    @SerializedName("options") val options: String?,
    @SerializedName("required") val required: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

