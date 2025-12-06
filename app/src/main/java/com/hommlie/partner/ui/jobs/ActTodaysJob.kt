package com.hommlie.partner

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActTodaysJobBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.ui.jobs.JobDetails
import com.hommlie.partner.ui.jobs.JobsViewModel
import com.hommlie.partner.ui.jobs.NewJobsAdapter
import com.hommlie.partner.ui.jobs.RaiseHelp
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_ddmmmyyyy
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import kotlinx.coroutines.flow.collectLatest

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

    // Flags & temp storage used for merging multi-call results
    private var newJobLoaded = false
    private var pendingLoaded = false
    private var completedLoaded = false
    private val tempAllList = mutableListOf<NewOrderData>()

    private var currentType = JobType.PENDING

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

        setupToolbar(binding.includeToolbar.root, title, this, R.color.white, R.color.black)
        setupInsets()
        setupRecyclerView()
        setupSwipeRefresh()

        userId = sharePreference.getString(PrefKeys.userId)
        prepareHashMaps()
        observeNewJobsData()
        setupDatePicker()
    }

    // --- UI setup helpers ---
    private fun setupInsets() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NewJobsAdapter(
            onCheckOrders = { clickedJobData ->
                if (clickedJobData.orderStatus == "3") {
                    startJobDetails(clickedJobData)
                } else {
                    // check if technician has other orders
                    viewModel.checkOrders(hashMapPendingJob)
                    observeHasOrder(clickedJobData) // this will navigate on success=false
                }
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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            when (title) {
                "Today's Completed Jobs" -> loadJobs(JobType.COMPLETED)
                "Today's Pending Jobs" -> loadJobs(JobType.PENDING)
                else -> loadJobs(JobType.ALL)
            }
        }
    }

    private fun prepareHashMaps() {
        hashMapNewJob.apply {
            put("user_id", userId)
            put("order_status", "2") // New/pending
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

    // --- Observers ---
    private fun observeNewJobsData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.jobsUIState.collectLatest { state ->
                    when (state) {
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActTodaysJob, lifecycleScope)

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

    // --- Job response handling ---
    private fun handleJobResponse(newList: List<NewOrderData>) {
        when (currentType) {
            JobType.COMPLETED -> {
                allJobsList.apply {
                    clear()
                    addAll(newList)
                }
                updateUI()
            }

            JobType.PENDING -> handlePendingSequence(newList)

            JobType.ALL -> handleAllSequence(newList)
        }
    }

    private fun handlePendingSequence(newList: List<NewOrderData>) {
        // Sequence: pending (order_status=3) first then new (order_status=2)
        if (!pendingLoaded) {
            pendingLoaded = true
            tempAllList.addAll(newList)
            // not final yet — wait for new jobs
            return
        }

        if (!newJobLoaded) {
            newJobLoaded = true
            tempAllList.addAll(newList)
        }

        // both arrived → finalize
        if (pendingLoaded && newJobLoaded) {
            allJobsList.apply {
                clear()
                addAll(tempAllList.distinctBy { it.orderId })
            }
            updateUI()
            // reset for next time
            resetFlags(JobType.PENDING)
        }
    }

    private fun handleAllSequence(newList: List<NewOrderData>) {
        // Sequence: pending -> new -> completed
        when {
            !pendingLoaded -> {
                pendingLoaded = true
                tempAllList.addAll(newList)
                return
            }

            !newJobLoaded -> {
                newJobLoaded = true
                tempAllList.addAll(newList)
                return
            }

            !completedLoaded -> {
                completedLoaded = true
                tempAllList.addAll(newList)
            }
        }

        if (pendingLoaded && newJobLoaded && completedLoaded) {
            allJobsList.apply {
                clear()
                addAll(tempAllList.distinctBy { it.orderId })
            }
            updateUI()
            resetFlags(JobType.ALL)
        }
    }

    private fun handleJobError(message: String) {
        if (message.equals("User Not Found", true) || message.equals("Employee Not Found", true)) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            CommonMethods.logOut(sharePreference, this)
            return
        }

        Log.w("ActTodaysJob", "Error: $message")

        when (currentType) {
            JobType.COMPLETED -> {
                allJobsList.clear()
                updateUI()
            }

            JobType.PENDING -> {
                if (!pendingLoaded) pendingLoaded = true
                else if (!newJobLoaded) newJobLoaded = true
                // finalize if both marked
                if (pendingLoaded && newJobLoaded) {
                    allJobsList.apply {
                        clear()
                        addAll(tempAllList.distinctBy { it.orderId })
                    }
                    updateUI()
                    resetFlags(JobType.PENDING)
                }
            }

            JobType.ALL -> {
                if (!pendingLoaded) pendingLoaded = true
                else if (!newJobLoaded) newJobLoaded = true
                else if (!completedLoaded) completedLoaded = true

                if (pendingLoaded && newJobLoaded && completedLoaded) {
                    allJobsList.apply {
                        clear()
                        addAll(tempAllList.distinctBy { it.orderId })
                    }
                    updateUI()
                    resetFlags(JobType.ALL)
                }
            }
        }
    }

    // --- Utilities ---
    private fun resetFlags(type: JobType) {
        newJobLoaded = false
        pendingLoaded = false
        tempAllList.clear()
        if (type == JobType.ALL) completedLoaded = false
    }

    private fun updateUI() {
        adapter.submitList(allJobsList.toList())
        binding.tvNumber.text = allJobsList.size.toString()

        val empty = allJobsList.isEmpty()
        binding.tvNodata.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvJobs.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun startJobDetails(item: NewOrderData) {
        val json = Gson().toJson(item)
        startActivity(Intent(this@ActTodaysJob, JobDetails::class.java).apply {
            putExtra("job_data", json)
        })
    }

    // Called when adapter item triggers checkOrders; kept separate so navigation happens from here
    private fun observeHasOrder(clickedJobData: NewOrderData) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasOrdersUiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActTodaysJob, lifecycleScope)
                        is UIState.Success -> {
                            viewModel.resetCheckOrder()
                            ProgressDialogUtil.dismiss()
                            if (state.data) {
                                Toast.makeText(this@ActTodaysJob, "Please finish the current job", Toast.LENGTH_SHORT).show()
                            } else {
                                startJobDetails(clickedJobData)
                            }
                        }
                        is UIState.Error -> {
                            viewModel.resetCheckOrder()
                            ProgressDialogUtil.dismiss()
                            Toast.makeText(this@ActTodaysJob, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is UIState.Idle -> Unit
                    }
                }
            }
        }
    }

    // Date-picker (Completed jobs)
    private fun setupDatePicker() {
        binding.tvDate.setOnClickListener {
            if (!CommonMethods.isCheckNetwork(this)) {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
                return@setOnClickListener
            }

            val calendar = java.util.Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val d = day.toString().padStart(2, '0')
                    val m = (month + 1).toString().padStart(2, '0')
                    val date = "$year-$m-$d"
                    parentDate = date
                    binding.tvDate.text = date.toFormattedDate_ddmmmyyyy()
                    hashMapCompletedJob["date"] = date
                    loadJobs(JobType.COMPLETED)
                },
                calendar[java.util.Calendar.YEAR],
                calendar[java.util.Calendar.MONTH],
                calendar[java.util.Calendar.DAY_OF_MONTH]
            ).apply {
                datePicker.maxDate = calendar.timeInMillis
                show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        when (title) {
            "Today's Completed Jobs" -> {
                binding.clDate.visibility = View.VISIBLE
                binding.tvDate.text = parentDate.toFormattedDate_ddmmmyyyy()
                hashMapCompletedJob["date"] = parentDate
                loadJobs(JobType.COMPLETED)
            }

            "Today's Pending Jobs" -> {
                binding.clDate.visibility = View.GONE
                loadJobs(JobType.PENDING)
            }

            else -> {
                binding.clDate.visibility = View.GONE
                loadJobs(JobType.ALL)
            }
        }
    }

    // Main loader: we intentionally call the sequence with small delays so ViewModel emissions are easier to merge.
    private fun loadJobs(type: JobType) {
        currentType = type
        resetFlags(type)

        lifecycleScope.launch {
            when (type) {
                JobType.COMPLETED -> {
                    hashMapCompletedJob["date"] = parentDate
                    viewModel.getNewJobs(hashMapCompletedJob)
                }

                JobType.PENDING -> {
                    // pending(3) then new(2)
                    viewModel.getNewJobs(hashMapPendingJob)
                    delay(250)
                    viewModel.getNewJobs(hashMapNewJob)
                }

                JobType.ALL -> {
                    // pending(3) -> new(2) -> completed(4)
                    viewModel.getNewJobs(hashMapPendingJob)
                    delay(250)
                    viewModel.getNewJobs(hashMapNewJob)
                    delay(250)
                    hashMapCompletedJob["date"] = parentDate
                    viewModel.getNewJobs(hashMapCompletedJob)
                }
            }
        }
    }

    enum class JobType { COMPLETED, PENDING, ALL }
}
