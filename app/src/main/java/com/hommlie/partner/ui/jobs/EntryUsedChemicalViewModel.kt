package com.hommlie.partner.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.model.UpdateFilledChemicalRequestBody
import com.hommlie.partner.model.VisitChemicals
import com.hommlie.partner.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryUsedChemicalViewModel  @Inject constructor(private val repository: JobsRepository) : ViewModel() {


    private val _uiStateUpdateFilledVisitChemical = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val uiStateUpdateFilledVisitChemical: StateFlow<UIState<SingleResponse>> = _uiStateUpdateFilledVisitChemical

    fun resetuiStateUpdateFilledVisitChemical(){
        _uiStateUpdateFilledVisitChemical.value = UIState.Idle
    }

    fun updateFilledChemical(reqBody : UpdateFilledChemicalRequestBody) = viewModelScope.launch {
        _uiStateUpdateFilledVisitChemical.value = UIState.Loading
        delay(1000)

        when (val result = repository.updateFilledVisitChemical(reqBody)) {
            is ApiResult.Success -> {
                if (result.data.status == 1) {
                    val data = result.data
                    _uiStateUpdateFilledVisitChemical.value = UIState.Success(data)
                }else{
                    _uiStateUpdateFilledVisitChemical.value = UIState.Error(result.data.message?:"Unknown Error")
                }
            }
            is ApiResult.NetworkError -> _uiStateUpdateFilledVisitChemical.value = UIState.Error("No internet connection")
            is ApiResult.Error ->{
                _uiStateUpdateFilledVisitChemical.value = UIState.Error(result.message)
            }
            is ApiResult.UnknownError -> _uiStateUpdateFilledVisitChemical.value = UIState.Error(result.message)
        }
    }
    
}