package com.hommlie.partner.apiclient

import retrofit2.Response
import org.json.JSONObject
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/*suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
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
} */

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): ApiResult<T> {

    return try {

        val response = apiCall()

        if (response.isSuccessful) {

            response.body()?.let {
                ApiResult.Success(it)
            } ?: ApiResult.UnknownError("Empty response body")

        } else {

            val errorMessage = try {

                response.errorBody()?.string()?.let { errorBody ->

                    JSONObject(errorBody)
                        .optString("message")
                        .ifBlank {
                            response.message().ifBlank { "Unknown server error" }
                        }

                } ?: response.message().ifBlank { "Unknown server error" }

            } catch (_: Exception) {

                response.message().ifBlank { "Unknown server error" }

            }

            ApiResult.Error(
                response.code(),
                errorMessage
            )
        }

    } catch (e: CancellationException) {

        // Never swallow coroutine cancellation
        throw e

    } catch (e: UnknownHostException) {

        ApiResult.NetworkError

    } catch (e: ConnectException) {

        ApiResult.NetworkError

    } catch (e: SocketTimeoutException) {

        ApiResult.NetworkError

    } catch (e: SSLHandshakeException) {

        ApiResult.NetworkError

    } catch (e: SSLException) {

        ApiResult.NetworkError

    } catch (e: IOException) {

        ApiResult.NetworkError

    } catch (e: HttpException) {

        ApiResult.Error(
            code = e.code(),
            message = e.message().orEmpty().ifBlank {
                "HTTP ${e.code()} Error"
            }
        )

    } catch (e: Exception) {

        ApiResult.UnknownError(
            e.localizedMessage ?: "Unexpected error"
        )
    }
}
