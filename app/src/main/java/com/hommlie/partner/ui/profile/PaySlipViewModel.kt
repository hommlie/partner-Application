package com.hommlie.partner.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.BreakDown
import com.hommlie.partner.model.PayslipResponse
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

    private val _salaryBreakDown = MutableStateFlow<UIState<PayslipResponse>>(UIState.Idle)
    val salaryBreakDown: StateFlow<UIState<PayslipResponse>> = _salaryBreakDown

    private val _deductionBreakDown = MutableStateFlow<UIState<List<PayslipResponse>>>(UIState.Idle)
    val deductionBreakDown: StateFlow<UIState<List<PayslipResponse>>> = _deductionBreakDown

    init {
//        val hashmap = HashMap<String,String>()
//        hashmap["user_id"] = sharePreference.getString(PrefKeys.userId)
//        getPaySlip(hashmap)
//        getPaySlipFake()
    }


    fun getPaySlip(map: HashMap<String, String>) {
        viewModelScope.launch {
            try {
                // Set loading state
                _salaryBreakDown.value = UIState.Loading
                _deductionBreakDown.value = UIState.Loading

                val response = repository.getPaySlip(map)

                if (response.status == 1 && response.data != null) {
                    // Success: post data
                    _salaryBreakDown.value = UIState.Success(response.data)
//                    _deductionBreakDown.value = UIState.Success(response.data.deduction_breakdown)
                } else {
                    // Error from API
                    _salaryBreakDown.value = UIState.Error(response.message)
                    _deductionBreakDown.value = UIState.Error(response.message)
                }
            } catch (e: Exception) {
                // Network / parsing error
                _salaryBreakDown.value = UIState.Error(e.localizedMessage ?: "Something went wrong")
                _deductionBreakDown.value = UIState.Error(e.localizedMessage ?: "Something went wrong")
            }
        }
    }

    // --- Fake response function ---
    fun getPaySlipFake() {
        viewModelScope.launch {
            _salaryBreakDown.value = UIState.Loading
            _deductionBreakDown.value = UIState.Loading

            delay(1500) // simulate API delay

            try {
                val fakeResponse = BreakDown(
                    salay_breakdown = listOf(
                        SalaryBreakDown("Basic Salary", "45000"),
                        SalaryBreakDown("HRA", "20000"),
                        SalaryBreakDown("Conveyance", "5000"),
                        SalaryBreakDown("Special Allowance", "3000")
                    ),
                    deduction_breakdown = listOf(
                        SalaryBreakDown("PF", "3600"),
                        SalaryBreakDown("Professional Tax", "200"),
                        SalaryBreakDown("Income Tax", "2500")
                    )
                )

//                _salaryBreakDown.value = UIState.Success(fakeResponse.salay_breakdown)
//                _deductionBreakDown.value = UIState.Success(fakeResponse.deduction_breakdown)

            } catch (e: Exception) {
                _salaryBreakDown.value = UIState.Error("Failed to load salary breakdown")
                _deductionBreakDown.value = UIState.Error("Failed to load deductions")
            }
        }
    }

    fun resetDeductionBreakDownUi(){
        _deductionBreakDown.value = UIState.Idle
    }

    fun resetSalaryBreakDownUi(){
        _salaryBreakDown.value = UIState.Idle
    }


}