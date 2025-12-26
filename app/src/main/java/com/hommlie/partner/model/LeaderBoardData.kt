package com.hommlie.partner.model

import com.google.gson.annotations.SerializedName


data class Leaderboardd(
    @SerializedName("leaderboard")
    val leaderboard : List<LeaderBoardData> ?=null,

    @SerializedName("user_stats")
    val userStats : LeaderBoardData ?=null,
)

data class LeaderBoardData(
    @SerializedName("id")
    val id : Int,

    @SerializedName("emp_name")
    val emp_name : String?=null,

    @SerializedName("total_coins")
    val total_coins : Double?=null,

    @SerializedName("rank")
    val rank : Int?=null,

    @SerializedName("is_current_user")
    val is_current_user : Boolean?=false,

    @SerializedName("profile")
    val profile : String?=null
)

