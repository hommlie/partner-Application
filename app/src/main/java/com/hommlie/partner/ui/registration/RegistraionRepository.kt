package com.hommlie.partner.ui.registration

import android.util.Log
import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.model.BankDetails
import com.hommlie.partner.model.ContactDetails
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.PersonalDetails
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.UserAboutDetailsData
import com.hommlie.partner.model.WorkZonesData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class RegistraionRepository @Inject constructor(private val apiInterface: ApiInterface) {

    suspend fun getWorkZones(): DynamicSingleResponseWithData<List<WorkZonesData>> {
        return apiInterface.getWorkZones()
    }
    suspend fun getUserAboutDetails(map: HashMap<String, String>) : DynamicSingleResponseWithData<UserAboutDetailsData>{
        return apiInterface.getUserAboutDetails(map)
    }

    suspend fun updateProfilePhoto(user_id: RequestBody, profilePhoto: MultipartBody.Part?) : SingleResponse{
        Log.d("UploadDebug", "Uploading user_id=$user_id")
        Log.d("UploadDebug", "File name=${profilePhoto?.body?.contentLength()} bytes, type=image/jpeg")
        return apiInterface.updateProfilePhoto(user_id,profilePhoto)
    }

    suspend fun savePersonalDetails(map: HashMap<String, String>): DynamicSingleResponseWithData<PersonalDetails> {
        return apiInterface.savePersonalDetails(map)
    }

    suspend fun saveContactDetails(map: HashMap<String, String>): DynamicSingleResponseWithData<ContactDetails> {
        return apiInterface.saveContactDetails(map)
    }

    suspend fun saveBankDetails(map: HashMap<String, String>): DynamicSingleResponseWithData<BankDetails> {
        return apiInterface.saveBankDetails(map)
    }

    suspend fun registerUser(
        userId : String,
        name: String,
        dob: String,
        age: Int,
        gender: String,
        email: String,
        workZone: Int,
        expInYear: Float,
        profilePhoto: MultipartBody.Part?,
        document: MultipartBody.Part?
    ): SingleResponse {
        return apiInterface.registerUser(
            userId = userId.toPlainRequestBody(),
            name = name.toPlainRequestBody(),
            dob = dob.toPlainRequestBody(),
            age = age.toString().toPlainRequestBody(),
            gender = gender.toPlainRequestBody(),
            email = email.toPlainRequestBody(),
            workZone = workZone.toString().toPlainRequestBody(),
            expInYear = expInYear.toString().toPlainRequestBody(),
            profilePhoto = profilePhoto,
            document = document
        )
    }

    fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())


}