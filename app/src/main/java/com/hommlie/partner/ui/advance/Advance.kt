package com.hommlie.partner.ui.advance

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityAdvanceBinding
import com.hommlie.partner.databinding.BottomsheetAdvanceRequestDetailsBinding
import com.hommlie.partner.model.AdvanceRequestList
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class Advance : AppCompatActivity() {

    private lateinit var binding : ActivityAdvanceBinding

    private val viewModel : AdvanceViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference
    private var hashmap = HashMap<String,String>()
    private lateinit var adapter: AdvanceHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdvanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Advance", this, R.color.ub__transparent, R.color.black)

        setupRecyclerView()
        observeAdanceHistory()


        hashmap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashmap["month"] = (CommonMethods.getCurrentMonthNumber()+1).toString()
        hashmap["year"]  = CommonMethods.getCurrentYear().toString()

        binding.tvMonth.text = CommonMethods.monthNameFromZeroBased(CommonMethods.getCurrentMonthNumber()).take(3)+"-"+CommonMethods.getCurrentYear()


        binding.tvMonth.setOnClickListener {
            if (CommonMethods.isCheckNetwork(this)) {
                val now = Calendar.getInstance()
                showMonthYearPicker(
                    context = this,
                ) { monthZeroBased, year ->
                    val monthNumber = monthZeroBased + 1
                    val monthName   = CommonMethods.monthNameFromZeroBased(monthZeroBased)

                    binding.tvMonth.text = "${monthName.take(3)}-$year "

                    hashmap["month"] = monthNumber.toString()
                    hashmap["year"]  = year.toString()

                    viewModel.getAdvanceRequestsHistory(hashmap)
                }
            } else {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
            }
        }

        binding.tvAddAdvance.setOnClickListener {
            val intent = Intent(this@Advance,AddAdvanceRequest::class.java)
            startActivity(intent)
        }
        binding.mcvAddrequest.setOnClickListener {
            val intent = Intent(this@Advance,AddAdvanceRequest::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = AdvanceHistoryAdapter{ advanceRequestList ->
            showRequestDetails(advanceRequestList)
        }
        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@Advance)
            setHasFixedSize(true)
            adapter = this@Advance.adapter
        }
    }



    private fun observeAdanceHistory(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.advanceHistoryState.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@Advance,lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val response = state.data.data

                            val totalAmount = response?.totalAmount
                                ?.replace(",", "")
                                ?.toDoubleOrNull() ?: 0.0

                            val pendingAmount = response?.pendingAmount
                                ?.replace(",", "")
                                ?.toDoubleOrNull() ?: 0.0

                            val approvedAmount = response?.approvedAmount
                                ?.replace(",", "")
                                ?.toDoubleOrNull() ?: 0.0

                            val rejectedAmount = response?.rejectedAmount
                                ?.replace(",", "")
                                ?.toDoubleOrNull() ?: 0.0

                            binding.tvApprovedamount.text = "₹ "+(approvedAmount.toInt())
                            binding.tvRequestamount.text = "₹ "+(totalAmount.toInt())

                            // Calculate approval rate
                            val approvalRate = if (totalAmount > 0) {
                                (approvedAmount / totalAmount) * 100
                            } else {
                                0.0
                            }
                            binding.tvApprovalRate.text = "%.1f%%".format(approvalRate)


                            adapter.submitList(response?.advanceRequests)

                            viewModel.resetAdvanceHistoryState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetAdvanceHistoryState()
                        }
                    }
                }
            }
        }

    }

    fun showMonthYearPicker(
        context: Context,
        onPicked: (monthZeroBased: Int, year: Int) -> Unit
    ) {
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

    override fun onResume() {
        super.onResume()
        viewModel.getAdvanceRequestsHistory(hashmap)
    }

    fun showRequestDetails(advanceRequestList: AdvanceRequestList) {

        val detailsDialogBinding = BottomsheetAdvanceRequestDetailsBinding.inflate(layoutInflater)

        val dialog = Dialog(this@Advance, R.style.CustomBottomSheetDialogTheme)
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
        detailsDialogBinding.tvRequestid.text = advanceRequestList.id.toString()
        detailsDialogBinding.tvRequestedamount.text = advanceRequestList.requestedAmount.toString()
        detailsDialogBinding.tvReason.text = advanceRequestList.reason.toString()
        detailsDialogBinding.tvRequestdate.text = advanceRequestList.requestDate.toString()
        detailsDialogBinding.tvRequeststatus.text = advanceRequestList.status.toString()
        detailsDialogBinding.tvRejectreason.text = advanceRequestList.reject_reason?:"-"
        detailsDialogBinding.tvApprovedamount.text = advanceRequestList.approvedAmount.toString()

        var colorRes = -1

        when (advanceRequestList.status?.lowercase()) {
            "approved" -> {
                colorRes = R.color.green
                detailsDialogBinding.tvApprovedamount.visibility = View.VISIBLE
                detailsDialogBinding.approvedamount.visibility = View.VISIBLE

                detailsDialogBinding.rejectreason.visibility = View.GONE
                detailsDialogBinding.tvRejectreason.visibility = View.GONE
            }

            "pending" -> {
                colorRes = R.color.orange
            }

            "rejected" -> {
                colorRes = R.color.red_logout
                detailsDialogBinding.tvApprovedamount.visibility = View.GONE
                detailsDialogBinding.approvedamount.visibility = View.GONE

                detailsDialogBinding.rejectreason.visibility = View.VISIBLE
                detailsDialogBinding.tvRejectreason.visibility = View.VISIBLE
            }

            else -> {
                colorRes = R.color.medium_gray
            }
        }

        detailsDialogBinding.tvRequeststatus.setTextColor(
            ContextCompat.getColor(this@Advance, colorRes)
        )

        detailsDialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.show()
    }


}