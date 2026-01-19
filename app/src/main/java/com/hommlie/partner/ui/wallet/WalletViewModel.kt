package com.hommlie.partner.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.ApiResult
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.DynamicSingleResponseWithData
import com.hommlie.partner.model.RedeemedData
import com.hommlie.partner.model.RewardItem
import com.hommlie.partner.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(private val repository: ExpenseRepository) : ViewModel() {

    private val _walletList = MutableStateFlow<UIState<List<RedeemedData>>>(UIState.Idle)
    val walletList: StateFlow<UIState<List<RedeemedData>>> = _walletList

    private val _coinData = MutableStateFlow<UIState<String>>(UIState.Idle)
    val coinData: StateFlow<UIState<String>> = _coinData

    private val _getRewardItems = MutableStateFlow<UIState<List<RewardItem>>>(UIState.Idle)
    val getRewardItems: StateFlow<UIState<List<RewardItem>>> = _getRewardItems

    private val _clickRedeem = MutableStateFlow<UIState<DynamicSingleResponseWithData<Any>>>(UIState.Idle)
    val clickRedeem: StateFlow<UIState<DynamicSingleResponseWithData<Any>>> = _clickRedeem

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance

    fun setCoinBalance(balance: Int) {
        _coinBalance.value = balance
    }

    fun getRedeemCoinsHistory(params: HashMap<String, String>) {
        viewModelScope.launch {
            _walletList.value = UIState.Loading
            when (val result = repository.getRedeemCoinsHistory(params)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1) {
                        if (response.data?.redeemedData!=null){
                            _walletList.value = UIState.Success(response.data.redeemedData)
                        }else {
                            _walletList.value = UIState.Error("No Data Found")
                        }
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

    fun clickRedeem(params: HashMap<String, String>) {
        viewModelScope.launch {
            _clickRedeem.value = UIState.Loading
            when (val result = repository.clickRedeem(params)) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1) {
                        _clickRedeem.value = UIState.Success(response)
                    } else {
                        _clickRedeem.value = UIState.Error(response.message ?: "Something went wrong")
                    }
                }
                is ApiResult.Error -> {
                    _clickRedeem.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _clickRedeem.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _clickRedeem.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun reset_clickRedeem(){
        _clickRedeem.value = UIState.Idle
    }

    fun getRewardItems() {
        viewModelScope.launch {
            _getRewardItems.value = UIState.Loading
            when (val result = repository.getRewardItems()) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.status == 1) {
                        _getRewardItems.value = UIState.Success(response.data.orEmpty())
                    } else {
                        _getRewardItems.value = UIState.Error(response.message ?: "Something went wrong")
                    }
                }
                is ApiResult.Error -> {
                    _getRewardItems.value = UIState.Error("Error ${result.code}: ${result.message}")
                }
                is ApiResult.NetworkError -> {
                    _getRewardItems.value = UIState.Error("Network error. Please check your connection.")
                }
                is ApiResult.UnknownError -> {
                    _getRewardItems.value = UIState.Error(result.message)
                }
            }
        }
    }
    fun reset_getRewardItem(){
        _getRewardItems.value = UIState.Idle
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