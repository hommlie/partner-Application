package com.hommlie.partner.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hommlie.partner.model.WalletItem
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor() : ViewModel() {

    private val _walletList = MutableLiveData<List<WalletItem>>()
    val walletList: LiveData<List<WalletItem>> = _walletList

    init {
        loadWalletTransactions()
    }

    private fun loadWalletTransactions() {
        _walletList.value =  getWalletTransactions()  //repository.getWalletTransactions()
    }

    fun getWalletTransactions(): List<WalletItem> {
        return listOf(
//            WalletItem("Salary", "₹50,000", "25 Dec 2025", true),
//            WalletItem("Electricity Bill", "₹1,200", "26 Dec 2025", false),
//            WalletItem("Bonus", "₹5,000", "01 Jan 2026", true),
//            WalletItem("Grocery Shopping", "₹2,500", "02 Jan 2026", false),
//            WalletItem("Food Reimbursement", "₹800", "05 Jan 2026", true),
//            WalletItem("Cab Ride", "₹300", "07 Jan 2026", false),
//            WalletItem("Project Incentive", "₹10,000", "10 Jan 2026", true),
//            WalletItem("Mobile Recharge", "₹499", "12 Jan 2026", false),
//            WalletItem("Internet Bill", "₹999", "15 Jan 2026", false),
//            WalletItem("Festival Bonus", "₹8,000", "20 Jan 2026", true),
//            WalletItem("Health Checkup", "₹1,500", "22 Jan 2026", false),
//            WalletItem("Travel Allowance", "₹3,000", "25 Jan 2026", true),
//            WalletItem("Movie Tickets", "₹700", "27 Jan 2026", false),
//            WalletItem("Salary", "₹50,500", "25 Feb 2026", true),
//            WalletItem("Shopping (Clothes)", "₹4,200", "28 Feb 2026", false),
//            WalletItem("Dividend Income", "₹2,000", "01 Mar 2026", true),
//            WalletItem("EMI Payment", "₹15,000", "05 Mar 2026", false)
        )
    }



}