package com.hommlie.partner.ui.leaderboard

import com.hommlie.partner.apiclient.ApiInterface
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.safeApiCall
import com.hommlie.partner.model.AdvanceRequests
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.LeaderBoardData
import com.hommlie.partner.model.Leaderboardd
import com.hommlie.partner.utils.SharePreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderBoardRepository @Inject constructor(private val apiInterface: ApiInterface,private val sharePreference: SharePreference) {

    suspend fun getLeaderBoard(params: HashMap<String, String>) : ApiResult<DynamicSingleResponseWithData<Leaderboardd>> {
        return safeApiCall { apiInterface.getLeaderBoard(params) }
    }
}