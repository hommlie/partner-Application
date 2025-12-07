package com.hommlie.partner.ui.attendence

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActAttendanceBinding
import com.hommlie.partner.model.AttendanceRecord
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.formatToIST
import com.hommlie.partner.utils.CommonMethods.safeParseInstant
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import com.prolificinteractive.materialcalendarview.CalendarDay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

@AndroidEntryPoint
class ActAttendance : AppCompatActivity() {

    private lateinit var binding : ActivityActAttendanceBinding

    @Inject
    lateinit var sharePreference: SharePreference

    private val viewModel : AddendanceViewModel by viewModels()

    private lateinit var adapter : LeaveTypeListAdapter
    private lateinit var leavetypeRecylerView : RecyclerView
//    private lateinit var lineChart: LineChart

//    private lateinit var punchActivity_adapter : PunchActivityAdapter
//    private lateinit var punchActivity_RecylerView : RecyclerView
    private var dateStatusMap: Map<String, String> = emptyMap()


    private var hashMapReqAttendence = HashMap<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Attendance", this, R.color.white, R.color.black)


//        lineChart = binding.lineChart

        leavetypeRecylerView = binding.rvLeavetypes
        adapter = LeaveTypeListAdapter()
        leavetypeRecylerView.layoutManager = LinearLayoutManager(this)
        leavetypeRecylerView.adapter = adapter

       /* punchActivity_RecylerView = binding.rvPunchtimes
        punchActivity_adapter = PunchActivityAdapter()
        punchActivity_RecylerView.layoutManager = LinearLayoutManager(this)
        punchActivity_RecylerView.setHasFixedSize(false)
        punchActivity_RecylerView.isNestedScrollingEnabled = false
        punchActivity_RecylerView.adapter = punchActivity_adapter */



        observLeaveTypeListState()
        observerDailyPuchLogs()
        observerAttendanceRecord()


        hashMapReqAttendence["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMapReqAttendence["month"] = (CommonMethods.getCurrentMonthNumber()+1).toString()
        hashMapReqAttendence["year"]  = CommonMethods.getCurrentYear().toString()
        viewModel.fetchAttenceRecordByMonth(hashMapReqAttendence)



        val hashMap = HashMap<String,String>()
        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["date"] = CommonMethods.getCurrentDateFormatted()
        viewModel.getDailyPuchLog(hashMap)
        binding.tvDate.text = CommonMethods.formatDateToDayAndWeek(hashMap["date"].toString())



        val calendarView = binding.calendarView

        // Highlight today's date by default
        val today = CalendarDay.today()
        calendarView.selectedDate = today

        // Minimum aur maximum date set karo
        binding.calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(2023, 1, 1))
            .setMaximumDate(today) // aaj se aage nahi jaa sakte
            .commit()

//        calendarView.setOnDateChangedListener { widget, date, selected ->
//            val today = CalendarDay.today()
//            if (date.isAfter(today)) {
//                Toast.makeText(this, "Future dates are not allowed", Toast.LENGTH_SHORT).show()
//                calendarView.selectedDate = today
//            } else {
//                val formattedDate = String.format(
//                    "%04d-%02d-%02d",
//                    date.year,
//                    date.month + 1,
//                    date.day
//                )
////                Toast.makeText(this, "Selected: $formattedDate", Toast.LENGTH_SHORT).show()
//                hashMap["date"] = formattedDate
//
//                viewModel.getDailyPuchLog(hashMap)
//
//                binding.tvDate.text = CommonMethods.formatDateToDayAndWeek(formattedDate)
//
//                //  Scroll smoothly to cl_punchtimes
//                binding.ncv.postDelayed({
//                    binding.clPunchtimes.setBackgroundColor(Color.parseColor("#FFFDE7"))
//                    binding.ncv.smoothScrollTo(0, binding.clPunchtimes.top)
//                    binding.clPunchtimes.postDelayed({
//                        binding.clPunchtimes.setBackgroundColor(Color.TRANSPARENT)
//                    }, 800)
//                }, 300)
//
//
//            }
//        }


