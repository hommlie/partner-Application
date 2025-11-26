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
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private lateinit var rvSalayBreakDown : RecyclerView
    private lateinit var rvDeductionBreakDown : RecyclerView

    private lateinit var salaryAdapter: BreakDownAdapter
    private lateinit var deductionAdapter: BreakDownAdapter
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
        setupToolbar(toolbarView, "Pay Slip", this, R.color.white, R.color.black)

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

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.getPaySlip(hashmap)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.salaryBreakDown.collect { state ->
                    when (state) {
                        is UIState.Idle -> { /* Do nothing */
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@PaySlip, lifecycleScope,
                            "Loading...",
                            "Please wait while we are generating your salary slip"
                        )
                            binding.progressBar.visibility = View.VISIBLE
                    }
                        is UIState.Success ->{
//                            salaryAdapter.updateData(state.data)

                            state.data.link?.let { loadPdf(it) }

                            binding.btnDownload.visibility = View.VISIBLE
                            binding.llPdf.visibility = View.VISIBLE
                            binding.tvNotransactionfound.visibility = View.GONE
                            binding.btnDownload.setOnClickListener { state.data.link?.let { it1 ->
                                downloadPdf(
                                    it1
                                )
                            } }

                            ProgressDialogUtil.dismiss()
                            viewModel.resetSalaryBreakDownUi()
                        }

                        is UIState.Error ->{
//                            CommonMethods.alertErrorOrValidationDialog(this@PaySlip, state.message)
                            binding.progressBar.visibility = View.GONE
                            binding.llPdf.visibility = View.GONE
                            binding.tvNotransactionfound.visibility = View.VISIBLE
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



    fun monthNameFromZeroBased(index: Int): String {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, index.coerceIn(0, 11)) }
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

    private fun loadPdf(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val inputStream = response.body?.byteStream()

                pdfFile = File(cacheDir, "temp.pdf")
                inputStream?.use { input ->
                    FileOutputStream(pdfFile!!).use { output ->
                        input.copyTo(output)
                    }
                }

                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pfd)
                val page = pdfRenderer!!.openPage(0)
                currentPage = page
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.pdfPage.setImageBitmap(bitmap)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@PaySlip, "Failed to load PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadPdf(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Payslip.pdf")
                .setDescription("Downloading PDF")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Payslip.pdf")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        currentPage?.close()
        pdfRenderer?.close()
        super.onDestroy()
    }

}