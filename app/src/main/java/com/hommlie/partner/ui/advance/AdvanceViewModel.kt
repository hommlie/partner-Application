package com.hommlie.partner.ui.advance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.AdvanceRequests
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvanceViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _advanceHistoryState = MutableStateFlow<UIState<DynamicSingleResponseWithData<AdvanceRequests>>>(UIState.Idle)
    val advanceHistoryState: StateFlow<UIState<DynamicSingleResponseWithData<AdvanceRequests>>> = _advanceHistoryState

    private val _addAdvanceState = MutableStateFlow<UIState<DynamicSingleResponseWithData<Any>>>(UIState.Idle)
    val addAdvanceState: StateFlow<UIState<DynamicSingleResponseWithData<Any>>> = _addAdvanceState


    fun getAdvanceRequestsHistory(params: HashMap<String, String>) {
        viewModelScope.launch {
            _advanceHistoryState.value = UIState.Loading
            when (val result = repository.getAdvanceRequestsHistory(params)) {
                is ApiResult.Success -> {
                    _advanceHistoryState.value = UIState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _advanceHistoryState.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _advanceHistoryState.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _advanceHistoryState.value = UIState.Error(result.message)
                }
            }
        }
    }

    fun addAdvanceRequest(params: HashMap<String, String>) {
        viewModelScope.launch {
            _addAdvanceState.value = UIState.Loading
            when (val result = repository.addAdvanceRequest(params)) {
                is ApiResult.Success -> {
                    _addAdvanceState.value = UIState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _addAdvanceState.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _addAdvanceState.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _addAdvanceState.value = UIState.Error(result.message)
                }
            }
        }
    }

    fun resetAdvanceHistoryState() {
        _advanceHistoryState.value = UIState.Idle
    }

    fun resetAddAdvanceState() {
        _addAdvanceState.value = UIState.Idle
    }

}