        calendarView.setOnDateChangedListener { widget, date, selected ->

            val today = CalendarDay.today()
            if (date.isAfter(today)) {
                Toast.makeText(this, "Future dates are not allowed", Toast.LENGTH_SHORT).show()
                calendarView.selectedDate = today
                return@setOnDateChangedListener
            }

            val formattedDate = String.format("%04d-%02d-%02d", date.year, date.month + 1, date.day)

            // Scroll to punch times
            binding.ncv.postDelayed({
                binding.clPunchtimes.setBackgroundColor(Color.parseColor("#FFFDE7"))
                binding.ncv.smoothScrollTo(0, binding.clPunchtimes.top)
                binding.clPunchtimes.postDelayed({
                    binding.clPunchtimes.setBackgroundColor(Color.TRANSPARENT)
                }, 800)
            }, 300)

            // Show selected date in UI
            binding.tvDate.text = CommonMethods.formatDateToDayAndWeek(formattedDate)

            // Get status from map
            val status = dateStatusMap[formattedDate] ?: "Pending"
            binding.tvStatus.text = status
            val ctx = binding.root.context

            when (status.lowercase()) {
                "present" -> {
                    binding.llDate.setBackgroundColor(ContextCompat.getColor(ctx, R.color.light_parrotgreen))
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.color_90C01F))
                    binding.tvStatus.visibility = View.VISIBLE
                }
                "half day" -> {
                    binding.llDate.setBackgroundColor(ContextCompat.getColor(ctx, R.color.light_orange))
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.orange))
                    binding.tvStatus.visibility = View.VISIBLE
                }
                "absent" -> {
                    binding.llDate.setBackgroundColor(ContextCompat.getColor(ctx, R.color.light_red))
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.red))
                    binding.tvStatus.visibility = View.VISIBLE
                }
                "holiday" -> {
                    binding.llDate.setBackgroundColor(ContextCompat.getColor(ctx, R.color.light_parrotgreen))
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.color_90C01F))
                    binding.tvStatus.visibility = View.VISIBLE
                }
                else -> {
                    binding.llDate.setBackgroundColor(ContextCompat.getColor(ctx, R.color.light_orange))
                    binding.tvStatus.visibility = View.GONE
                }

        }


            // Optional: call API if needed
            hashMap["date"] = formattedDate
            viewModel.getDailyPuchLog(hashMap)
        }






        binding.calendarView.setOnMonthChangedListener { widget, date ->
            val currentYear = CalendarDay.today().year
            val currentMonth = CalendarDay.today().month

            // Agar future month ya future year hua
            if (date.year > currentYear ||
                (date.year == currentYear && date.month > currentMonth)) {

                Toast.makeText(this, "Future month not allowed", Toast.LENGTH_SHORT).show()

                // Wapas current month pe le aao
                binding.calendarView.setCurrentDate(CalendarDay.today(), true)
            } else {
                // valid month ke liye API call
                val hashMapReqAttendence = HashMap<String, String>()
                hashMapReqAttendence["user_id"] = sharePreference.getString(PrefKeys.userId)
                hashMapReqAttendence["month"] = (date.month + 1).toString()
                hashMapReqAttendence["year"] = date.year.toString()

                viewModel.fetchAttenceRecordByMonth(hashMapReqAttendence)
            }
        }

