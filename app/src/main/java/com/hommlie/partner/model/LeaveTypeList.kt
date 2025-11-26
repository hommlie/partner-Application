package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName

data class LeaveTypeList(

    @SerializedName("status")
    val status : Int,

    @SerializedName("status")
    val message : String,

    @SerializedName("LeaveTypeList_data")
    val leaveTypeList_data : List<LeaveTypeList_Data>?,


)

data class LeaveTypeList_Data(

    @SerializedName("leave_type_no")
    val leave_type_no : Float,

    @SerializedName("leave_type_name")
    val leave_type_name : String,

    @SerializedName("leave_type_name_color")
    val leave_type_name_color : String,

    @SerializedName("leave_type_icon")
    val leave_type_icon : String,

)