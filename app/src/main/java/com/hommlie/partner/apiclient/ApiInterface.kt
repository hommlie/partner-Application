package com.hommlie.partner.apiclient

import com.hommlie.partner.model.AdvanceRequests
import com.hommlie.partner.model.AttendanceResponse
import com.hommlie.partner.model.BankDetails
import com.hommlie.partner.model.CheckVersionResponse
import com.hommlie.partner.model.ChemicalsResponse
import com.hommlie.partner.model.CmsPageResponse
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.ContactDetails
import com.hommlie.partner.model.DailyPunchLogResponse
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.JobSummary
import com.hommlie.partner.model.LeaderBoardData
import com.hommlie.partner.model.Leaderboardd
import com.hommlie.partner.model.LeaveTypeList
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.OnlineOfflineResponse
import com.hommlie.partner.model.OrderQuestions
import com.hommlie.partner.model.PaymentLinkResponse
import com.hommlie.partner.model.PaymentStatus
import com.hommlie.partner.model.PersonalDetails
import com.hommlie.partner.model.SalaryBreakDown
import com.hommlie.partner.model.SigninSignup
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.SingleResponseForOrderThree
import com.hommlie.partner.model.TravelLogResponse
import com.hommlie.partner.model.UserAboutDetailsData
import com.hommlie.partner.model.VerifyOtp
import com.hommlie.partner.model.WorkZones
import com.hommlie.partner.model.WorkZonesData
import com.hommlie.partner.ui.leaderboard.Leaderboard
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface ApiInterface {

    @POST("register_emp")
    suspend fun registerEmpl(@Body map: HashMap<String, String>): SigninSignup

    @POST("emailverify_emp")
    suspend fun verifyEmpl(@Body map: HashMap<String, String>): VerifyOtp

    @POST("partner/user_verification")
    suspend fun checkProfileVerificationStatus(@Body map: HashMap<String, String>): SingleResponse

    @POST("partner/GetAllChemicals")
    suspend fun getChemicalsHave(@Body map:HashMap<String,String>): ChemicalsResponse

    @POST("partner/GetUnverifiedChemicals")
    suspend fun getNewChemicals(@Body map:HashMap<String,String>): ChemicalsResponse

    @POST("partner/ApproveChemicals")
    suspend fun verifyChemicals(@Body map:HashMap<String,String>): SingleResponse

    @POST("partner/travellog")
    suspend fun getTravelLogs(@Body map:HashMap<String,String>): TravelLogResponse

    @POST("partner/partnerActiveStatus")
    suspend fun goOnlineOfflineEmp(@Body map: HashMap<String, String>): OnlineOfflineResponse

    @GET("cmspages")
    suspend fun getCmsData(): CmsPageResponse

    @POST("partner/newOrders")
    suspend fun getOrderByOrderStatus(@Body map: HashMap<String, String>): NewOrder

    @POST("partner/dailyPuchLog")
    suspend fun dailyPucchLog(@Body map: HashMap<String, String>): DailyPunchLogResponse

    @POST("partner/newOrders")
    suspend fun getOnsiteJob(@Body map : HashMap<String,String>) : NewOrder

    @POST("partner/leavetypelist")
    suspend fun getLeaveTypeList(@Body map : HashMap<String,String>) : LeaveTypeList

    @POST("partner/get_monthly_attendance")
    suspend fun getAttendance(@Body map : HashMap<String,String>): AttendanceResponse

    @POST("partner/newQuestions")
    suspend fun getQuestions(@Body map: HashMap<String, String>): OrderQuestions

    @Multipart
    @POST("partner/newQuestionAnswer")
    suspend fun submitAnswer(
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>
    ): SingleResponse

    @Multipart
    @POST("partner/newChangeStatus")
    suspend fun changeorderStatus(
        @PartMap map: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part emp_onsite_image: MultipartBody.Part,
    ): SingleResponseForOrderThree

    @Multipart
    @POST("partner/newChangeStatus")
    suspend fun changeorderStatusWhenCheque(
        @PartMap map: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part emp_onsite_image: MultipartBody.Part,
    ): SingleResponse


    @Multipart
    @POST("partner/newChangeStatus")
    suspend fun changeorderStatusJobDone(
        @PartMap map: Map<String, @JvmSuppressWildcards RequestBody>,
//        @Part emp_onsite_image: MultipartBody.Part,
    ): SingleResponse


    @POST("partner/newSendOtpAtStart")
    suspend fun sendOtp(@Body map:HashMap<String,String>): SingleResponse

    @GET("partner/workzones")
    suspend fun getWorkZones(): DynamicSingleResponseWithData<List<WorkZonesData>>

    @POST("partner/useraboutdetails")
    suspend fun getUserAboutDetails(@Body map:HashMap<String,String>): DynamicSingleResponseWithData<UserAboutDetailsData>

    @Multipart
    @POST("partner/updateProfilePhoto")
    suspend fun updateProfilePhoto(
        @Part("user_id") userId: RequestBody,
        @Part profilePhoto: MultipartBody.Part?): SingleResponse

    @POST("partner/save_personal_details")
    suspend fun savePersonalDetails(@Body map:HashMap<String,String>):DynamicSingleResponseWithData<PersonalDetails>

    @POST("partner/save_contact_details")
    suspend fun saveContactDetails(@Body map:HashMap<String,String>):DynamicSingleResponseWithData<ContactDetails>

    @POST("partner/save_bank_details")
    suspend fun saveBankDetails(@Body map:HashMap<String,String>):DynamicSingleResponseWithData<BankDetails>

    @POST("partner/paySlip")
    suspend fun getPaySlip(@Body map : HashMap<String,String>): DynamicSingleResponseWithData<SalaryBreakDown>

    @Multipart
    @POST("partner/registeration_form")
    suspend fun registerUser(
        @Part("user_id") userId: RequestBody,
        @Part("name") name: RequestBody,
        @Part("dob") dob: RequestBody,
        @Part("age") age: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("email") email: RequestBody,
        @Part("work_zone_id") workZone: RequestBody,
        @Part("expInYear") expInYear: RequestBody,
        @Part profilePhoto: MultipartBody.Part?,
        @Part document: MultipartBody.Part?
    ): SingleResponse

    @POST("partner/newGenrateQr")
    suspend fun generateQr(@Body map : HashMap<String,String>): DynamicSingleResponseWithData<PaymentLinkResponse>

    @POST("partner/chekcPamentStaus")
    suspend fun chekcPamentStaus(@Body map : HashMap<String,String>): DynamicSingleResponseWithData<PaymentStatus>

    @Multipart
    @POST("partner/saveBill")
    suspend fun saveBill(
        @Part("user_id") user_id: RequestBody,
        @Part("title") title: RequestBody,
        @Part("details") details: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("expense_date") expense_date: RequestBody,
        @Part items: List<MultipartBody.Part>
    ): Response<SingleResponse>

    @POST("partner/getBills")
    suspend fun getBills(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<List<ExpenseHistory>>>

    @POST("partner/getUserJobData")
    suspend fun getUserJobData(@Body map : HashMap<String,String>): Response<DynamicSingleResponseWithData<JobSummary>>

    @POST("partner/deleteaccount")
    suspend fun deleteAccount(@Body map: HashMap<String, String>): SingleResponse

    @GET("checkVersion")
    suspend fun checkVersion(): Response<CheckVersionResponse>

    @POST("partner/help")
    suspend fun raiseTicket(@Body hashMap: HashMap<String, String>) : Response<DynamicSingleResponseWithData<Any>>

    @POST("partner/getSalaryAdvance")
    suspend fun getAdvanceRequests(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<AdvanceRequests>>

    @POST("partner/addSalaryAdvance")
    suspend fun addAdvanceRequests(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<Any>>

    @POST("partner/leaderboardCoins")
    suspend fun getLeaderBoard(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<Leaderboardd>>

    @POST("partner/redeemHistory")
    suspend fun getRedeemCoinsHistory(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<List<CoinItem>>>

    @POST("partner/coinBalance")
    suspend fun getCoinBalance(@Body hashMap: HashMap<String,String>) : Response<DynamicSingleResponseWithData<String>>

}