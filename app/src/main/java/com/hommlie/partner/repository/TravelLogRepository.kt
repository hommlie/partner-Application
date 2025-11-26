package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.model.TravelLogResponse
import retrofit2.http.Body
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelLogRepository @Inject constructor(
    private val api: ApiInterface
) {

    suspend fun getTravelLogs(map:HashMap<String,String>): TravelLogResponse{
        return api.getTravelLogs(map)
    }

}