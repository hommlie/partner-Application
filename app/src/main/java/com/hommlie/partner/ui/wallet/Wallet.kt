package com.hommlie.partner.ui.wallet

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityWalletBinding
import com.hommlie.partner.databinding.BottomsheetAdvanceRequestDetailsBinding
import com.hommlie.partner.databinding.BottomsheetcoinBinding
import com.hommlie.partner.model.AdvanceRequestList
import com.hommlie.partner.model.CoinItem
import com.hommlie.partner.model.RedeemedData
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class Wallet : AppCompatActivity() {
    private lateinit var binding : ActivityWalletBinding

    @Inject
    lateinit var sharePreference: SharePreference
    private lateinit var walletAdapter: WalletAdapter
    private val viewModel: WalletViewModel by viewModels()

    private var hashmap = HashMap<String,String>()
    private var hashmapUserId = HashMap<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // This is Android 15 or above
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }
        } else {
            // This is Android 14 or below
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "H - Wallet", this, R.color.activity_bg, R.color.black)

        hashmap["emp_id"] = sharePreference.getString(PrefKeys.userId)
        hashmap["month"] = (CommonMethods.getCurrentMonthNumber()+1).toString()
        hashmap["year"]  = CommonMethods.getCurrentYear().toString()

        binding.tvMonth.text = CommonMethods.monthNameFromZeroBased(CommonMethods.getCurrentMonthNumber()).take(3)+"-"+CommonMethods.getCurrentYear()+"  "

        binding.tvMonth.setOnClickListener {
            if (CommonMethods.isCheckNetwork(this)) {
                val now = Calendar.getInstance()
                showMonthYearPicker(
                    context = this,
                ) { monthZeroBased, year ->
                    val monthNumber = monthZeroBased + 1
                    val monthName   = CommonMethods.monthNameFromZeroBased(monthZeroBased)

                    binding.tvMonth.text = "${monthName.take(3)}-$year  "

                    hashmap["month"] = monthNumber.toString()
                    hashmap["year"]  = year.toString()

                    viewModel.getRedeemCoinsHistory(hashmap)
                }
            } else {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
            }
        }

        // RecyclerView setup
        walletAdapter = WalletAdapter{ coinRedeemDetails ->
            showCoinDetails(coinRedeemDetails)
        }
        binding.rvWallet.adapter = walletAdapter
        binding.rvWallet.layoutManager = LinearLayoutManager(this@Wallet)

        binding.tvWithdraw.setOnClickListener {
            if (binding.tvAmount.text.toString() == "₹0"){
                CommonMethods.getToast(this@Wallet,"You don't have sufficient amount")
            }else{
                CommonMethods.getToast(this@Wallet,"Withdraw request sent successfully")
            }

        }

        observeCoinHistory()
        viewModel.getRedeemCoinsHistory(hashmap)

        observeCoinData()

        binding.tvWithdraw.setOnClickListener {

            val balance = viewModel.coinBalance.value

            when {
                balance <= 0 -> {
                    CommonMethods.showConfirmationDialog(this@Wallet, "Insufficient Coins", "You don’t have enough coins to withdraw.",false,false){ dialog ->
                        dialog.dismiss()
                    }
                }
                balance % 100 != 0 -> {
                    CommonMethods.showConfirmationDialog(this@Wallet, "Invalid Amount", "Withdrawal coins must be in multiples of 100\neg: 100, 200, 300",false,false){ dialog ->
                        dialog.dismiss()
                    }
                }
                else -> {
                    CommonMethods.showConfirmationDialog(this@Wallet, "Confirm Withdrawal", "Are you sure you want to withdraw $balance coins?",false,true){ dialog ->
                        dialog.dismiss()
                    }
                }
            }
        }

    }

    fun showMonthYearPicker(context: Context, onPicked: (monthZeroBased: Int, year: Int) -> Unit) {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)+1

        val allMonths = arrayOf(
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        )
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 16)
        }
        val monthPicker = NumberPicker(context).apply {
            wrapSelectorWheel = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val yearPicker = NumberPicker(context).apply {
            wrapSelectorWheel = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            minValue = 2000
            maxValue = currentYear
            value = currentYear
        }
        fun updateMonthPicker(selectedYear: Int, keepMonthIndex: Int) {
            val allowedMonths = if (selectedYear == currentYear) {
                // sirf current month ke previous tak
                allMonths.sliceArray(0 until currentMonth)
            } else {
                allMonths
            }

            val oldIndex = keepMonthIndex.coerceAtMost(allowedMonths.size - 1)

            monthPicker.displayedValues = null
            monthPicker.minValue = 0
            monthPicker.maxValue = allowedMonths.size - 1
            monthPicker.displayedValues = allowedMonths
            monthPicker.value = oldIndex
        }
        // initialize with current year & current month -1 (previous)
        val initialMonth = (currentMonth - 1).coerceAtLeast(0)
        updateMonthPicker(currentYear, initialMonth)

        yearPicker.setOnValueChangedListener { _, _, newYear ->
            updateMonthPicker(newYear, monthPicker.value)
        }
        container.addView(monthPicker)
        container.addView(yearPicker)
        AlertDialog.Builder(context)
            .setTitle("Select Month & Year")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                onPicked(monthPicker.value, yearPicker.value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun observeCoinHistory(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.walletList.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@Wallet,lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val list = state.data

                            if (list.isEmpty()) {
                                binding.tvNotransactionfound.visibility = View.VISIBLE
                                walletAdapter.submitList(emptyList())
                            } else {
                                binding.tvNotransactionfound.visibility = View.GONE
                                walletAdapter.submitList(list)
                            }
                            viewModel.reset_RedeemListUiState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            binding.tvNotransactionfound.visibility = View.VISIBLE
                            viewModel.reset_RedeemListUiState()
                        }
                    }
                }
            }
        }
    }
    private fun observeCoinData(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.coinData.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@Wallet,lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val balance = state.data.substringBefore(".").toInt()
                            viewModel.setCoinBalance(balance)
                            binding.tvAmount.text = balance.toString()

                            viewModel.reset_getCoinBalance()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_getCoinBalance()
                        }
                    }
                }
            }
        }
    }

    fun showCoinDetails(advanceRequestList: RedeemedData) {

        val detailsDialogBinding = BottomsheetcoinBinding.inflate(layoutInflater)

        val dialog = Dialog(this@Wallet, R.style.CustomBottomSheetDialogTheme)
        //  dialog.setContentView(successDialogBinding.root)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(detailsDialogBinding.root)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)

        // Your data binding same as before
        detailsDialogBinding.tvRequestid.text = advanceRequestList.trackingId.toString()
        detailsDialogBinding.tvRequestedamount.text = advanceRequestList.coinsRedeemed+" coins"
        detailsDialogBinding.tvReason.text = advanceRequestList.adminNote.toString()
        detailsDialogBinding.tvRequestdate.text = advanceRequestList.createdAt.toString()
        detailsDialogBinding.tvRequeststatus.text = advanceRequestList.statusLabel.toString()

        Glide.with(this@Wallet)
            .load(advanceRequestList.itemImageFullUrl)
            .dontAnimate()
            .dontTransform()
            .into(detailsDialogBinding.ivItemimage)

        var colorRes = -1
        when (advanceRequestList.statusLabel?.lowercase()) {
            "approved" -> { colorRes = R.color.green }
            "pending" -> { colorRes = R.color.orange }
            "rejected" -> { colorRes = R.color.red_logout }
            else -> { colorRes = R.color.medium_gray }
        }
        detailsDialogBinding.tvRequeststatus.setTextColor(ContextCompat.getColor(this@Wallet, colorRes))

        detailsDialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        hashmapUserId["user_id"] = sharePreference.getString(PrefKeys.userId)
        viewModel.getCoinBalance(hashmapUserId)
    }
}