package com.hommlie.partner.ui.jobs.reschedule

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityRescheduleJobBinding
import com.hommlie.partner.model.TimeSlot
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
import com.hommlie.partner.utils.ProgressDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@AndroidEntryPoint
class RescheduleJob : AppCompatActivity() {
    private lateinit var binding : ActivityRescheduleJobBinding
    private val viewModel : RescheduleJobViewModel by viewModels()

    private lateinit var dateAdapter : RescheduleDateAdapter
    private lateinit var timeAdapter: RescheduleTimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRescheduleJobBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.setVisitID(intent.getStringExtra("visit_id")?:"")

        observeGetTimeSlots()
        observeConfirmBtnState()
        observeConfirmRescheduleService()
        setupListeners()
        setUpTimeAdapter()
        setUpDateAdapter()

        viewModel.getTimeSlotsFromApi()

        binding.btnConfirm.setOnClickListener {
            if (!binding.btnConfirm.isEnabled) return@setOnClickListener
            viewModel.rescheduleService()
        }
    }

    private fun observeConfirmBtnState(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.selectedTimeId.collect { value ->
                    binding.btnConfirm.isEnabled = value != 0
                    binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@RescheduleJob,
                        if (value == 0)
                            R.color.disable_btn
                        else
                            R.color.color_primary
                    ))
                }
            }
        }
    }

    private fun observeGetTimeSlots(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateGetTimeSlots.collect { state ->
                    when(state){
                        is UIState.Idle -> {

                        }
                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@RescheduleJob,lifecycleScope,"Please wait!...","Please wait while we are fetching available slots")
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val data = state.data
                            viewModel.storeTimeSlots(data)


                            // ✅ Initial load (today + next 4 days)
                            val today = LocalDate.now()
                            val dates = viewModel.getDatesFromSelected(today)

                            dateAdapter.updateList(dates)

                            val firstDate = dates.first()
                            viewModel.setSelectedDate(firstDate.date.toString())

                            val timeList = viewModel.getTimeSlot(firstDate.date)
                            timeAdapter.updateList(timeList)
                            viewModel.setSelectedTimeId(0)
//                            timeList.firstOrNull { it.isEnabled }?.let {
//                                viewModel.setSelectedTime(it.label?: "")
//                                viewModel.setSelectedTimeId(it.id ?: 0)
//                            }
                            viewModel.resetUiStateGetTimeSlots()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.showConfirmationDialog(this@RescheduleJob,"Alert!",state.message,false,false,"Ok"){}
                            viewModel.resetUiStateGetTimeSlots()
                        }

                    }
                }
            }
        }
    }

    private fun observeConfirmRescheduleService(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateReschduleService.collect { state ->
                    when(state){
                        is UIState.Idle -> {

                        }
                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@RescheduleJob,lifecycleScope,"Please wait!...","Please wait while we are scheduling service")
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetuiStateReschduleService()
                            CommonMethods.showSuccessDialog(
                                this@RescheduleJob,
                                "Service reschedule \nsuccessfully"
                            ) {
                                finish()
                                finishSlideActivity()
                            }
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.showConfirmationDialog(this@RescheduleJob,"Alert!",state.message,false,false,"Ok"){
                            }
                            viewModel.resetuiStateReschduleService()
                        }

                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
            finishSlideActivity()
        }
        onBackPressedDispatcher.addCallback(this) {
            finish()
            finishSlideActivity()
        }

        binding.mcvDate.setOnClickListener {

            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()) // ❌ past disable
                .build()

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Service Date")
                .setTheme(R.style.MyDatePickerTheme)
                .setCalendarConstraints(constraints)
                .build()

            datePicker.show(supportFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->

                val selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                // ✅ ViewModel update
                viewModel.setSelectedDate(selectedDate.toString())

                // ✅ 🔥 Date Adapter Update (MAIN LOGIC)
                val newDates = viewModel.getDatesFromSelected(selectedDate)
                dateAdapter.updateList(newDates)

                // ✅ Time slots update
                val timeList = viewModel.getTimeSlot(selectedDate)
                timeAdapter.updateList(timeList)
                viewModel.setSelectedTimeId(0)
            }
        }
    }

    private fun setUpDateAdapter() {

        dateAdapter = RescheduleDateAdapter { selectedDate ->

            val formattedDate = selectedDate.date.toString()
            viewModel.setSelectedDate(formattedDate)

            val timeList = viewModel.getTimeSlot(selectedDate.date)
            timeAdapter.updateList(timeList)
            viewModel.setSelectedTimeId(0)
        }

        binding.rvDate.apply {
            layoutManager = LinearLayoutManager(this@RescheduleJob, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }

    }
    private fun setUpTimeAdapter(){
        timeAdapter = RescheduleTimeAdapter { time ->
            viewModel.setSelectedTime(time.label?: "")
            viewModel.setSelectedTimeId(time.id ?: 0)
        }
        binding.rvTime.apply {
            layoutManager = GridLayoutManager(this@RescheduleJob ,3, LinearLayoutManager.VERTICAL,false)
            adapter = timeAdapter
        }
    }
    override fun attachBaseContext(base: Context) {
        val configuration = Configuration(base.resources.configuration)

        // 🔥 Ignore system font scaling
        configuration.fontScale = 1.0f

        // (Optional but recommended) Ignore display size scaling
        configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE

        val context = base.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }


}