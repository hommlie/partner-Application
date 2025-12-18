package com.hommlie.partner.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.DailyPunchLogResponse
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.HomeOptionModel
import com.hommlie.partner.model.JobSummary
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.model.OnlineOfflineResponse
import com.hommlie.partner.model.PunchSession
import com.hommlie.partner.model.WeatherResponse
import com.hommlie.partner.repository.HomeRepository
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel  @Inject constructor(
    private val repository: HomeRepository,
    private val sharePreference: SharePreference
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<OnlineOfflineResponse>>(UIState.Idle)
    val uiState: StateFlow<UIState<OnlineOfflineResponse>> = _uiState

    private val _isOnsiteService = MutableStateFlow<Boolean?>(null)
    val isOnsiteService: StateFlow<Boolean?> = _isOnsiteService

    private val _homeoptions_uiState = MutableStateFlow<UIState<List<HomeOptionModel>>>(UIState.Idle)
    val homeoptions_uiState: StateFlow<UIState<List<HomeOptionModel>>> = _homeoptions_uiState

    private val _timerText = MutableStateFlow("00:00:00")
    val timerText: StateFlow<String> = _timerText

    private val _dailyPuchLog = MutableStateFlow<UIState<DailyPunchLogResponse>>(UIState.Idle)
    val dailyPunchLog : StateFlow<UIState<DailyPunchLogResponse>> = _dailyPuchLog

    private val _weather = MutableStateFlow<UIState<WeatherResponse>>(UIState.Idle)
    val weather: StateFlow<UIState<WeatherResponse>> = _weather

    private val _onsiteJob = MutableStateFlow<UIState<NewOrder>>(UIState.Idle)
    val onsiteJob : StateFlow<UIState<NewOrder>> = _onsiteJob

    private var timerJob: Job? = null

    private val _jobDataUiState = MutableStateFlow<UIState<DynamicSingleResponseWithData<JobSummary>>>(UIState.Idle)
    val jobDataUiState : StateFlow<UIState<DynamicSingleResponseWithData<JobSummary>>> = _jobDataUiState


    init {
        fetchHomeOptionsMock()
    }


    fun goOnlineOffline(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            try {
                val response = repository.goOnlineOfflineEmp(data)
                if (response.status == 1 && response.data != null) {

                    _uiState.value = UIState.Success(response)
                } else {
                    _uiState.value = UIState.Error(response.message ?: "Empty data")
                }
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.localizedMessage ?: "Error occurred")
            }
        }
    }

    fun getDailyPuchLog(map : HashMap<String,String>){
        viewModelScope.launch {
            _dailyPuchLog.value = UIState.Loading
            try{
                val respone = repository.dailyPuchLog(map)  //getMockDailyPunchLogResponse()

                if (respone.data !=null){
                    _dailyPuchLog.value = UIState.Success(respone)
                }else{
                    _dailyPuchLog.value = UIState.Error(respone.message ?: "Punch logs empty")
                }

            }catch (e : Exception){
                _dailyPuchLog.value = UIState.Error(e.localizedMessage ?: "Error occurred")
            }
        }
    }

    fun getOnsiteJob(userId: String){

        val hashMap = HashMap<String,String>()
        hashMap["user_id"] = userId
        hashMap["order_status"] = "3"

        viewModelScope.launch {
            _onsiteJob.value = UIState.Loading
            try {
                val response = repository.getOnsiteJob(hashMap)

                if (response.status == 1 && response.data != null){

                    _onsiteJob.value = UIState.Success(response)

                }else{
                    _onsiteJob.value = UIState.Error(response.message?: "Something went wrong")
                }
            }catch (e : Exception){
                _onsiteJob.value = UIState.Error(e.localizedMessage?: "Error occurred")
            }
        }
    }

    fun resetgetOnsiteJob(){
        _onsiteJob.value = UIState.Idle
    }


    fun checkIfOnsiteService(userId: String) {
        viewModelScope.launch {
            _isOnsiteService.value = null //UIState.Loading

            val hashMap = HashMap<String, String>()
            hashMap["user_id"] = userId
            hashMap["order_status"] = "3"

            try {
                val response = repository.getOrderByOrderStatus(hashMap)
                    val restResponse = response.data
                    if (restResponse != null && response.status == 1 && restResponse.isNotEmpty() && restResponse.size>=1) {
                        _isOnsiteService.value = true //UIState.Success(true)
                    } else {
                        _isOnsiteService.value = false //UIState.Success(false)
                    }
            } catch (e: Exception) {
                _isOnsiteService.value = false  //UIState.Success(false)
//                _isOnsiteState.value = UIState.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }


    fun getMockHomeOptionsResponse(): List<HomeOptionModel> {
        return listOf(
            HomeOptionModel(
                id = "1",
                name = "Travel\nLogs",
                iconUrl = R.drawable.ic_travel  //"https://example.com/icons/bike_service.png"
            ),
            HomeOptionModel(
                id = "2",
                name = "Attendance",
                iconUrl = R.drawable.ic_attendence
            ),
            HomeOptionModel(
                id = "3",
                name = "H-Coin",
                iconUrl = R.drawable.ic_hcoin
            ),
//            HomeOptionModel(
//                id = "4",
//                name = "Reward\n& Points",
//                iconUrl = R.drawable.ic_rewardd
//            ),
            HomeOptionModel(
                id = "5",
                name = "Leader Board",
                iconUrl = R.drawable.ic_leaderboard
            ),
            HomeOptionModel(
                id = "6",
                name = "Refer\n& Earn",
                iconUrl = R.drawable.ic_refer
            )
        )
    }

    fun fetchHomeOptionsMock() {
        viewModelScope.launch {
            _homeoptions_uiState.value = UIState.Loading

            delay(500)

            val mockResponse = getMockHomeOptionsResponse()
            _homeoptions_uiState.value = UIState.Success(mockResponse)
        }
    }



    fun resetOnsiteServiceCheck() {
        _isOnsiteService.value = null
    }


    fun resetUIState() {
        _uiState.value = UIState.Idle
    }

    fun resetDailyPunchState() {
        _dailyPuchLog.value = UIState.Idle
    }


    fun resetHomeOptionsUIState() {
        _homeoptions_uiState.value = UIState.Idle
    }

    fun startTimer(sessions: List<PunchSession>, isPunchedIn: Boolean) {
        timerJob?.cancel()

        val totalWorkedMillis = calculateFixedWorkedMillis(sessions)
        val latestPunchIn = findLastPunchIn(sessions)

        if (isPunchedIn && latestPunchIn != null) {
            val punchInMillis = parseUtcToMillis(latestPunchIn)

            timerJob = viewModelScope.launch(Dispatchers.Main) {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val liveMillis = now - punchInMillis
                    val totalMillis = totalWorkedMillis + liveMillis
                    _timerText.value = formatMillisToHHmmss(totalMillis)
                    delay(1000)
                }
            }
        } else {
            _timerText.value = formatMillisToHHmmss(totalWorkedMillis)
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    private fun calculateFixedWorkedMillis(sessions: List<PunchSession>): Long {
        return sessions.sumOf { session ->
            try {
                if (session.punchOut != null) {
                    val inMillis = parseUtcToMillis(session.punchIn)
                    val outMillis = parseUtcToMillis(session.punchOut)
                    (outMillis - inMillis).coerceAtLeast(0L)
                } else {
                    0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }

    private fun findLastPunchIn(sessions: List<PunchSession>): String? {
        return sessions.lastOrNull { it.punchOut == null }?.punchIn
    }

    fun parseUtcToMillis(dateTimeStr: String): Long {
        return try {
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val utcDateTime = OffsetDateTime.parse(dateTimeStr, formatter)
            val indiaTime = utcDateTime.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
            indiaTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun formatMillisToHHmmss(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun fetchWeather(lat: Double, lng: Double, apiKey: String) {
        viewModelScope.launch {
            _weather.value = UIState.Loading
            try {
                val location = "$lat,$lng"
                val response = repository.getCurrentWeather(location, apiKey)
                _weather.value = UIState.Success(response)
            } catch (e: Exception) {
                _weather.value = UIState.Error(e.localizedMessage ?: "Unknown error")
                Log.e("WeatherViewModel", "Error: ${e.message}")
            }
        }
    }


    fun getUserJobData() {
        val hashMap = HashMap<String,String>()
        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["date"] = CommonMethods.getCurrentDateFormatted()
        viewModelScope.launch {
            _jobDataUiState.value = UIState.Loading
            val result = repository.getUserJobData(hashMap)
            _jobDataUiState.value = when (result) {
                is ApiResult.Success -> UIState.Success(result.data)
                is ApiResult.Error ->  UIState.Error("Error : ${result.message}")
                is ApiResult.UnknownError ->  UIState.Error("Error : ${result.message}")
                ApiResult.NetworkError -> UIState.Error("No internet connection")
            }

        }
    }

    fun reset_jobDataUiState(){
        _jobDataUiState.value = UIState.Idle
    }




}