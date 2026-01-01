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
import com.hommlie.partner.model.RewardItem
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

    fun getFakeRewards(): List<RewardItem> {
        return listOf(

            // ---------- Winter Exclusive (3) ----------
            RewardItem(
                id = "1",
                rewardType = "Winter Exclusive",
                productName = "Free Travel Bag",
                description = "Spend 5000 coins to unlock this reward",
                worthText = "⭐ Travel Bag Worth ₹ 2,999 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/rucksack/w/r/k/-original-imahfskuwb2bq8g3.jpeg?q=70",
                requiredCoin = 5000
            ),
            RewardItem(
                id = "2",
                rewardType = "Winter Exclusive",
                productName = "Woolen Jacket",
                description = "Spend 7000 coins to unlock this reward",
                worthText = "⭐ Jacket Worth ₹ 3,499 ⭐",
                isLocked = true,
                imageRes = "https://m.media-amazon.com/images/I/619xMvtqClL._AC_UL480_FMwebp_QL65_.jpg",
                requiredCoin = 7000
            ),
            RewardItem(
                id = "3",
                rewardType = "Winter Exclusive",
                productName = "Thermal Flask",
                description = "Spend 3000 coins to unlock this reward",
                worthText = "⭐ Flask Worth ₹ 1,499 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/bottle/c/y/n/500-smart-water-thermal-bottle-stainless-steel-mr46-1-led-original-imaghh2gafmb8th9.jpeg?q=70",
                requiredCoin = 3000
            ),

            // ---------- Electronics (2) ----------
            RewardItem(
                id = "4",
                rewardType = "Electronics",
                productName = "Bluetooth Earbuds",
                description = "Spend 8000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 4,999 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/headphone/p/6/p/tws-earbuds-pro-style-bluetooth-2nd-gen-magsafe-charging-case-original-imahhv4eehjh3rty.jpeg?q=70",
                requiredCoin = 8000
            ),
            RewardItem(
                id = "5",
                rewardType = "Electronics",
                productName = "Smart Power Bank",
                description = "Spend 6000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 2,499 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/power-bank/h/y/f/-original-imah439zhgxtxqh7.jpeg?q=70",
                requiredCoin = 6000
            ),

            // ---------- Super Reward (3) ----------
            RewardItem(
                id = "6",
                rewardType = "Super Reward",
                productName = "Smart Watch",
                description = "Spend 12000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 7,999 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/smartwatch/f/v/t/-original-imahghmj5yd4zngd.jpeg?q=70",
                requiredCoin = 12000
            ),
            RewardItem(
                id = "7",
                rewardType = "Super Reward",
                productName = "Wireless Headphones",
                description = "Spend 10000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 6,499 ⭐",
                isLocked = true,
                imageRes = "https://m.media-amazon.com/images/I/41JACWT-wWL._AC_UY327_FMwebp_QL65_.jpg",
                requiredCoin = 10000
            ),
            RewardItem(
                id = "8",
                rewardType = "Super Reward",
                productName = "Table Stand Combo",
                description = "Spend 9000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 3,999 ⭐",
                isLocked = true,
                imageRes = "https://m.media-amazon.com/images/I/71zKx3CNPFL._AC_UL480_FMwebp_QL65_.jpg",
                requiredCoin = 9000
            ),

            // ---------- Special Picks (Added by Me) (2) ----------
            RewardItem(
                id = "9",
                rewardType = "Special Pick",
                productName = "Premium Sunglasses",
                description = "Spend 4000 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 2,199 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/sunglass/g/d/e/m-2660s10147-moonx-original-imahfyqufqk8kdh7.jpeg?q=70",
                requiredCoin = 4000
            ),
            RewardItem(
                id = "10",
                rewardType = "Special Pick",
                productName = "Gym Duffle Bag",
                description = "Spend 5500 coins to unlock this reward",
                worthText = "⭐ Worth ₹ 2,799 ⭐",
                isLocked = true,
                imageRes = "https://rukminim2.flixcart.com/image/612/612/xif0q/duffel-bag/a/a/3/-original-imagrzzzyjppuzyd.jpeg?q=70",
                requiredCoin = 5500
            )
        )
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