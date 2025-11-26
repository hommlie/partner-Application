package com.hommlie.partner.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.CheckVersionResponse
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel(){

    private val _checkversionUIState = MutableStateFlow<UIState<CheckVersionResponse>>(UIState.Idle)
    val checkversionUIState : StateFlow<UIState<CheckVersionResponse>> = _checkversionUIState

    fun checkversion() {
        viewModelScope.launch {
            _checkversionUIState.value = UIState.Loading
            val result = authRepository.checkAppVersion()
            _checkversionUIState.value = when (result) {
                is ApiResult.Success -> UIState.Success(result.data)
                is ApiResult.Error ->  UIState.Error("Error : ${result.message}")
                is ApiResult.UnknownError ->  UIState.Error("Error : ${result.message}")
                ApiResult.NetworkError -> UIState.Error("No internet connection")
            }

        }
    }

    fun reset_checkversionUIState(){
        _checkversionUIState.value = UIState.Idle
    }
}