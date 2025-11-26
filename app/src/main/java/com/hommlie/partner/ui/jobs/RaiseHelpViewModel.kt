package com.hommlie.partner.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RaiseHelpViewModel @Inject constructor(private val repository: JobsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<DynamicSingleResponseWithData<Any>>>(UIState.Idle)
    val uiState: StateFlow<UIState<DynamicSingleResponseWithData<Any>>> = _uiState

    fun raiseTicket(hashMap: HashMap<String, String>) = viewModelScope.launch {
        _uiState.value = UIState.Loading

        when (val result = repository.raiseTicket(hashMap)) {
            is ApiResult.Success -> {
                if (result.data.status == 1) {
                    val data = result.data
                    _uiState.value = UIState.Success(data)
                }else{
                    _uiState.value = UIState.Error(result.data.message?:"Unknown Error")
                }
            }
            is ApiResult.NetworkError -> _uiState.value = UIState.Error("No internet connection")
            is ApiResult.Error ->{
                _uiState.value = UIState.Error(result.message)
            }
            is ApiResult.UnknownError -> _uiState.value = UIState.Error(result.message)
        }
    }
    fun reset_uistate(){
        _uiState.value = UIState.Idle
    }

}