package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.OrderQuestions
import com.hommlie.partner.model.PaymentLinkResponse
import com.hommlie.partner.model.PaymentStatus
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.SingleResponseForOrderThree
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import javax.inject.Inject

class JobsRepository @Inject constructor(private val apiService : ApiInterface) {

    suspend fun getNewJobs(map : HashMap<String,String>) : NewOrder{
        return apiService.getOrderByOrderStatus(map)
    }

    suspend fun getQuestions( map: HashMap<String, String>): OrderQuestions {
        return apiService.getQuestions(map)
    }

    suspend fun submitAnswer(
        params: Map<String, @JvmSuppressWildcards RequestBody>,
        images: List<MultipartBody.Part>
    ): SingleResponse {
        return apiService.submitAnswer(params, images)
    }

    suspend fun changeorderStatus(
        map: Map<String, @JvmSuppressWildcards RequestBody>,
        emp_onsite_image: MultipartBody.Part,
    ): SingleResponseForOrderThree {
        return apiService.changeorderStatus(map,emp_onsite_image)
    }

    suspend fun changeorderStatusWhenCheque(
        map: Map<String, @JvmSuppressWildcards RequestBody>,
        cheque_img: MultipartBody.Part,
    ): SingleResponse {
        return apiService.changeorderStatusWhenCheque(map,cheque_img)
    }
    suspend fun submitReferral(map: Map<String, String>): SingleResponse {
        return apiService.submitReferral(map)
    }

    suspend fun changeorderStatusJobDone(
        map: Map<String, @JvmSuppressWildcards RequestBody>,
//        emp_onsite_image: MultipartBody.Part,
    ): SingleResponse {
        return apiService.changeorderStatusJobDone(map)
    }

    suspend fun sendOtp(map:HashMap<String,String>): SingleResponse{
        return apiService.sendOtp(map)
    }

    suspend fun generatePaymentQr(map:HashMap<String,String>): DynamicSingleResponseWithData<PaymentLinkResponse>{
        return apiService.generateQr(map)
    }

    suspend fun chekcPamentStaus(map:HashMap<String,String>): DynamicSingleResponseWithData<PaymentStatus>{
        return apiService.chekcPamentStaus(map)
    }

    suspend fun raiseTicket(hashMap: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<Any>> {
        return safeApiCall { apiService.raiseTicket(hashMap) }
    }


}