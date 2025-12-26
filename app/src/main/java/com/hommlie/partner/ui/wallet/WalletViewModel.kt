package com.hommlie.partner.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(private val repository: ExpenseRepository) : ViewModel() {

    private val _walletList = MutableStateFlow<UIState<List<CoinItem>>>(UIState.Idle)
    val walletList: StateFlow<UIState<List<CoinItem>>> = _walletList

    private val _coinData = MutableStateFlow<UIState<String>>(UIState.Idle)
    val coinData: StateFlow<UIState<String>> = _coinData

    private val _coinBalance = MutableStateFlow("0")
    val coinBalance: StateFlow<String> = _coinBalance
    fun setCoinBalance(balance: String) {
        _coinBalance.value = balance
    }

    fun getRedeemCoinsHistory(params: HashMap<String, String>) {
        viewModelScope.launch {
            _walletList.value = UIState.Loading
            when (val result = repository.getRedeemCoinsHistory(params)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1) {
                        _walletList.value = UIState.Success(response.data.orEmpty())
                    } else {
                        _walletList.value = UIState.Error(response.message ?: "Something went wrong"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _walletList.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _walletList.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _walletList.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun reset_RedeemListUiState(){
        _walletList.value = UIState.Idle
    }

    fun getCoinBalance(params: HashMap<String, String>) {
        viewModelScope.launch {
            _coinData.value = UIState.Loading
            when (val result = repository.getCoinBalance(params)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1) {
                        _coinData.value = UIState.Success(response.data.toString())
                    } else {
                        _coinData.value = UIState.Error(response.message ?: "Something went wrong")
                    }
                }
                is ApiResult.Error -> {
                    _coinData.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _coinData.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _coinData.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun reset_getCoinBalance(){
        _coinData.value = UIState.Idle
    }

  /*  private fun loadWalletTransactions() {
        _walletList.value =  getWalletTransactions()  //repository.getWalletTransactions()
    }

    fun getWalletTransactions(): List<CoinItem> {
        return listOf(
            WalletItem("Salary", "₹50,000", "25 Dec 2025", true),
            WalletItem("Electricity Bill", "₹1,200", "26 Dec 2025", false),
            WalletItem("Bonus", "₹5,000", "01 Jan 2026", true),
            WalletItem("Grocery Shopping", "₹2,500", "02 Jan 2026", false),
            WalletItem("Food Reimbursement", "₹800", "05 Jan 2026", true),
            WalletItem("Cab Ride", "₹300", "07 Jan 2026", false),
            WalletItem("Project Incentive", "₹10,000", "10 Jan 2026", true),
            WalletItem("Mobile Recharge", "₹499", "12 Jan 2026", false),
            WalletItem("Internet Bill", "₹999", "15 Jan 2026", false),
            WalletItem("Festival Bonus", "₹8,000", "20 Jan 2026", true),
            WalletItem("Health Checkup", "₹1,500", "22 Jan 2026", false),
            WalletItem("Travel Allowance", "₹3,000", "25 Jan 2026", true),
            WalletItem("Movie Tickets", "₹700", "27 Jan 2026", false),
            WalletItem("Salary", "₹50,500", "25 Feb 2026", true),
            WalletItem("Shopping (Clothes)", "₹4,200", "28 Feb 2026", false),
            WalletItem("Dividend Income", "₹2,000", "01 Mar 2026", true),
            WalletItem("EMI Payment", "₹15,000", "05 Mar 2026", false)
        )
    } */

}