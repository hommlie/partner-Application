package com.hommlie.partner.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.NewOrder
import com.hommlie.partner.repository.HomeRepository
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repository: JobsRepository
) : ViewModel() {

    private val _jobsUiState = MutableStateFlow<UIState<NewOrder>>(UIState.Idle)
    val jobsUIState : StateFlow<UIState<NewOrder>> = _jobsUiState



    fun getNewJobs(map : HashMap<String,String>){
        viewModelScope.launch {
            _jobsUiState.value = UIState.Loading

            try {
                val response = repository.getNewJobs(map)

                if (response.status == 1 && response.data!=null){
                    _jobsUiState.value = UIState.Success(response)
                }else{
                    _jobsUiState.value = UIState.Error(response.message ?: "An error occurred")
                }

            }catch (e : Exception){
                _jobsUiState.value = UIState.Error(e.localizedMessage ?: "An error occurred")
            }
        }

    }
    fun resetGetNewJobs(){
        _jobsUiState.value = UIState.Idle
    }



    private val _hasOrdersUiState = MutableStateFlow<UIState<Boolean>>(UIState.Loading)
    val hasOrdersUiState: StateFlow<UIState<Boolean>> = _hasOrdersUiState

    fun checkOrders(map: HashMap<String, String>) {
        viewModelScope.launch {
            _hasOrdersUiState.value = UIState.Loading

            try {
                val response = repository.getNewJobs(map)

                if (response.status == 1 && !response.data.isNullOrEmpty()) {
                    _hasOrdersUiState.value = UIState.Success(true)
                } else {
                    _hasOrdersUiState.value = UIState.Success(false)
                }
            } catch (e: Exception) {
                _hasOrdersUiState.value = UIState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
    fun resetCheckOrder(){
        _hasOrdersUiState.value = UIState.Idle
    }



}