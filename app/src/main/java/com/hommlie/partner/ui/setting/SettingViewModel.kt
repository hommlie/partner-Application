package com.hommlie.partner.ui.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val sharePreference: SharePreference,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _deleteAccountState = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val deleteAccountState: StateFlow<UIState<SingleResponse>> = _deleteAccountState

    data class UserInfo(
        val name: String,
        val email: String,
        val mobile: String?,
        val isGuest: Boolean
    )

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    fun loadUserInfo() {
        val isLoggedIn = sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)
        _userInfo.value = if (isLoggedIn) {
            UserInfo(
                name = sharePreference.getString(PrefKeys.userName) ?: "Unknown",
                email = sharePreference.getString(PrefKeys.userEmail) ?: "",
                mobile = sharePreference.getString(PrefKeys.userMobile).replace("+91", "+91 "),
                isGuest = false
            )
        } else {
            UserInfo("Guest", "guest@gmail.com", null, true)
        }
    }



    fun logout(context : Context) {
        CommonMethods.logOut(sharePreference, context)
    }



    fun deleteAccount(params: HashMap<String, String>) {
        viewModelScope.launch {
            _deleteAccountState.value = UIState.Loading
            try {
                val response = authRepository.deleteAccount(params)
                _deleteAccountState.value = UIState.Success(response)
            } catch (e: Exception) {
                _deleteAccountState.value = UIState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun resetUIDeleteAccount(){
        _deleteAccountState.value = UIState.Idle
    }

}
