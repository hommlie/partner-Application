package com.hommlie.partner.ui.attendence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.AttendanceData
import com.hommlie.partner.model.AttendanceRecord
import com.hommlie.partner.model.AttendanceResponse
import com.hommlie.partner.model.AttendanceSummary
import com.hommlie.partner.model.DailyPunchLogResponse
import com.hommlie.partner.model.LeaveTypeList
import com.hommlie.partner.model.LeaveTypeList_Data
import com.hommlie.partner.repository.AttendanceRepository
import com.hommlie.partner.repository.TravelLogRepository
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddendanceViewModel @Inject constructor(
    private val sharePreference: SharePreference,
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _leaveTypeListState = MutableStateFlow<UIState<LeaveTypeList>>(UIState.Idle)
    val leaveTypeListState : StateFlow<UIState<LeaveTypeList>> = _leaveTypeListState

    private val _attendanceRecordState = MutableStateFlow<UIState<AttendanceResponse>>(UIState.Idle)
    val attendanceRecordState : StateFlow<UIState<AttendanceResponse>> = _attendanceRecordState

    private val _dailyPuchLog = MutableStateFlow<UIState<DailyPunchLogResponse>>(UIState.Idle)
    val dailyPunchLog : StateFlow<UIState<DailyPunchLogResponse>> = _dailyPuchLog

    init {
        fetchMockLeaveType()
    }


    fun getPuchLogByDate(map : HashMap<String,String>){
        viewModelScope.launch {
            _dailyPuchLog.value = UIState.Loading
            try{
                val respone =   repository.dailyPuchLog(map)  //getMockDailyPunchLogResponse()

                if (respone.status == 1 && respone.data !=null){
                    _dailyPuchLog.value = UIState.Success(respone)
                }else{
                    _dailyPuchLog.value = UIState.Error(respone.message ?: "Punch logs empty")
                }

            }catch (e : Exception){
                _dailyPuchLog.value = UIState.Error(e.localizedMessage ?: "Error occurred")
            }
        }
    }


    fun getDailyPuchLog(map : HashMap<String,String>){
        viewModelScope.launch {
            _dailyPuchLog.value = UIState.Loading
            try{
                val respone =   repository.dailyPuchLog(map)  //getMockDailyPunchLogResponse()

                if (respone.status == 1 && respone.data !=null){
                    _dailyPuchLog.value = UIState.Success(respone)
                }else{
                    _dailyPuchLog.value = UIState.Error(respone.message ?: "Punch logs empty")
                }

            }catch (e : Exception){
                _dailyPuchLog.value = UIState.Error(e.localizedMessage ?: "Error occurred")
            }
        }
    }



    private fun fetchMockLeaveType(){
        viewModelScope.launch {
            _leaveTypeListState.value = UIState.Loading

            delay(1000)

            val leaveTypeList = leaveTypeMockResponse()
            _leaveTypeListState.value = UIState.Success(leaveTypeList)
        }
    }

    private fun leaveTypeMockResponse(): LeaveTypeList {
        val mockData = listOf(
            LeaveTypeList_Data(
                leave_type_no = 5.0f,
                leave_type_name = "Apply Adoption Leave",
                leave_type_name_color = "#9C27B0",
                leave_type_icon = "https://example.com/icons/paternity.png"
            ),
            LeaveTypeList_Data(
                leave_type_no = 1.0f,
                leave_type_name = "Casual Leave",
                leave_type_name_color = "#4CAF50",
                leave_type_icon = "https://example.com/icons/casual.png"
            ),
            LeaveTypeList_Data(
                leave_type_no = 2.0f,
                leave_type_name = "Sick Leave",
                leave_type_name_color = "#F44336",
                leave_type_icon = "https://example.com/icons/sick.png"
            ),
            LeaveTypeList_Data(
                leave_type_no = 3.0f,
                leave_type_name = "Earned Leave",
                leave_type_name_color = "#2196F3",
                leave_type_icon = "https://example.com/icons/earned.png"
            ),
            LeaveTypeList_Data(
                leave_type_no = 4.0f,
                leave_type_name = "Maternity Leave",
                leave_type_name_color = "#E91E63",
                leave_type_icon = "https://example.com/icons/maternity.png"
            ),
            LeaveTypeList_Data(
                leave_type_no = 5.0f,
                leave_type_name = "Paternity Leave",
                leave_type_name_color = "#9C27B0",
                leave_type_icon = "https://example.com/icons/paternity.png"
            )
        )

        return LeaveTypeList(
            status = 200,
            message = "Leave types fetched successfully",
            leaveTypeList_data = mockData
        )
    }





    fun fetchAttenceRecordByMonth(hashMap: HashMap<String,String>){
        viewModelScope.launch {
            _attendanceRecordState.value = UIState.Loading
            delay(500)
            try {
                val response = repository.getAttendance(hashMap)
                _attendanceRecordState.value = UIState.Success(response)
            }catch (e : Exception){
                _attendanceRecordState.value = UIState.Error(e.localizedMessage?:"Error occured")
            }

//            delay(1000)
//            val leaveTypeList = attendanceMockResponse()
//            _attendanceRecordState.value = UIState.Success(leaveTypeList)
        }
    }



    private fun attendanceMockResponse(): AttendanceResponse {
        val attendanceList = listOf(
            AttendanceRecord(
                date = "2025-08-01",
                status = "Present",
                workingHours = "08:30",
                dotColor = "#4CAF50"
            ),
            AttendanceRecord(
                date = "2025-08-02",
                status = "Half Day",
                workingHours = "04:00",
                dotColor = "#FF9800"
            ),
            AttendanceRecord(
                date = "2025-08-03",
                status = "Leave",
                workingHours = "00:00",
                dotColor = "#9C27B0"
            ),
            AttendanceRecord(
                date = "2025-08-04",
                status = "Absent",
                workingHours = "00:00",
                dotColor = "#F44336"
            )
        )

        val summary = AttendanceSummary(
            absent = 5,
            onLeave = 2,
            halfDay = 3,
            lateIn = 1,
            earlyOut = 4,
            deficitHr = 0,
            totalWH = 350,
            daysWorked = 5,
            avgWH = 9
        )

        val data = AttendanceData(
            month = "August",
            year = 2025,
            summary = summary,
            attendance = attendanceList
        )

        return AttendanceResponse(
            status = 1,
            message = "Attendance fetched successfully",
            data = data
        )
    }



    fun reset_leaveTypeListState(){
        _leaveTypeListState.value = UIState.Idle
    }

    fun resetDailyPunchState() {
        _dailyPuchLog.value = UIState.Idle
    }

    fun resetAttendanceRecordState() {
        _attendanceRecordState.value = UIState.Idle
    }


}