//        binding.lineChart.apply {
//            setTouchEnabled(true)
//            setDragEnabled(true)
//            setScaleEnabled(true)
//            setPinchZoom(true)
//            isScaleYEnabled = false
//            setOnTouchListener { v, event ->
//                v.parent.requestDisallowInterceptTouchEvent(true)
//                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
//                    v.parent.requestDisallowInterceptTouchEvent(false)
//                }
//                false
//            }
//        }




    }


    private fun observerDailyPuchLogs(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.dailyPunchLog.collect{ state ->
                    when (state){
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(this@ActAttendance,lifecycleScope)
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            state.data.data?.data?.let { punchList ->
                                if (punchList.isNotEmpty()) {
                                    // Sort by punchIn safely
                                    val sortedList = punchList.sortedBy { safeParseInstant(it.punchIn) }

                                    // Submit list to adapter
//                                    punchActivity_adapter.submitList(sortedList)

                                    // Pick first non-null punchIn
                                    val firstPunchIn = sortedList.firstOrNull { !it.punchIn.isNullOrBlank() }?.punchIn
                                    // Pick last non-null punchOut
                                    val lastPunchOut = sortedList.lastOrNull { !it.punchOut.isNullOrBlank() }?.punchOut

                                    // Display in UI
                                    binding.tvIntime.text = firstPunchIn?.let { formatToIST(it) } ?: "--"
                                    binding.tvOuttime.text = lastPunchOut?.let { formatToIST(it) } ?: "--"

                                    // Calculate total working hours
                                    val totalMillis = sortedList.fold(0L) { acc, punch ->
                                        val inTime = safeParseInstant(punch.punchIn ?: return@fold acc)
                                        val outTime = safeParseInstant(punch.punchOut ?: return@fold acc)
                                        if (inTime != null && outTime != null) {
                                            acc + (outTime.toEpochMilli() - inTime.toEpochMilli())
                                        } else acc
                                    }

                                    val totalHours = totalMillis / (1000 * 60 * 60)
                                    val totalMinutes = (totalMillis / (1000 * 60)) % 60
                                    binding.tvTotaltime.text = String.format("%02d:%02d", totalHours, totalMinutes)+" Hrs"

                                } else {
                                    // Empty list
//                                    punchActivity_adapter.submitList(emptyList())
                                    binding.tvIntime.text = "--"
                                    binding.tvOuttime.text = "--"
                                    binding.tvTotaltime.text = "--"
                                }
                            } ?: run {
                                // Null data
//                                punchActivity_adapter.submitList(emptyList())
                                binding.tvIntime.text = "--"
                                binding.tvOuttime.text = "--"
                                binding.tvTotaltime.text = "--"
                            }



                            viewModel.resetDailyPunchState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@ActAttendance, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@ActAttendance)
                                return@collect
                            }
//                            punchActivity_adapter.submitList(null)
                            binding.tvIntime.text = "--"
                            binding.tvOuttime.text = "--"
                            binding.tvTotaltime.text = "--"
                            viewModel.resetDailyPunchState()
                        }

                        is UIState.Idle -> {

                        }
                    }
                }
            }
        }
    }


