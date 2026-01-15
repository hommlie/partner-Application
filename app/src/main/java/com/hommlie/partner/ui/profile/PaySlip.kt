package com.hommlie.partner.ui.profile

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityPaySlipBinding
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
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.hommlie.partner.model.SalaryBreakDown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class PaySlip : AppCompatActivity() {

    private lateinit var binding : ActivityPaySlipBinding

    private val viewModel : PaySlipViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference

    private var hashmap = HashMap<String,String>()

    private var pdfFile: File? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaySlipBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }

        } else {
            // This is Android 14 or below

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Pay Slip", this, R.color.ub__transparent, R.color.black)

        hashmap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashmap["month"] = CommonMethods.getCurrentMonthNumber().toString()
        hashmap["year"]  = CommonMethods.getCurrentYear().toString()
        hashmap["date"] = "${hashmap["year"]}-${hashmap["month"]}-01"

        binding.tvMonth.text = CommonMethods.getPreviousMonthName()+" "+CommonMethods.getCurrentYear()


        viewModel.getPaySlip(hashmap)

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
                    binding.tvMonth.text = "$monthName $year"

                    // Update params
                    hashmap["month"] = monthNumber.toString()
                    hashmap["year"]  = year.toString()

                    // Call API with updated month/year
                    viewModel.getPaySlip(hashmap)
                }
            } else {
                CommonMethods.alertDialogNoInternet(this, getString(R.string.no_internet))
            }
        }

        binding.btnDownload.setOnClickListener {
//            sharePdfFile(pdfFile!!)   // OR save to downloads
            pdfFile?.let { showPdfSuccess(it) }

        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.salaryBreakDown.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@PaySlip, lifecycleScope,
                            "Loading...",
                            "Please wait while we are generating your salary slip"
                        )

                    }
                        is UIState.Success -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                generatePayslipPdf(state.data) // PDF generate in background

//                                withContext(Dispatchers.Main) {
//                                    ProgressDialogUtil.dismiss()   // UI update on main thread
                                    viewModel.resetSalaryBreakDownUi()
//                                }
                            }
                        }


                        is UIState.Error ->{
//                            CommonMethods.alertErrorOrValidationDialog(this@PaySlip, state.message)

                            binding.tvNotransactionfound.visibility = View.VISIBLE
                            binding.pdfView.visibility = View.GONE
                            binding.btnDownload.visibility = View.GONE
                            ProgressDialogUtil.dismiss()
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@PaySlip, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@PaySlip)
                                return@collect
                            }
                            viewModel.resetSalaryBreakDownUi()
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
        val currentMonth = now.get(Calendar.MONTH) // 0 = Jan, 7 = Aug

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

       /* fun updateMonthPicker(selectedYear: Int, keepMonthIndex: Int) {
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
        } */
       fun updateMonthPicker(selectedYear: Int, keepMonthIndex: Int) {

           val allowedMonths = if (selectedYear == currentYear) {
               if (currentMonth == 0) {
                   // January → no previous month
                   emptyArray()
               } else {
                   allMonths.sliceArray(0 until currentMonth)
               }
           } else {
               allMonths
           }

           monthPicker.displayedValues = null

           if (allowedMonths.isEmpty()) {
               // disable month picker safely
               monthPicker.minValue = 0
               monthPicker.maxValue = 0
               monthPicker.displayedValues = arrayOf("N/A")
               monthPicker.value = 0
               monthPicker.isEnabled = false
               return
           }

           monthPicker.isEnabled = true
           monthPicker.minValue = 0
           monthPicker.maxValue = allowedMonths.size - 1
           monthPicker.displayedValues = allowedMonths

           val safeIndex = keepMonthIndex.coerceIn(0, allowedMonths.size - 1)
           monthPicker.value = safeIndex
       }


        // initialize with current year & current month -1 (previous)
