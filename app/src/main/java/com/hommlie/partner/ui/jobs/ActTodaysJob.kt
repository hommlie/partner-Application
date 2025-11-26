package com.hommlie.partner.ui.jobs

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActTodaysJobBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_ddmmmyyyy
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ActTodaysJob : AppCompatActivity() {

    private lateinit var binding: ActivityActTodaysJobBinding

    @Inject
    lateinit var sharePreference: SharePreference

    private val viewModel: JobsViewModel by viewModels()
    private val allJobsList = mutableListOf<NewOrderData>()

    private lateinit var adapter: NewJobsAdapter

    private val hashMapNewJob = HashMap<String, String>()
    private val hashMapPendingJob = HashMap<String, String>()
    private val hashMapCompletedJob = HashMap<String, String>()

    private var userId = ""
    private var title = ""
    private var parentDate = ""

    // Track API completions for “All Jobs”
    private var pendingLoaded = false
    private var completedLoaded = false
    private val tempAllList = mutableListOf<NewOrderData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActTodaysJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        title = intent.getStringExtra("title") ?: "Today's Pending Jobs"
        parentDate = CommonMethods.getCurrentDateFormatted()

        setupToolbar(binding.includeToolbar.root, title, this, R.color.activity_bg, R.color.black)
        setupInsets()
        setupRecyclerView()
        setupSwipeRefresh()

        userId = sharePreference.getString(PrefKeys.userId)
        prepareHashMaps()
        observeNewJobsData()
        setupDatePicker()
    }

    //  System Bar Setup
    private fun setupInsets() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    //  Setup RecyclerView + Adapter
    private fun setupRecyclerView() {
        adapter = NewJobsAdapter(
            onCheckOrders = { clickedJobData ->
                viewModel.checkOrders(hashMapPendingJob)
                observeHasOrder(clickedJobData)
            },
            onClick_raiseHelp = { clickedJobData ->
                val json = Gson().toJson(clickedJobData)
                startActivity(Intent(this, RaiseHelp::class.java).apply {
                    putExtra("job_data", json)
                })
            }
        )

        binding.rvJobs.layoutManager = LinearLayoutManager(this)
        binding.rvJobs.adapter = adapter
    }

    //  Swipe to Refresh
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            when (title) {
                "Today's Completed Jobs" -> viewModel.getNewJobs(hashMapCompletedJob)
                "Today's Pending Jobs" -> viewModel.getNewJobs(hashMapNewJob)
                else -> loadAllJobs()
            }
        }
    }

    //  Prepare API parameters
    private fun prepareHashMaps() {
        hashMapNewJob.apply {
            put("user_id", userId)
            put("order_status", "2") // Pending
            put("date", CommonMethods.getCurrentDateFormatted())
        }
        hashMapPendingJob.apply {
            put("user_id", userId)
            put("order_status", "3")
        }
        hashMapCompletedJob.apply {
            put("user_id", userId)
            put("order_status", "4")
        }
    }

    //  Observe Job Data
    private fun observeNewJobsData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.jobsUIState.collect { state ->
                    when (state) {
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(this@ActTodaysJob, lifecycleScope)
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            handleJobResponse(state.data.data ?: emptyList())
                            binding.swipeRefresh.isRefreshing = false
                            viewModel.resetGetNewJobs()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            handleJobError(state.message)
                            binding.swipeRefresh.isRefreshing = false
                            viewModel.resetGetNewJobs()
                        }

                        is UIState.Idle -> Unit
                    }
                }
            }
        }
    }



    //  Handle Job Success Response
    private fun handleJobResponse(newList: List<NewOrderData>) {
        when (title) {
            "Today's Completed Jobs",
            "Today's Pending Jobs" -> {
                allJobsList.clear()
                allJobsList.addAll(newList)
                updateUI()
            }

            else -> {
                //  All Jobs case (Pending + Completed)
                if (!pendingLoaded) {
                    pendingLoaded = true
                    tempAllList.addAll(newList)
                    Log.d("ActTodaysJob", "Pending data loaded: ${newList.size}")
                } else if (!completedLoaded) {
                    completedLoaded = true
                    tempAllList.addAll(newList)
                    Log.d("ActTodaysJob", "Completed data loaded: ${newList.size}")
                }

                checkIfBothLoaded()
            }
        }
    }

    //  Handle API Error (including No Data case)
    private fun handleJobError(message: String) {
        if (message.equals("User Not Found", true) ||
            message.equals("Employee Not Found", true)
        ) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            CommonMethods.logOut(sharePreference, this)
            return
        }

        Log.w("ActTodaysJob", "Error: $message")

        //  If All Jobs mode, mark whichever failed as loaded
        if (title != "Today's Completed Jobs" && title != "Today's Pending Jobs") {
            if (!pendingLoaded) {
                pendingLoaded = true
                Log.w("ActTodaysJob", "Pending API failed or empty")
            } else if (!completedLoaded) {
                completedLoaded = true
                Log.w("ActTodaysJob", "Completed API failed or empty")
            }
            checkIfBothLoaded()
        } else {
            allJobsList.clear()
            updateUI()
        }
    }

    //  Helper to finalize merging once both APIs have responded
    private fun checkIfBothLoaded() {
        if (pendingLoaded && completedLoaded) {
            allJobsList.clear()
            allJobsList.addAll(tempAllList.distinctBy { it.orderId })
            updateUI()

            // Reset flags for next time
            pendingLoaded = false
            completedLoaded = false
            tempAllList.clear()

            Log.d("ActTodaysJob", "Both APIs processed → Final list size: ${allJobsList.size}")
        }
    }

    //  Update UI
    private fun updateUI() {
        adapter.submitList(ArrayList(allJobsList))
        binding.tvNumber.text = allJobsList.size.toString()

        if (allJobsList.isEmpty()) {
            binding.tvNodata.visibility = View.VISIBLE
            binding.rvJobs.visibility = View.GONE
        } else {
            binding.tvNodata.visibility = View.GONE
            binding.rvJobs.visibility = View.VISIBLE
        }
    }



    //  Observe Has Order
    private fun observeHasOrder(clickedJobData: NewOrderData) {
        lifecycleScope.launch {
            viewModel.hasOrdersUiState.collect { state ->
                when (state) {
                    is UIState.Idle -> Unit
                    is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActTodaysJob, lifecycleScope)
                    is UIState.Success -> {
                        viewModel.resetCheckOrder()
                        ProgressDialogUtil.dismiss()
                        if (state.data) {
                            Toast.makeText(this@ActTodaysJob, "Please finish the current job", Toast.LENGTH_SHORT).show()
                        } else {
                            val json = Gson().toJson(clickedJobData)
                            startActivity(Intent(this@ActTodaysJob, JobDetails::class.java).apply {
                                putExtra("job_data", json)
                            })
                        }
                    }
                    is UIState.Error -> {
                        viewModel.resetCheckOrder()
                        ProgressDialogUtil.dismiss()
                        Toast.makeText(this@ActTodaysJob, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    //  Date picker for Completed Jobs
    private fun setupDatePicker() {
        binding.tvDate.setOnClickListener {
            if (!CommonMethods.isCheckNetwork(this)) {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val d = day.toString().padStart(2, '0')
                    val m = (month + 1).toString().padStart(2, '0')
                    val date = "$year-$m-$d"
                    parentDate = date
                    binding.tvDate.text = date.toFormattedDate_ddmmmyyyy()
                    hashMapCompletedJob["date"] = date
                    viewModel.getNewJobs(hashMapCompletedJob)
                },
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            ).apply {
                datePicker.maxDate = calendar.timeInMillis
                show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (title == "Today's Completed Jobs") {
            binding.clDate.visibility = View.VISIBLE
            binding.tvDate.text = parentDate.toFormattedDate_ddmmmyyyy()

            hashMapCompletedJob["date"] = parentDate
            viewModel.getNewJobs(hashMapCompletedJob)

        } else if (title == "Today's Pending Jobs") {
            binding.clDate.visibility = View.GONE
            viewModel.getNewJobs(hashMapNewJob)

        } else {
            // 🔄 Reset flags for All Jobs
            binding.clDate.visibility = View.GONE
            pendingLoaded = false
            completedLoaded = false
            tempAllList.clear()

            lifecycleScope.launch {
                // 1️⃣ Get Pending Jobs First
                viewModel.getNewJobs(hashMapNewJob)

                // 2️⃣ Then Completed Jobs
                hashMapCompletedJob["date"] = parentDate
                viewModel.getNewJobs(hashMapCompletedJob)
            }
        }
    }


    //  Sequential loading for All Jobs
    private fun loadAllJobs() {
        lifecycleScope.launch {
            pendingLoaded = false
            completedLoaded = false
            tempAllList.clear()

            // Fetch pending → completed sequentially
            viewModel.getNewJobs(hashMapNewJob)
            delay(300) // small delay to ensure ViewModel emits sequentially
            hashMapCompletedJob["date"] = parentDate
            viewModel.getNewJobs(hashMapCompletedJob)
        }
    }
}
