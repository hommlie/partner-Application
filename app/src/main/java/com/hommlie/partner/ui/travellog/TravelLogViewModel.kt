package com.hommlie.partner.ui.travellog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.JobItem
import com.hommlie.partner.model.Location
import com.hommlie.partner.model.TravelLogData
import com.hommlie.partner.model.TravelLogResponse
import com.hommlie.partner.repository.TravelLogRepository
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TravelLogViewModel @Inject constructor(
    private val sharePreference: SharePreference,
    private val repository: TravelLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<TravelLogResponse>>(UIState.Idle)
    val uiState: StateFlow<UIState<TravelLogResponse>> = _uiState

    private val _currentDate = MutableStateFlow<String>("")
    val currentDate : StateFlow<String> = _currentDate



//    init {
//        fetchTravelLogs()
////        fetchTravelLogsMock()
//    }

    private fun updateCurrentDate(date: String){
        _currentDate.value = date
    }


    fun fetchTravelLogs(context: Context, date : String) {
        if (!CommonMethods.isInternetAvailable(context)) {
            _uiState.value = UIState.Error("No internet connection")
            return
        }

        viewModelScope.launch {

            val hashMap = HashMap<String,String>()
            hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
            hashMap["date"] = date

            _uiState.value = UIState.Loading

            try {
                val response = repository.getTravelLogs(hashMap)

                if (response.status == 1 && response.data != null) {
                    _uiState.value = UIState.Success(response)
                } else {
                    _uiState.value = UIState.Error(response.message)
                }

            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetUIState() {
        _uiState.value = UIState.Idle
    }

    fun getMockTravelLogResponse(): TravelLogResponse {
        return TravelLogResponse(
            status = 1,
            message = "Mocked travel log fetched successfully.",
            data = TravelLogData(
                date = "2025-07-17",
                total_distance = 15.6,
                start_location = Location(
                    latitude = 19.0760,
                    longitude = 72.8777
                ),
                jobs = listOf(
                    JobItem(
                        job_id = 101,
                        location_name = "Job 1 Location",
                        latitude = 19.0820,
                        longitude = 72.8860,
                        start_time = "10:00 AM",
                        end_time = "11:00 AM",
                        distance_from_previous = 1.2
                    ),
                    JobItem(
                        job_id = 102,
                        location_name = "Job 2 Location",
                        latitude = 19.0900,
                        longitude = 72.8940,
                        start_time = "12:00 PM",
                        end_time = "1:00 PM",
                        distance_from_previous = 2.5
                    ),
                    JobItem(
                        job_id = 103,
                        location_name = "Job 3 Location",
                        latitude = 19.0965,
                        longitude = 72.9010,
                        start_time = "2:00 PM",
                        end_time = "3:00 PM",
                        distance_from_previous = 1.8
                    ),
                    JobItem(
                        job_id = 104,
                        location_name = "Job 4 Location",
                        latitude = 19.1010,
                        longitude = 72.9075,
                        start_time = "4:00 PM",
                        end_time = "5:00 PM",
                        distance_from_previous = 1.3
                    ),
                    JobItem(
                        job_id = 105,
                        location_name = "Job 5 Location",
                        latitude = 19.1080,
                        longitude = 72.9150,
                        start_time = "6:00 PM",
                        end_time = "7:00 PM",
                        distance_from_previous = 2.3
                    )
                )
            )
        )
    }



    fun fetchTravelLogsMock() {
        viewModelScope.launch {
            _uiState.value = UIState.Loading

            delay(1000) // simulate network delay

            val mockResponse = getMockTravelLogResponse()
            _uiState.value = UIState.Success(mockResponse)
        }
    }




}


