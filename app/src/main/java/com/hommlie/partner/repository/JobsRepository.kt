package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.GelServicesData
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.OrderQuestions
import com.hommlie.partner.model.PaymentLinkResponse
import com.hommlie.partner.model.PaymentStatus
import com.hommlie.partner.model.ScheduleGelServiceRequest
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.SingleResponseForOrderThree
import com.hommlie.partner.model.TimeSlot
import com.hommlie.partner.model.UpdateFilledChemicalRequestBody
import com.hommlie.partner.model.VisitChemicals
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    suspend fun uploadJobCardPhoto(user_id: RequestBody,
                                   orderNo : RequestBody,
                                   srIds : RequestBody,
                                   profilePhoto: MultipartBody.Part?) : ApiResult<SingleResponse>{
        return safeApiCall { apiService.uploadJobCardPhoto(user_id,orderNo,srIds,profilePhoto)}
    }

    suspend fun getGelServices(hashMap: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<GelServicesData>> {
        return safeApiCall { apiService.getGelServices(hashMap) }
    }

    suspend fun scheduleGelService(request: ScheduleGelServiceRequest): ApiResult<SingleResponse> {
        return safeApiCall { apiService.scheduleGelService(request) }
    }

    suspend fun getVisitChemicals(hashMap: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<List<VisitChemicals>>> {
        return safeApiCall { apiService.getVisitChemicals(hashMap) }
    }

    suspend fun updateFilledVisitChemical(request: UpdateFilledChemicalRequestBody): ApiResult<SingleResponse> {
        return safeApiCall { apiService.updateFilledVisitChemical(request) }
    }

    suspend fun getTimeSlots(): ApiResult<DynamicSingleResponseWithData<List<TimeSlot>>> {
        return safeApiCall { apiService.getTimeSlots() }
    }

    suspend fun rescheduleService(hashMap: HashMap<String, String>): ApiResult<SingleResponse> {
        return safeApiCall { apiService.rescheduleService(hashMap) }
    }

}