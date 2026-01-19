package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.AdvanceRequests
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.RewardItem
import javax.inject.Inject

class ExpenseRepository @Inject constructor(private val apiInterface: ApiInterface) {

    suspend fun getAdvanceRequestsHistory(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<AdvanceRequests>> {
        return safeApiCall { apiInterface.getAdvanceRequests(params) }
    }

    suspend fun addAdvanceRequest(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<Any>> {
        return safeApiCall { apiInterface.addAdvanceRequests(params) }
    }

    suspend fun getRedeemCoinsHistory(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<CoinItem>> {
        return safeApiCall { apiInterface.getRedeemCoinsHistory(params) }
    }
    suspend fun getCoinBalance(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<String>> {
        return safeApiCall { apiInterface.getCoinBalance(params) }
    }
    suspend fun clickRedeem(params: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<Any>> {
        return safeApiCall { apiInterface.clickRedeem(params) }
    }
    suspend fun getRewardItems(): ApiResult<DynamicSingleResponseWithData<List<RewardItem>>> {
        return safeApiCall { apiInterface.getRewardItems() }
    }
}