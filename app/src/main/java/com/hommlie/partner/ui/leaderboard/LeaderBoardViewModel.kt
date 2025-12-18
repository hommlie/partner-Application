package com.hommlie.partner.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.LeaderBoardData
import com.hommlie.partner.model.Leaderboardd
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderBoardViewModel @Inject constructor(private val sharePreference: SharePreference,private val leaderBoardRepository: LeaderBoardRepository) : ViewModel() {

    private val _getLeaderboardUiState = MutableStateFlow<UIState<Leaderboardd>>(UIState.Idle)
    val getLeaderboardUiState: StateFlow<UIState<Leaderboardd>> = _getLeaderboardUiState

    init {
        val hashMap = HashMap<String,String>()
        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        getLeaderBoard(hashMap)
    }


    fun getLeaderBoard(hashMap: HashMap<String, String>) = viewModelScope.launch {
        _getLeaderboardUiState.value = UIState.Loading

        when (val result = leaderBoardRepository.getLeaderBoard(hashMap)) {
            is ApiResult.Success -> {
                if (result.data.status == 1 && result.data.data!=null) {
                    val data = result.data.data
                    _getLeaderboardUiState.value = UIState.Success(data)
                }else{
                    _getLeaderboardUiState.value = UIState.Error(result.data.message)
                }
            }

            is ApiResult.NetworkError -> _getLeaderboardUiState.value =
                UIState.Error("No internet connection")

            is ApiResult.Error ->{
                _getLeaderboardUiState.value = UIState.Error(result.message)
            }

            is ApiResult.UnknownError -> _getLeaderboardUiState.value = UIState.Error(result.message)
        }
    }
    fun reset_getLeaderboardUiState() {
        _getLeaderboardUiState.value = UIState.Idle
    }


}