//    private fun safeParseInstant(dateString: String?): Instant {
//        return try {
//            if (dateString.isNullOrBlank()) {
//                Instant.EPOCH // default fallback (1970-01-01)
//            } else {
//                Instant.parse(dateString) // server ka format ISO-8601 hai (works fine)
//            }
//        } catch (e: Exception) {
//            Instant.EPOCH // fallback on error
//        }
//    }



    private fun observLeaveTypeListState(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.leaveTypeListState.collect{ state ->
                    when(state){
                        is UIState.Loading ->{
                            ProgressDialogUtil.showLoadingProgress(this@ActAttendance,lifecycleScope)
                        }
                        is UIState.Success ->{
                            state.data.leaveTypeList_data?.let { adapter.submitList(it) }
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_leaveTypeListState()
                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_leaveTypeListState()
                        }
                        is UIState.Idle ->{

                        }
                    }
                }
            }
        }
    }



    fun formatToEpoch(utcTime: String?): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(utcTime)
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }




    private fun observerAttendanceRecord(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.attendanceRecordState.collect{ state ->
                    when (state){
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(this@ActAttendance,lifecycleScope)
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()

                            if (state.data.message.equals("User Not Found", true) ||
                                state.data.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@ActAttendance, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@ActAttendance)
                                return@collect
                            }

                            val attendanceData = state.data?.data
                            val summary = attendanceData?.summary

                            // Update summary views safely
                            binding.tvAbsent.text = summary?.absent?.toString() ?: "-"
                            binding.tvLeave.text = summary?.onLeave?.toString() ?: "-"
                            binding.tvPresent.text = summary?.daysWorked?.toString() ?: "-"

//                            binding.tvDayworked.text = summary?.daysWorked?.toString() ?: "-"
//                            binding.tvLatein.text = summary?.lateIn?.toString() ?: "-"
//                            binding.tvEarlyout.text = summary?.earlyOut?.toString() ?: "-"
//                            binding.tvDeficithr.text = summary?.deficitHr?.toString() ?: "-"
//                            binding.tvTotalwh.text = summary?.totalWH?.toString() ?: "-"
//                            binding.tvAvgwh.text = summary?.avgWH?.toString() ?: "-"

                            // Update calendar dots
                            val attendance = attendanceData?.attendance
                            if (!attendance.isNullOrEmpty()) {
//                                setupLineChart(binding.lineChart,attendance)
                                val dateStatusMapping = attendance.associate { it.date to it.status }

                                setupCalendarDots(attendance)

                                // store dateStatusMap in a variable to use in click listener
                                dateStatusMap = dateStatusMapping
                            } else {
                                clearCalendarDots()  // remove old dots if no data
                            }

                            viewModel.resetAttendanceRecordState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetAttendanceRecordState()
                        }

                        is UIState.Idle -> {

                        }
                    }
                }
            }
        }
    }


    private fun setupCalendarDots(attendanceList: List<AttendanceRecord>) {
//        val calendarView = binding.calendarView
        binding.calendarView.removeDecorators()

        val colorMap = mutableMapOf<Int, MutableList<CalendarDay>>()

        for (record in attendanceList) {
            val (year, month, day) = record.date.split("-").map { it.toInt() }
            val date = CalendarDay.from(year, month - 1, day)

            val color = Color.parseColor(record.dotColor)
            colorMap.getOrPut(color) { mutableListOf() }.add(date)
        }

        for ((color, dates) in colorMap) {
            binding.calendarView.addDecorator(DotDecorator(color, dates))
        }


//        // Add future-date disabler decorator AFTER dot decorators
//        binding.calendarView.addDecorator(FutureDateDisabler())
    }

    fun clearCalendarDots() {
        binding.calendarView.removeDecorators()  // or clear your dot decorators
    }



 /*   @SuppressLint("SetTextI18n")
    private fun setupLineChart(lineChart: LineChart, attendance: List<AttendanceRecord>) {
        if (attendance.isEmpty()) return

        val entries = ArrayList<Entry>()
        var totalHours = 0f

        attendance.forEachIndexed { index, att ->
            val hours = convertTimeToHours(att.workingHours)
            entries.add(Entry(index.toFloat(), hours))
            totalHours += hours
        }

        // ✅ Calculate average working hours
        val avgHours = totalHours / attendance.size

        // ✅ Calculate growth percentage (compare last record with first)
        val first = convertTimeToHours(attendance.first().workingHours)
        val last = convertTimeToHours(attendance.last().workingHours)
        val percentChange = if (first > 0) ((last - first) / first) * 100 else 0f

        // ✅ Update TextViews with real values
        binding.tvAvgworkinghours.text = String.format("%.1f hours", avgHours)
        binding.tvPercent.text = String.format("%+.1f%%", percentChange)

        // ✅ Optional: change color dynamically
        if (percentChange >= 0) {
            binding.tvPercent.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            binding.tvPercent.setTextColor(ContextCompat.getColor(this, R.color.red))
        }

        // ✅ Chart dataset setup
        val dataSet = LineDataSet(entries, "").apply {
            color = ContextCompat.getColor(this@ActAttendance,R.color.color_primary)
            lineWidth = 2f
            setDrawValues(false)
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor( ContextCompat.getColor(this@ActAttendance,R.color.color_primary))
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@ActAttendance, R.drawable.bg_roundwhite5)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val data = LineData(dataSet)
        lineChart.data = data

        // ✅ X-axis styling
        val xAxis = lineChart.xAxis
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.TRANSPARENT
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawLabels(false)

        // ✅ Y-axis styling (hour lines + labels)
        val leftAxis = lineChart.axisLeft
        leftAxis.isEnabled = true
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawLabels(true)
        leftAxis.textColor = Color.parseColor("#808080")
        leftAxis.textSize = 12f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 24f
        leftAxis.granularity = 2f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#E0E0E0")
        leftAxis.gridLineWidth = 1f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()} H"
        }

        lineChart.axisRight.isEnabled = false

        // ✅ General styling
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setBackgroundColor(Color.TRANSPARENT)
        lineChart.setViewPortOffsets(90f, 20f, 20f, 20f)

        lineChart.animateX(800, Easing.EaseInOutQuad)
        lineChart.invalidate()
    } */

    private fun convertTimeToHours(time: String): Float {
        val parts = time.split(":")
        val hours = parts.getOrNull(0)?.toFloatOrNull() ?: 0f
        val minutes = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        return hours + minutes / 60f
    }




}