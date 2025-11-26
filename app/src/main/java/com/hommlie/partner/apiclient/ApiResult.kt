package com.hommlie.partner.apiclient

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
    data class UnknownError(val message: String) : ApiResult<Nothing>()
}