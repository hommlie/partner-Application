package com.hommlie.partner.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SigninSignup
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.utils.FcmTokenProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val fcmTokenProvider: FcmTokenProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<SigninSignup>>(UIState.Idle)
    val uiState: StateFlow<UIState<SigninSignup>> = _uiState

    private val _enteredMobileNo = MutableStateFlow("")
    val enteredMobileNo: StateFlow<String> = _enteredMobileNo

    private val _strToken = MutableStateFlow("")
    val strToken: StateFlow<String> = _strToken

    init {
        fetchTokenAndSendToServer()
    }

    fun onMobileNumberChanged(number: String) {
        _enteredMobileNo.value = number
    }

    fun registerUser(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            try {
                val response = repository.registerEmployee(data)
                _uiState.value = UIState.Success(response)
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }


    fun fetchTokenAndSendToServer() {
        viewModelScope.launch {
            try {
                _strToken.value = fcmTokenProvider.getToken()
                Log.d("FCM_TOKEN", strToken.value)
                // send token via repository
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Failed: ${e.message}")
            }
        }
    }

    fun resetUIState() {
        _uiState.value = UIState.Idle
    }
}



