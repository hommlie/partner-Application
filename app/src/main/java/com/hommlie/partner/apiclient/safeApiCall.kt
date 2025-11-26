package com.hommlie.partner.apiclient

import retrofit2.Response
import org.json.JSONObject
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let {
                ApiResult.Success(it)
            } ?: ApiResult.UnknownError("Empty response body")
        } else {
            val errorMsg = response.errorBody()?.string()?.let {
                try {
                    JSONObject(it).optString("message", "Unknown server error")
                } catch (_: Exception) {
                    "Unknown server error"
                }
            } ?: response.message()
            ApiResult.Error(response.code(), errorMsg)
        }
    } catch (e: Exception) {
        when (e) {
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException -> ApiResult.NetworkError
            else -> ApiResult.UnknownError(e.localizedMessage ?: "Unexpected error")
        }
    }
}