//        val initialMonth = (currentMonth - 1).coerceAtLeast(0)
        val initialMonth = if (currentMonth == 0) 0 else currentMonth - 1
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

    override fun onDestroy() {
        currentPage?.close()
        pdfRenderer?.close()
        super.onDestroy()
    }

    private fun generatePayslipPdf(data: SalaryBreakDown) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfGenerator = PayslipPdfGenerator(this@PaySlip)

                // Prepare earnings map
                val earnings = mapOf(
                    "basic" to data.basic,
                    "hra" to data.hra,
                    "conveyance" to data.conveyance,
                    "medicalAllowance" to data.medicalAllowance,
                    "groomingAllowance" to data.groomingAllowance,
                    "travel_allowance" to data.travel_allowance,
                    "extra" to data.extra
                )

                // Prepare deductions map (add more as needed)
                val deductions = mapOf(
                    "advance" to data.advance
                )

                val payslipData = PayslipPdfGenerator.PayslipData(
                    empName = data.emp_name,
                    empId = data.emp_id,
                    empPhone = data.emp_phone,
                    aadharNo = data.aadhar_no,
                    uinNo = data.uin_no,
                    selectedMonth = data.selectedMonthWords,
                    cycleStart = data.cycleStart,
                    cycleEnd = data.cycleEnd,
                    presentDays = data.presentDays ?: 0,
                    paidLeaves = data.paidLeaves ?: 0,
                    location = data.location,
                    payDate = data.pay_date,
                    earnings = earnings,
                    deductions = deductions,
                    netPay = data.net_pay ?: "0.00"
                )

//                // Generate file path
//                val fileName = PayslipPdfGenerator.getPayslipFileName(
//                    data.emp_name,
//                    data.selectedMonthWords
//                )
//                val directory = PayslipPdfGenerator.getPayslipDirectory(this@PaySlip)
//                val filePath = File(directory, fileName).absolutePath
                val fileName = PayslipPdfGenerator.getPayslipFileName(
                    data.emp_name,
                    data.selectedMonthWords
                )

                val directory = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "Payslips"
                )

                if (!directory.exists()) directory.mkdirs()

                val filePath = File(directory, fileName).absolutePath

                // Generate PDF
                pdfFile = pdfGenerator.generatePayslipPdf(payslipData, filePath)

                // Switch to main thread to show success
                withContext(Dispatchers.Main) {
//                    showPdfSuccess(pdfFile)
                    renderPdfInsideApp(pdfFile!!)
                    ProgressDialogUtil.dismiss()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PaySlip,
                        "Failed to generate PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showPdfSuccess(pdfFile: File) {
        AlertDialog.Builder(this)
            .setTitle("Payslip Generated")
            .setMessage("Payslip has been saved")// to:\n${pdfFile.absolutePath}")
            .setPositiveButton("Open") { _, _ ->
                openPdfFile(pdfFile)
            }
            .setNegativeButton("Share") { _, _ ->
                sharePdfFile(pdfFile)
            }
            .setNeutralButton("Close", null)
            .setCancelable(false)
            .show()
    }
    /*private fun renderPdfInsideApp(pdfFile: File) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            if (pdfRenderer!!.pageCount > 0) {
                currentPage = pdfRenderer!!.openPage(0)

                val bitmap = Bitmap.createBitmap(
                    currentPage!!.width,
                    currentPage!!.height,
                    Bitmap.Config.ARGB_8888
                )

                currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                binding.pdfView.visibility = View.VISIBLE
                binding.pdfView.fromFile(pdfFile)
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .spacing(10)
                    .load()

                binding.btnDownload.visibility = View.VISIBLE
                binding.tvNotransactionfound.visibility = View.GONE
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Preview failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } */
    private fun renderPdfInsideApp(pdfFile: File) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            if (pdfRenderer!!.pageCount > 0) {
                currentPage = pdfRenderer!!.openPage(0)

                val bitmap = Bitmap.createBitmap(
                    currentPage!!.width,
                    currentPage!!.height,
                    Bitmap.Config.ARGB_8888
                )

                currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                binding.pdfView.visibility = View.VISIBLE
                binding.pdfView.setImageBitmap(bitmap)
//                Glide.with(this@PaySlip).load(bitmap).into(binding.pdfView)


                binding.btnDownload.visibility = View.VISIBLE
                binding.tvNotransactionfound.visibility = View.GONE
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Preview failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        startActivity(Intent.createChooser(shareIntent, "Share Payslip"))
    }

}