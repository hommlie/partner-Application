package com.hommlie.partner.model

data class SubmitRequest(
    val visit_id: Int,
    val order_status: Int,
    val user_id: Int,
    val services: List<ServiceAnswer>
)

data class ServiceAnswer(
    val order_id: Int,
    val answers: List<DaocollectAnswer>
)

