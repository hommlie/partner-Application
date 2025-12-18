package com.hommlie.partner.ui.addwork

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.model.JobSummary
import com.hommlie.partner.model.SingleResponse
import com.hommlie.partner.repository.HomeRepository
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddWorkViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val sharePreference: SharePreference
) : ViewModel() {

    private val _historyFetched = MutableStateFlow<Boolean>(false)
    val historyFetched : StateFlow<Boolean> = _historyFetched

    fun set_historyFetched(value: Boolean){
        _historyFetched.value = value
    }

    private val _saveBillUiState = MutableStateFlow<UIState<SingleResponse>>(UIState.Idle)
    val saveBillUiState: StateFlow<UIState<SingleResponse>> = _saveBillUiState

    fun saveBill(
        userId: String,
        title: String,
        details: String,
        amount: String,
        expense_date: String,
        imageUris: List<Uri>,
        context: Context
    ) {
        viewModelScope.launch {
            _saveBillUiState.value = UIState.Loading
            val result = homeRepository.saveBill(
                userId,
                title,
                details,
                amount,
                expense_date,
                imageUris,
                context
            )
            _saveBillUiState.value = when (result) {
                is ApiResult.Success -> {
                    if (result.data.status==1){
                        set_historyFetched(false)
                        UIState.Success(result.data)
                    }else{
                        UIState.Error(result.data.message.toString())
                    }
                }
                is ApiResult.Error -> UIState.Error("Error : ${result.message}")
                ApiResult.NetworkError -> UIState.Error("No internet connection")
                is ApiResult.UnknownError -> UIState.Error(result.message)
            }

        }
    }

    fun reset_saveBillUiState() {
        _saveBillUiState.value = UIState.Idle
    }

    private val _expenseHistoryUiState = MutableStateFlow<UIState<List<ExpenseHistory>>>(UIState.Idle)
    val expenseHistoryUiState: StateFlow<UIState<List<ExpenseHistory>>> = _expenseHistoryUiState


    fun fetchExpenseHistory(hashMap: HashMap<String, String>) = viewModelScope.launch {
        _expenseHistoryUiState.value = UIState.Loading

        when (val result = homeRepository.getExpenseHistory(hashMap)) {
            is ApiResult.Success -> {
                if (result.data.status == 1) {
                    val data = result.data.data.orEmpty()
                    _expenseHistoryUiState.value = UIState.Success(data)
                    set_historyFetched(true)
                }else{
                    _expenseHistoryUiState.value = UIState.Error(result.data.message)
                }
            }

            is ApiResult.NetworkError -> _expenseHistoryUiState.value =
                UIState.Error("No internet connection")

            is ApiResult.Error ->{
                _expenseHistoryUiState.value = UIState.Error(result.message)
            }

            is ApiResult.UnknownError -> _expenseHistoryUiState.value = UIState.Error(result.message)
        }
    }
    fun reset_expenseHistoryUiState() {
        _expenseHistoryUiState.value = UIState.Idle
    }

}