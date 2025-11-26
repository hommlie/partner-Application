package com.hommlie.partner.ui.addwork

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.hommlie.partner.R
import com.hommlie.partner.adapter.SelectedImageAdapter
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActAddWorkBinding
import com.hommlie.partner.model.ExpenseHistory
import com.hommlie.partner.ui.jobs.JobsViewModel
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_ddmmmyyyy
import com.hommlie.partner.utils.KeyboardUtils
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ActAddWork : AppCompatActivity() {

    private lateinit var binding : ActivityActAddWorkBinding

    @Inject
    lateinit var sharePreference: SharePreference

    private val viewModel : AddWorkViewModel by viewModels()
    private lateinit var adapter: ExpenseHistoryAdapter
    private var allExpenseList: List<ExpenseHistory> = emptyList()
    private var pendingExpenseList: List<ExpenseHistory> = emptyList()
    private var apprvedExpenceList: List<ExpenseHistory> = emptyList()


    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: SelectedImageAdapter
    private var cameraImageUri: Uri? = null

    private var hashmap = HashMap<String,String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActAddWorkBinding.inflate(layoutInflater)
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
        setupToolbar(toolbarView, "Expense", this, R.color.ub__transparent, R.color.black)


        observeUIStateSaveBill()
        setupRecyclerView()
        observeExpenseHistory()

        hashmap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashmap["month"] = (CommonMethods.getCurrentMonthNumber()+1).toString()
        hashmap["year"]  = CommonMethods.getCurrentYear().toString()


        binding.tvAdd.setOnClickListener {
            binding.tvAdd.setTextColor(ContextCompat.getColor(this, R.color.color_primary))
            binding.tvHistory.setTextColor(ContextCompat.getColor(this, R.color.medium_gray))
            binding.viewAdd.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_primary)
            binding.viewHistory.backgroundTintList = ContextCompat.getColorStateList(this, R.color.activity_bg)

            binding.tvAdd.typeface = ResourcesCompat.getFont(this, R.font.inter_medium)
            binding.tvHistory.typeface = ResourcesCompat.getFont(this, R.font.inter_regular)

            binding.clAdd.visibility = View.VISIBLE
            binding.clHistory.visibility = View.GONE
            binding.ivCalender.visibility = View.GONE
        }

        binding.tvHistory.setOnClickListener {
            binding.tvHistory.setTextColor(ContextCompat.getColor(this, R.color.color_primary))
            binding.tvAdd.setTextColor(ContextCompat.getColor(this, R.color.medium_gray))
            binding.viewHistory.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_primary)
            binding.viewAdd.backgroundTintList = ContextCompat.getColorStateList(this, R.color.activity_bg)

            binding.tvHistory.typeface = ResourcesCompat.getFont(this, R.font.inter_medium)
            binding.tvAdd.typeface = ResourcesCompat.getFont(this, R.font.inter_regular)

            binding.clHistory.visibility = View.VISIBLE
            binding.clAdd.visibility = View.GONE
            binding.ivCalender.visibility = View.VISIBLE

            if (!viewModel.historyFetched.value) {
                viewModel.fetchExpenseHistory(hashmap)
            }

        }


        binding.llPending.setOnClickListener {
            binding.mcvPending.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.mcvApproved.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))
            binding.mcvReject.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))

            if (allExpenseList.size==0){
                binding.tvNodata.visibility = View.VISIBLE
                binding.rvExpensehistory.visibility = View.GONE
                binding.tvNodata.text = "No data found for this\nMonth ${hashmap["month"]}-${hashmap["year"]}"
            }else{
                adapter.submitList(allExpenseList)
                binding.tvNodata.visibility = View.GONE
                binding.rvExpensehistory.visibility = View.VISIBLE
            }

        }

        binding.llApproved.setOnClickListener {
            binding.mcvApproved.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.mcvPending.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))
            binding.mcvReject.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))

            if (pendingExpenseList.size==0){
                binding.tvNodata.visibility = View.VISIBLE
                binding.rvExpensehistory.visibility = View.GONE
                binding.tvNodata.text = "No pending data found for this\nMonth ${hashmap["month"]}-${hashmap["year"]}"
            }else{
                adapter.submitList(pendingExpenseList)
                binding.tvNodata.visibility = View.GONE
                binding.rvExpensehistory.visibility = View.VISIBLE
            }

        }

        binding.llReject.setOnClickListener {
            binding.mcvReject.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.mcvPending.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))
            binding.mcvApproved.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_line_color))

            if (apprvedExpenceList.size==0){
                binding.tvNodata.visibility = View.VISIBLE
                binding.rvExpensehistory.visibility = View.GONE
                binding.tvNodata.text = "No approved data found for this\nMonth ${hashmap["month"]}-${hashmap["year"]}"
            }else{
                adapter.submitList(apprvedExpenceList)
                binding.tvNodata.visibility = View.GONE
                binding.rvExpensehistory.visibility = View.VISIBLE
            }

        }


        binding.cardChoosefile.setOnClickListener {
            binding.edtTotaltask.clearFocus()
            binding.edtInfo.clearFocus()
            binding.edtAmount.clearFocus()
            KeyboardUtils.hideKeyboard(this@ActAddWork)
            showAttachmentOptions()
        }

        imageAdapter = SelectedImageAdapter { uri ->
            val updatedList = selectedImages.filterNot { it.toString() == uri.toString() }
            updateImageList(updatedList)
        }

        binding.recyclerViewImages.apply {
            layoutManager = LinearLayoutManager(this@ActAddWork, LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
            adapter = imageAdapter
        }


        binding.ivCalender.setOnClickListener {
            if (CommonMethods.isCheckNetwork(this)) {
                val now = Calendar.getInstance()
                showMonthYearPicker(
                    context = this,
//                    initialYear = now.get(Calendar.YEAR),
//                    initialMonthZeroBased = now.get(Calendar.MONTH)
                ) { monthZeroBased, year ->
                    val monthNumber = monthZeroBased + 1
                    val monthName   = monthNameFromZeroBased(monthZeroBased)

                    // UI update
//                    binding.tvMonth.text = "$monthName $year"

                    // Update params
                    hashmap["month"] = monthNumber.toString()
                    hashmap["year"]  = year.toString()

                    // Call API with updated month/year
                    viewModel.fetchExpenseHistory(hashmap)
                }
            } else {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
            }
        }


        binding.edtDate.setOnClickListener {

            if(CommonMethods.isCheckNetwork(this)) {

                val calendar = Calendar.getInstance()
                val year = calendar[Calendar.YEAR]
                val month = calendar[Calendar.MONTH]
                val day = calendar[Calendar.DAY_OF_MONTH]

                val datePickerDialog = DatePickerDialog(
                    this,
                    { datePicker, year, month, dayOfMonth ->
                        var d = dayOfMonth.toString()
                        var m = (month + 1).toString()
                        val y = year.toString()
                        if (d.length == 1) {
                            d = "0$d"
                        }
                        if (m.length == 1) {
                            m = "0$m"
                        }
                        val date = "$year-$m-$d"
                        val date2 = "$d-$m-$year"
                        binding.edtDate.setText(date)
                    },
                    year, month, day
                )
                datePickerDialog.datePicker.maxDate = calendar.timeInMillis
                datePickerDialog.show()
            }else{
                CommonMethods.alertDialogNoInternet(this, resources.getString(R.string.no_internet))
            }
        }


        binding.btnUpdate.setOnClickListener {
            if (binding.edtTotaltask.text.toString().trim().isEmpty()){
                CommonMethods.showErrorFullMsg(this@ActAddWork,"Title is required")
            }
            else if (binding.edtInfo.text.toString().trim().isEmpty()){
                CommonMethods.showErrorFullMsg(this@ActAddWork,"Enter bill details")
            }
            else if (binding.edtDate.text.toString().trim().isEmpty()){
                CommonMethods.showErrorFullMsg(this@ActAddWork,"Expense date is required")
            }
            else if (binding.edtAmount.text.toString().trim().isEmpty()){
                CommonMethods.showErrorFullMsg(this@ActAddWork,"Bill amount is required")
            }
            else if (selectedImages.isEmpty()){
                CommonMethods.showErrorFullMsg(this@ActAddWork,"Image is required")
            }else{
                viewModel.saveBill(
                    userId = sharePreference.getString(PrefKeys.userId),
                    title = binding.edtTotaltask.text.toString().trim(),
                    details = binding.edtInfo.text.toString().toString(),
                    amount = binding.edtAmount.text.toString().toString(),
                    expense_date = binding.edtDate.text.toString().toString(),
                    imageUris = selectedImages,
                    context = this
                )
//                CommonMethods.showSuccessFullMsg(this@ActAddWork,"Your work added successfully.")
            }
        }


    }


    private fun updateImageList(newList: List<Uri>) {
        selectedImages.clear()
        selectedImages.addAll(newList)

        imageAdapter.submitList(selectedImages.toList())

        binding.recyclerViewImages.visibility =
            if (selectedImages.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCameraInternal()
        else Toast.makeText(this@ActAddWork, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }


    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            lifecycleScope.launch {
                ProgressDialogUtil.showAleartLoadingProgress(
                    this@ActAddWork,
                     lifecycleScope,
                    "Processing...",
                    "Please wait while we are compressing your image"
                )

                val compressedUri = withContext(Dispatchers.IO) {
                    CommonMethods.compressImageFromUri(this@ActAddWork, cameraImageUri!!)
                }

                compressedUri?.let { uri ->
                    val updatedList = (selectedImages + uri).distinctBy { it.toString() }
                    updateImageList(updatedList)
                }

                ProgressDialogUtil.dismiss()
            }
        }
    }


    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri :Uri? -> // GetMultipleContents()
        if (uri!=null) {
            ProgressDialogUtil.showAleartLoadingProgress(
                this@ActAddWork,
                lifecycleScope,
                "Processing...",
                "Please wait while we are processing your images"
            )

            lifecycleScope.launch {
                val newCompressedUris = withContext(Dispatchers.IO) {
//                    uris.mapNotNull { uri ->
                        CommonMethods.compressImageFromUri(this@ActAddWork, uri)
//                    }
                }

                // Remove duplicates using URI.toString()
//                val uniqueUris = (selectedImages + newCompressedUris)
//                    .distinctBy { it.toString() }
//                updateImageList(uniqueUris)
                newCompressedUris?.let { newUri ->
                    // Update your selected image list with this single image
                    updateImageList(listOf(newUri))
                }

                ProgressDialogUtil.dismiss()
            }
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun launchCameraInternal() {
        val imageFile = File.createTempFile("photo_", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        takePhotoLauncher.launch(uri)
    }


    private fun showAttachmentOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this@ActAddWork)
            .setTitle("Select Option")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> openGallery()
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun observeUIStateSaveBill(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.saveBillUiState.collect{ state ->
                    when(state){
                        is UIState.Idle ->{

                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showAleartLoadingProgress(this@ActAddWork,lifecycleScope,"Uploading...","Please wait we are collecting your bill.")
                        }
                        is UIState.Success ->{
                            CommonMethods.alertErrorOrValidationDialog(this@ActAddWork,"Your bill added successfully\nYou can check in history section")
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_saveBillUiState()

                            binding.edtInfo.text = null
                            binding.edtTotaltask.text =null
                            binding.edtDate.text = null
                            binding.edtAmount.text = null
                            selectedImages.clear()
                            updateImageList(selectedImages)

//                            lifecycleScope.launch {
//                                delay(1000)
//                                finish()
//                            }
                        }
                        is UIState.Error ->{

                            Log.e("BookAppointment booking", state.message)
                            CommonMethods.alertErrorOrValidationDialog(this@ActAddWork,state.message)
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_saveBillUiState()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseHistoryAdapter()
        binding.rvExpensehistory.apply {
            layoutManager = LinearLayoutManager(this@ActAddWork)
            setHasFixedSize(true)
            adapter = this@ActAddWork.adapter
        }
    }


    private fun observeExpenseHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expenseHistoryUiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(this@ActAddWork, lifecycleScope)
                        }
                        is UIState.Success -> {
                            allExpenseList = state.data
                            adapter.submitList(allExpenseList)

                            if (allExpenseList.size==0){
                                binding.tvNodata.visibility = View.VISIBLE
                                binding.rvExpensehistory.visibility = View.GONE
                                binding.tvNodata.text = "No data found for this\nMonth ${hashmap["month"]}-${hashmap["year"]}"
                            }else{
                                binding.tvNodata.visibility = View.GONE
                                binding.rvExpensehistory.visibility = View.VISIBLE
                            }

                            // set to All
                            binding.mcvPending.setCardBackgroundColor(ContextCompat.getColor(this@ActAddWork, R.color.white))
                            binding.mcvApproved.setCardBackgroundColor(ContextCompat.getColor(this@ActAddWork, R.color.gray_line_color))
                            binding.mcvReject.setCardBackgroundColor(ContextCompat.getColor(this@ActAddWork, R.color.gray_line_color))

                            pendingExpenseList = allExpenseList.filter { it.status?.lowercase() == "pending" }
                            apprvedExpenceList = allExpenseList.filter { it.status?.lowercase() == "approved" }

                            // Set count of data
                            binding.tvPendingcount.text = allExpenseList.size.toString()
                            binding.tvApprovedcount.text = pendingExpenseList.size.toString()
                            binding.tvRejectedcount.text = apprvedExpenceList.size.toString()

                            ProgressDialogUtil.dismiss()

                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            binding.tvNodata.visibility = View.VISIBLE
                            binding.rvExpensehistory.visibility = View.GONE
                            binding.tvNodata.text = "No data found for this\nMonth ${hashmap["month"]}-${hashmap["year"]}"
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@ActAddWork, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@ActAddWork)
                                return@collect
                            }
//                            Toast.makeText(this@ActAddWork, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
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
        val currentMonth = now.get(Calendar.MONTH)

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
                allMonths.sliceArray(0 until currentMonth+1)
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

    fun monthNameFromZeroBased(index: Int): String {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, index.coerceIn(0, 11)) }
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

}