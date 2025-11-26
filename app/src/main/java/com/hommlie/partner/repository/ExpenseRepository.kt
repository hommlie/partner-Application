package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.AdvanceRequests
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import javax.inject.Inject

class ExpenseRepository @Inject constructor(private val apiInterface: ApiInterface) {

    suspend fun getAdvanceRequestsHistory(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<AdvanceRequests>> {
        return safeApiCall { apiInterface.getAdvanceRequests(params) }
    }

    suspend fun addAdvanceRequest(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<Any>> {
        return safeApiCall { apiInterface.addAdvanceRequests(params) }
    }
}