package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class OrderQuestions(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val serviceData: ServiceData
)

data class ServiceData(
    @SerializedName("visit_id") val id: String,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("inspection_type") val inspectionType: Int,
    @SerializedName("order_question_count") val orderCount: Int,
    @SerializedName("services") val orderQuestions: List<OrderQuestionByService> ?= null
)
data class OrderQuestionByService(
    @SerializedName("service_id") val service_id: Int,
    @SerializedName("product_name") val service_name: String,
    @SerializedName("questions") val questions: List<Questions>
)
data class Questions(
    @SerializedName("id") val id: Int,
    @SerializedName("label") val label: String,
    @SerializedName("type") val type: String,
    @SerializedName("options") val options: String?,
    @SerializedName("required") val required: String,
    @SerializedName("status") val status: String
)
data class DaoCollectAllQuestionsOfAllServices(
    @SerializedName("service_id") val service_id: Int,
    @SerializedName("product_name") val service_name: String,
    @SerializedName("id") val id: Int,
    @SerializedName("label") val label: String,
    @SerializedName("type") val type: String,
    @SerializedName("options") val options: String?,
    @SerializedName("required") val required: String,
    @SerializedName("status") val status: String
)

