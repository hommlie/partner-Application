package com.hommlie.partner.repository

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.model.AttendanceResponse
import com.hommlie.partner.model.BreakDown
import com.hommlie.partner.model.DailyPunchLogResponse
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.LeaveTypeList
import com.hommlie.partner.model.PayslipResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor( private val apiService : ApiInterface) {

    suspend fun getLeaveTypeList(data : HashMap<String,String>) : LeaveTypeList{
        return apiService.getLeaveTypeList(data)
    }

    suspend fun dailyPuchLog( map: HashMap<String, String>): DailyPunchLogResponse {
        return apiService.dailyPucchLog(map)
    }

    suspend fun getAttendance(map: HashMap<String, String>): AttendanceResponse{
        return apiService.getAttendance(map)
    }

    suspend fun getPaySlip(map: HashMap<String, String>): DynamicSingleResponseWithData<PayslipResponse> {
        return apiService.getPaySlip(map)
    }
}