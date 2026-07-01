package com.hommlie.partner.ui.jobs.reschedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SelfGeneratedDateModel
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.TimeSlot
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.compareTo

@HiltViewModel
class RescheduleJobViewModel @Inject constructor(private val repository: JobsRepository) : ViewModel() {

    private val _storeTimeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val storeTimeSlots: StateFlow<List<TimeSlot>> = _storeTimeSlots
    fun storeTimeSlots(list: List<TimeSlot>) {
        _storeTimeSlots.value = list
    }

    private val _visitID = MutableStateFlow("")
    val visitID: StateFlow<String> = _visitID
    fun setVisitID(date: String) {
        _visitID.value = date
    }

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    private val _selectedTime = MutableStateFlow("")
    val selectedTime: StateFlow<String> = _selectedTime
    fun setSelectedTime(pattern: String) {
        _selectedTime.value = pattern
    }
    private val _selectedTimeId = MutableStateFlow(0)
    val selectedTimeId: StateFlow<Int> = _selectedTimeId
    fun setSelectedTimeId(pattern: Int) {
        _selectedTimeId.value = pattern
    }


    fun getDatesFromSelected(selectedDate: LocalDate): List<SelfGeneratedDateModel> {

        val list = mutableListOf<SelfGeneratedDateModel>()
        val today = LocalDate.now()

        repeat(5) { i ->
            val date = selectedDate.plusDays(i.toLong())

            val label = when {
                date == today -> "Today"
                date == today.plusDays(1) -> "Tomorrow"
                else -> {
                    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    "${date.dayOfMonth} $month"
                }
            }

            val dayName = date.dayOfWeek.name.take(3)

            list.add(
                SelfGeneratedDateModel(
                    display = label,
                    date = date,
                    day = dayName,
                    isSelected = i == 0
                )
            )
        }

        return list
    }
    fun getTimeSlot(selectedDate: LocalDate): List<TimeSlot> {

        val list = storeTimeSlots.value.map { it.copy() }

        val currentTime = LocalTime.now().plusHours(1)

        if (selectedDate == LocalDate.now()) {

            list.forEach { slot ->

                val startTime = slot.startHour?.let {
                    runCatching { LocalTime.parse(it) }.getOrNull()
                }

                if (startTime != null && !startTime.isAfter(currentTime)) {
                    slot.isEnabled = false
                    slot.isSelected = false
                }
            }
        }
        // 🔥 ensure fresh selection
        list.forEach { it.isSelected = false }
//        list.firstOrNull { it.isEnabled }?.isSelected = true

        return list
    }


    private val _uiStateGetTimeSlots = MutableStateFlow<UIState<List<TimeSlot>>>(UIState.Idle)
    val uiStateGetTimeSlots: StateFlow<UIState<List<TimeSlot>>> = _uiStateGetTimeSlots

    fun resetUiStateGetTimeSlots(){
        _uiStateGetTimeSlots.value = UIState.Idle
    }

    fun getTimeSlotsFromApi() = viewModelScope.launch {
        _uiStateGetTimeSlots.value = UIState.Loading
        delay(1000)

        when (val result = repository.getTimeSlots()) {
            is ApiResult.Success -> {
                if (result.data.status == 1 && result.data.data!=null && result.data.data.isNotEmpty()) {
                    val data = result.data.data
                    _uiStateGetTimeSlots.value = UIState.Success(data)
                }else{
                    _uiStateGetTimeSlots.value = UIState.Error(result.data.message?:"Unknown Error")
                }
            }
            is ApiResult.NetworkError -> _uiStateGetTimeSlots.value = UIState.Error("No internet connection")
            is ApiResult.Error ->{
                _uiStateGetTimeSlots.value = UIState.Error(result.message)
            }
            is ApiResult.UnknownError -> _uiStateGetTimeSlots.value = UIState.Error(result.message)
        }
    }

    private val _uiStateReschduleService = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateReschduleService: StateFlow<UIState<SingleResponse>> = _uiStateReschduleService

    fun resetuiStateReschduleService(){
        _uiStateReschduleService.value = UIState.Idle
    }

    fun rescheduleService() = viewModelScope.launch {
        _uiStateReschduleService.value = UIState.Loading
        delay(1000)

        val hashMap = HashMap<String, String>()
        hashMap["visit_id"] = visitID.value
        hashMap["desired_date"] = selectedDate.value
        hashMap["timeslot_id"] = selectedTimeId.value.toString()

        when (val result = repository.rescheduleService(hashMap)) {
            is ApiResult.Success -> {
                if (result.data.status == 1 ) {
                    val data = result.data
                    _uiStateReschduleService.value = UIState.Success(data)
                }else{
                    _uiStateReschduleService.value = UIState.Error(result.data.message?:"Unknown Error")
                }
            }
            is ApiResult.NetworkError -> _uiStateReschduleService.value = UIState.Error("No internet connection")
            is ApiResult.Error ->{
                _uiStateReschduleService.value = UIState.Error(result.message)
            }
            is ApiResult.UnknownError -> _uiStateReschduleService.value = UIState.Error(result.message)
        }
    }

}