package com.hommlie.partner.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KYCViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiState : StateFlow<UIState<SingleResponse>> = _uiState


    fun checkStatus(hashMap: HashMap<String,String>){
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            delay(2000)
            try {
                val response = repository.checkProfileVerificationStatus(hashMap)
                _uiState.value = UIState.Success(response)
            }catch (e : Exception){
                _uiState.value = UIState.Error(e.message ?: "Something went wrong")
            }
        }
    }


    fun resetUIState(){
        _uiState.value = UIState.Idle
    }

}