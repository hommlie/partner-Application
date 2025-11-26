package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiClient
import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.model.CheckVersionResponse
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.JobSummary
import com.hommlie.partner.model.SigninSignup
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.VerifyOtp
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiInterface
) {
    suspend fun registerEmployee(data: HashMap<String, String>): SigninSignup {
        return apiService.registerEmpl(data)
    }

    suspend fun verifyEmpl(data : HashMap<String, String>): VerifyOtp {
        return apiService.verifyEmpl(data)
    }

    suspend fun checkProfileVerificationStatus(data : HashMap<String, String>): SingleResponse {
        return apiService.checkProfileVerificationStatus(data)
    }

    suspend fun deleteAccount(data: HashMap<String, String>): SingleResponse {
        return apiService.deleteAccount(data)
    }

    suspend fun checkAppVersion(): ApiResult<CheckVersionResponse> {
        return try {
            val response = apiService.checkVersion()

            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error(response.code(), "Empty response body")
            } else {
                val errorMsg = try {
                    val errorJson = response.errorBody()?.use { it.string() }
                    if (!errorJson.isNullOrEmpty()) {
                        JSONObject(errorJson).optString("message", "Unknown error")
                    } else {
                        response.message()
                    }
                } catch (e: Exception) {
                    response.message()
                }
                ApiResult.Error(response.code(), errorMsg)
            }
        } catch (e: Exception) {
            ApiResult.Error(-1, e.localizedMessage ?: "Unknown error")
        }
    }

}
