package com.hommlie.partner.model

data class AttendanceResponse(
    val status: Int,
    val message: String,
    val data: AttendanceData
)

data class AttendanceData(
    val month: String,
    val year: Int,
    val summary: AttendanceSummary,
    val attendance: List<AttendanceRecord>
)

data class AttendanceSummary(
    val absent: Int,
    val onLeave: Int,
    val halfDay: Int,
    val lateIn: Int,
    val earlyOut: Int,
    val deficitHr: Int,
    val totalWH: Int,
    val daysWorked: Int,
    val avgWH: Int
)

data class AttendanceRecord(
    val date: String,
    val status: String,
    val workingHours: String,
    val dotColor: String
)

