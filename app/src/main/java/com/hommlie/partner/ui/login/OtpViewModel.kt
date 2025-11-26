package com.hommlie.partner.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SigninSignup
import com.hommlie.partner.model.VerifyOtp
import com.hommlie.partner.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _timeLeft = MutableStateFlow("00:45")
    val timeLeft: StateFlow<String> = _timeLeft

    private val _canResend = MutableStateFlow(false)
    val canResend: StateFlow<Boolean> = _canResend

    private val _enteredOtp = MutableStateFlow("")
    val enteredOtp: StateFlow<String> = _enteredOtp

    private val _enteredmobileNo = MutableStateFlow("")
    val enteredMobileNo: StateFlow<String> = _enteredmobileNo

    private val _uiState = MutableStateFlow<UIState<VerifyOtp>>(UIState.Idle)
    val uiState: StateFlow<UIState<VerifyOtp>> = _uiState

    private var timerJob: Job? = null

    fun startOtpTimer(totalMillis: Long = 25000L) {
        timerJob?.cancel()
        _canResend.value = false

        timerJob = viewModelScope.launch {
            val interval = 1000L
            var remaining = totalMillis

            while (remaining >= 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                _timeLeft.value = String.format("%02d:%02d", minutes, seconds)

                delay(interval)
                remaining -= interval
            }

            _canResend.value = true
            _timeLeft.value = "Resend"
        }
    }

    fun resendOtp(phone: String) {
        viewModelScope.launch {
            try {
//                repository.sendOtp(phone) // suspend function
                _canResend.value = false
                startOtpTimer()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateOtp(otp: String) {
        _enteredOtp.value = otp
    }

    fun setMobileNumber(number: String) {
        _enteredmobileNo.value = number
    }

    fun verifyOtp(data: HashMap<String, String>) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            try {
                val response = repository.verifyEmpl(data)
                _uiState.value = UIState.Success(response)
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }


    fun resetTimer() {
        timerJob?.cancel()
        _canResend.value = false
        _timeLeft.value = "00:45"
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun resetUIState() {
        _uiState.value = UIState.Idle
    }
}
