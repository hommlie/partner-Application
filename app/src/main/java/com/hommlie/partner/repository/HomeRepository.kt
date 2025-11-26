package com.hommlie.partner.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.apiclient.WeatherApiInterface
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.CmsPageResponse
import com.hommlie.partner.model.DailyPunchLogResponse
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.JobSummary
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.OnlineOfflineResponse
import com.hommlie.partner.model.SigninSignup
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.VerifyOtp
import com.hommlie.partner.model.WeatherResponse
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toPlainRequestBody
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

class HomeRepository @Inject constructor( private val apiService: ApiInterface, private val weatherApiService : WeatherApiInterface) {

    suspend fun goOnlineOfflineEmp( map: HashMap<String, String>): OnlineOfflineResponse{
        return apiService.goOnlineOfflineEmp(map)
    }

    suspend fun getOrderByOrderStatus( map: HashMap<String, String>): NewOrder{
        return apiService.getOrderByOrderStatus(map)
    }

    suspend fun dailyPuchLog( map: HashMap<String, String>): DailyPunchLogResponse{
        return apiService.dailyPucchLog(map)
    }

    suspend fun getOnsiteJob(map : HashMap<String,String>) : NewOrder{
        return apiService.getOnsiteJob(map)
    }

    suspend fun getCurrentWeather(location: String, apiKey: String): WeatherResponse{
        return weatherApiService.getCurrentWeather(location,apiKey)
    }

    suspend fun saveBill(
        userId: String,
        title: String,
        details: String,
        amount : String,
        expense_date :String,
        imageUris: List<Uri>,
        context: Context
    ): ApiResult<SingleResponse> {
        return try {

            Log.d("BookAppointment", "ImageUris count: ${imageUris.size}")

            val parts = imageUris.mapIndexed { index, uri ->
//                Log.d("BookAppointment", "Image $index URI: $uri")
                CommonMethods.prepareFilePart("images", uri, context)
            }
            /*  //  Log each multipart file’s metadata
              parts.forEachIndexed { index, part ->
                  Log.d("BookAppointment", "Multipart $index headers: ${part.headers}")
              } */

            val response = apiService.saveBill(
                userId.toPlainRequestBody(),
                title.toPlainRequestBody(),
                details.toPlainRequestBody(),
                amount.toPlainRequestBody(),
                expense_date.toPlainRequestBody(),
                parts
            )

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

    suspend fun getExpenseHistory(hashMap: HashMap<String, String>): ApiResult<DynamicSingleResponseWithData<List<ExpenseHistory>>> {
        return safeApiCall { apiService.getBills(hashMap) }
    }



    suspend fun getUserJobData(hashMap: HashMap<String,String>): ApiResult<DynamicSingleResponseWithData<JobSummary>> {
        return try {
            val response = apiService.getUserJobData(hashMap)

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

    suspend fun getCms(): CmsPageResponse {
        return apiService.getCmsData()
    }


}
