package com.hommlie.partner.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.SalaryBreakDown
import com.hommlie.partner.repository.AttendanceRepository
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaySlipViewModel @Inject constructor(private val repository: AttendanceRepository, private val sharePreference: SharePreference) :ViewModel() {

    private val _salaryBreakDown = MutableStateFlow<UIState<SalaryBreakDown>>(UIState.Idle)
    val salaryBreakDown: StateFlow<UIState<SalaryBreakDown>> = _salaryBreakDown


    fun getPaySlip(map: HashMap<String, String>) {
        viewModelScope.launch {
            try {
                // Set loading state
                _salaryBreakDown.value = UIState.Loading

                val response = repository.getPaySlip(map)

                if (response.status == 1 && response.data != null) {
                    // Success: post data
                    _salaryBreakDown.value = UIState.Success(response.data)
//                    _deductionBreakDown.value = UIState.Success(response.data.deduction_breakdown)
                } else {
                    // Error from API
                    _salaryBreakDown.value = UIState.Error(response.message)
                }
            } catch (e: Exception) {
                // Network / parsing error
                _salaryBreakDown.value = UIState.Error(e.localizedMessage ?: "Something went wrong")
            }
        }
    }

    fun resetSalaryBreakDownUi(){
        _salaryBreakDown.value = UIState.Idle
    }


}