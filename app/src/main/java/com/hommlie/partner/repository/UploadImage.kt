package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.UploadPhoto
import okhttp3.MultipartBody
import javax.inject.Inject

class UploadImage @Inject constructor(private val apiService : ApiInterface) {

    suspend fun uploadImage(image: MultipartBody.Part?) : ApiResult<DynamicSingleResponseWithData<UploadPhoto>>{
        return safeApiCall { apiService.uploadPhoto(image)}
    }
}