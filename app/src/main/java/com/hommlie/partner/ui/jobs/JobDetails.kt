package com.hommlie.partner.ui.jobs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityJobDetailsBinding
import com.hommlie.partner.databinding.BottomsheetFeetbackBinding
import com.hommlie.partner.databinding.BottomsheetOtpBinding
import com.hommlie.partner.databinding.BottomsheetPaymentBinding
import com.hommlie.partner.databinding.BottomsheetSignatureBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.model.ServiceModel
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.KeyboardUtils
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.SignatureView
import com.hommlie.partner.utils.SwipeButton
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@AndroidEntryPoint
class JobDetails : AppCompatActivity() {

    private lateinit var binding : ActivityJobDetailsBinding

    private val viewModel : JobDetailsViewModel by viewModels()
    private var payment_dialog : Dialog?=null

    lateinit var signatureView: SignatureView

    @Inject
    lateinit var sharePreference: SharePreference

    private var check_paymentStatusJob: Job? = null

    private lateinit var jobData: NewOrderData

    private lateinit var dialogView: View
    private lateinit var btnSubmit: Button
    private lateinit var alertDialog: AlertDialog
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncherForCheque : ActivityResultLauncher<Intent>

    val hashMap =  HashMap<String,String>()

    var isComeFromHome : Int = 0

    companion object{
        var isonsiteAnswersubmit = MutableLiveData<String>()
        var isOnCompleteAnswersubmit = MutableLiveData<String>()
        var isonCompleteChemicalFilled = MutableLiveData<String>()
        var OnSiteQuestions = ""
        var OnCompletedQuestions = ""
        var imagewhenStart : Bitmap?=null
        var chequeImage : Bitmap ?= null
        var serviceStartAt : MutableLiveData<String> = MutableLiveData()
        var signature : ByteArray?=null
    }

//    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        binding = ActivityJobDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // For Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                CommonMethods.getToast(this@JobDetails, "Back is disabled on this screen")
            }
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
        setupToolbar(toolbarView, "Job Details", this, R.color.transparent, R.color.black)

        isonsiteAnswersubmit.value = "0"
        isOnCompleteAnswersubmit.value = "0"
        isonCompleteChemicalFilled.value ="0"
        OnSiteQuestions = "0"
        OnCompletedQuestions = "0"
        imagewhenStart = null
        chequeImage = null

        isComeFromHome = intent.getIntExtra("isComeFromHome",0)

//        val json = intent.getStringExtra("job_data")
//        jobData = Gson().fromJson(json, NewOrderData::class.java)

        val json = intent.getStringExtra("job_data")
        if (!json.isNullOrEmpty()) {
            jobData = Gson().fromJson(json, NewOrderData::class.java)
        } else {
            Log.e("JobDetails", "Job data is missing in intent!")
        }

        binding.tvCustName.text = jobData.name
//        binding.tvVariation.text="${jobData.attribute?:"-"}\n${jobData.serviceName?:"-"}"
//        binding.tvSize.text=jobData.variation?:"-"
//        binding.tvCategory.text=jobData.categoryName?:"-"
//        binding.tvSubcategory.text=jobData.subcategoryName?:"-"
        binding.tvType.text="Visit ID :- "+jobData.orderId


        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
//        hashMap["order_id"] = jobData.orderId.toString()
        hashMap["visit_id"] = jobData.orderId.toString()


        if (jobData.orderStatus=="3"){
            serviceStartAt.value = jobData.onsite_updated_at

        }else if(jobData.orderStatus =="2"){
            serviceStartAt.value = "yet to start"
            viewModel.sentOnsiteotp(hashMap)
            showOTPBottomsheet(this)
        }

        isonsiteAnswersubmit.value =  jobData.IsQuestionsSubmitted
        OnSiteQuestions = jobData.OnSiteQuestions.toString()
        OnCompletedQuestions = jobData.OnCompletedQuestions.toString()


        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the captured image
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.extras?.get("data") as? Bitmap
                }
                // Use the bitmap if it's not null
                imageBitmap?.let {
                    imagewhenStart = it
                    binding.ivCaptureImagebeforejobstart.setImageBitmap(it)
                }
            }
        }
        cameraLauncherForCheque = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.extras?.get("data") as? Bitmap
                }
                bitmap?.let { bmp ->
                    chequeImage = bmp
                    // Callback to update BottomSheet image
                    cameraResultCallbackForCheque?.invoke(bmp)
                }
            }
        }
        setupServiceListAdapter(binding.rvService, jobData.services)

        observeStartTime()
        observeDuration()

        observeJobFinish()

        isonsiteAnswersubmit.observe(this) { data ->
            if (data == "1"){
                binding.swipebtn.text ="Completed"
            }
        }

        isOnCompleteAnswersubmit.observe(this) { data ->
            if (data =="1"){
//                showPaymentBottomsheet(this@JobDetails)
                binding.swipebtn.text = "Finish Job"
//                binding.mcvSwipebtn.visibility=View.GONE
            }
        }



        serviceStartAt.observe(this) { data ->
            if (!data.isNullOrEmpty() && data!="yet to start") {
                viewModel.startDurationUpdater(data)
                viewModel.showStartedTime(data)
                binding.ivCaptureImagebeforejobstart.isEnabled = false
            }else{
                binding.tvStartTime.text = data
            }
        }


        dialogView = LayoutInflater.from(this).inflate(R.layout.success_bottomsheet_dialog, null)
        btnSubmit = dialogView.findViewById<Button>(R.id.btn_takeselfie)

        btnSubmit.setOnClickListener {
            alertDialog.dismiss()
            takeSelfie()
        }

//        orderLastUpdated_at = intent.getStringExtra("updated_at").toString()

        observSendOTP()




        viewModel.updateOrderStatus(jobData.orderStatus.toString())

        viewModel.orderStatus.observe(this) { status ->

            if (viewModel.orderStatus.value == "2"){
//                binding.swipebtn.text = "Start Pre-Inspection"
                binding.mcvSwipebtn.visibility = View.VISIBLE

                if (OnSiteQuestions == "1"){
                    if (isonsiteAnswersubmit.value == "1"){
                        binding.swipebtn.text = "Completed"
                    }else{
                        binding.swipebtn.text = "Start Pre-Inspection"
//                        CommonMethods.getToast(this@JobDetails, isonsiteAnswersubmit.value!!.toString())
                    }
                }else{
                    binding.swipebtn.text = "Completed"
                }

            }else if (viewModel.orderStatus.value == "3"){
                binding.swipebtn.text = "Start Post-Inspection"

                if (isComeFromHome == 0) {
                    if (OnCompletedQuestions == "1") {
                        if (isOnCompleteAnswersubmit.value == "1") {
                            binding.swipebtn.text = "Completed"
                            binding.mcvSwipebtn.visibility = View.GONE

//                            showPaymentSheetAfterCheckingPaymentStatus()
                            showSignatureBottomsheet()
                        } else {
                            binding.swipebtn.text = "Start Post-Inspection"
                            binding.swipebtn.showResultIcon(false, true)
                            binding.mcvSwipebtn.visibility = View.VISIBLE
                        }
                    } else {
                        binding.swipebtn.text = "Completed"
                        binding.mcvSwipebtn.visibility = View.GONE

                       // showPaymentSheetAfterCheckingPaymentStatus()
                        showSignatureBottomsheet()
                    }
                    binding.statusAutocomplete.setText("Complete")
                }else{

                    if (OnSiteQuestions == "1"){
                        if (isonsiteAnswersubmit.value == "1"){
                            binding.swipebtn.text = "Completed"
                            binding.mcvSwipebtn.visibility = View.VISIBLE
                            isComeFromHome = 0
                        }else{
                            viewModel.updateOrderStatus("2")
                            isComeFromHome = 0
                        }
                    }else{
                        binding.swipebtn.text = "Completed"
                        binding.mcvSwipebtn.visibility = View.VISIBLE
                        isComeFromHome = 0
                    }
//                    binding.swipebtn.text = "Completed"
//                    binding.mcvSwipebtn.visibility = View.VISIBLE
//                    isComeFromHome = 0
                    binding.statusAutocomplete.setText("On-Site")
                }


            }else{
                binding.mcvSwipebtn.visibility = View.GONE
            }

        }



        binding.swipebtn.setOnSwipeListener(object : SwipeButton.OnSwipeListener {
            override fun onButtonTouched() {

            }

            override fun onButtonReleased() {

            }

            override fun onSwipeConfirm() {
                if (CommonMethods.isCheckNetwork(applicationContext)) {
                    if (binding.swipebtn.text=="Start Pre-Inspection") {
                        val intent = Intent(this@JobDetails, ActQuestionary::class.java)
                        intent.putExtra("orderId", jobData.orderId.toString())
                        intent.putExtra("questionfor","Onsite")
                        intent.putExtra("order_status",viewModel.orderStatus.value)
                        startActivity(intent)
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(800)
                            binding.swipebtn.showResultIcon(false, true)
                        }
                    }
                    if (binding.swipebtn.text=="Start Post-Inspection"){
                        val intent = Intent(this@JobDetails, ActQuestionary::class.java)
                        intent.putExtra("orderId", jobData.orderId.toString())
                        intent.putExtra("questionfor","OnCompleted")
                        intent.putExtra("order_status",viewModel.orderStatus.value)
                        startActivity(intent)

                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(800)
                            binding.swipebtn.showResultIcon(false, true)
                        }

                    }
                    if (binding.swipebtn.text == "Completed"){
                        lifecycleScope.launch {
                            delay(500)
                            viewModel.updateOrderStatus("3")
                        }
                    }
                    if (binding.swipebtn.text == "Finish Job"){
//                        showPaymentSheetAfterCheckingPaymentStatus()
                        showSignatureBottomsheet()
                    }

                } else {
                    binding.swipebtn.showResultIcon(false, true)
                    CommonMethods.alertErrorOrValidationDialog(this@JobDetails, resources.getString(R.string.no_internet))

                }

            }
        })

    }

    fun byteArrayToBase64(signature: ByteArray?): String? {
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }


    private fun showPaymentSheetAfterCheckingPaymentStatus(){

        val map = HashMap<String, RequestBody>()
        map["user_id"] = sharePreference.getString(PrefKeys.userId).toRequestBody("text/plain".toMediaTypeOrNull())
        map["visit_id"] = jobData.orderId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        map["order_status"] = "4".toRequestBody("text/plain".toMediaTypeOrNull())

        if(signature!=null) {
            map["singnature"] = byteArrayToBase64(signature)!!.toRequestBody("text/plain".toMediaTypeOrNull())
        }


        if (jobData.payment_type == "2"){  // paid by wallet
            viewModel.jobFinish(map)

        }else if (jobData.payment_type == "3"){  // paid by online
            viewModel.jobFinish(map)
        }
        else if (jobData.payment_type == "1"){   // cod

            if (jobData.payment_status == "1"){  // paid
                viewModel.jobFinish(map)

            }else if (jobData.payment_status == "0"){  // pending
                val payment_dialog = showPaymentBottomsheet(this@JobDetails,map)
                observePaymentStatus(map,payment_dialog)
            }
        }

    }

    private fun clearOtpFocus(binding: BottomsheetOtpBinding) {
        val otpFields = listOf(
            binding.otpDigit1,
            binding.otpDigit2,
            binding.otpDigit3,
            binding.otpDigit4,
            binding.otpDigit5,
            binding.otpDigit6
        )
        otpFields.forEach { it.clearFocus() }
    }



    private fun observeEnteredOtp(binding: BottomsheetOtpBinding) {
        lifecycleScope.launch {
            viewModel.enteredOtp.collect { otp ->
                if (otp.length == 6) {
                    clearOtpFocus(binding)
                    KeyboardUtils.hideKeyboard(binding.otpDigit6)
                }
            }
        }
    }




    fun showOTPBottomsheet(context: Activity) {
        val dialog = BottomSheetDialog(context)

        val binding = BottomsheetOtpBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.tvNotedesc.text="${binding.tvNotedesc.text.toString()} ${jobData.mobile}."

        observeEnteredOtp(binding)

        val swipeBtn = binding.swipebtn
        swipeBtn.setOnSwipeListener(object : SwipeButton.OnSwipeListener {
            override fun onButtonTouched() {
//                mBottomSheetBehavior.isDraggable = false
            }

            override fun onButtonReleased() {
                /*if (tripDetailsModel.riderDetails.size>1)

                else
                    mBottomSheetBehavior.setAllowUserDragging(false)*/
//                mBottomSheetBehavior.isDraggable = true

            }

            override fun onSwipeConfirm() {

                if (CommonMethods.isCheckNetwork(applicationContext)) {
//                    dialog.dismiss()
                    val map = HashMap<String, RequestBody>()
                    map["user_id"] = sharePreference.getString(PrefKeys.userId).toRequestBody("text/plain".toMediaTypeOrNull())
                    map["visit_id"] = jobData.orderId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    map["order_status"] = "3".toRequestBody("text/plain".toMediaTypeOrNull())
                    map["otp"] = viewModel.enteredOtp.value.toRequestBody("text/plain".toMediaTypeOrNull())

                    observServiceStart_OTPVerified(dialog,swipeBtn)

                    val otp = viewModel.enteredOtp.value.orEmpty()
                    val imageBitmap = imagewhenStart

                    when {
                        otp.length < 6 -> {
                            CommonMethods.getToast(this@JobDetails, "Please Enter OTP")
                            swipeBtn.showResultIcon(false, true)
                        }
                        imageBitmap == null -> {
                            Log.e("ActService", "Image is null, cannot proceed with API call.")
                            showCustomDialog()
                            swipeBtn.showResultIcon(false, true)
                        }
                        else -> {
                            val imagePart = CommonMethods.prepareImagePart("emp_onsite_image", imageBitmap)
                            viewModel.verifyOtp_changeOrderStatus(map, imagePart)
                        }
                    }


                } else {
                    swipeBtn.showResultIcon(false, true)
                    CommonMethods.alertErrorOrValidationDialog(this@JobDetails, resources.getString(R.string.no_internet))

                }

            }
        })

        binding.tvTimer.setOnClickListener {
//            viewModel.resendOtp()
            viewModel.sentOnsiteotp(hashMap)
        }
        binding.tvCancel.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        observeTimer(binding.tvTimer)
        setOtpListeners(binding)
        dialog.setCancelable(false)
        dialog.show()
    }


    fun showPaymentBottomsheet(context: Activity, map: HashMap<String, RequestBody>): Dialog {
        var chequeno = viewModel.chequeno.value.orEmpty()
        var chequeBitmap = chequeImage

        val dialog = BottomSheetDialog(context)
        val binding = BottomsheetPaymentBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        observeGeneratePaymentQR(binding)

        // --- Common Reset Logic
        fun resetChequeFields() {
            map.remove("type")
            map.remove("cheque_number")
            chequeImage = null
            chequeBitmap = null
            viewModel.updateChequeNo("")
            chequeno = ""
            binding.edtChequeno.setText(null)
            binding.ivChequeimage.setImageDrawable(context.getDrawable(R.drawable.ic_photo_camera))
        }

        binding.tvAmount.text = "\u20b9"+jobData.price

        binding.edtChequeno.addTextChangedListener {
            viewModel.updateChequeNo(it.toString())
        }

        // --- Payment Method Selection
        binding.rbUpi.setOnClickListener {

            binding.llCheque.visibility = View.GONE
            binding.mcvSwipebtn.visibility = View.GONE

            val qrImageUrl = viewModel.qrImageUrl.value

            if (qrImageUrl.isNullOrEmpty()) {
                binding.ivQr.visibility = View.GONE
                binding.clProgress.visibility = View.GONE
            } else {
                binding.ivQr.visibility = View.VISIBLE
                binding.clProgress.visibility = View.VISIBLE
                Glide.with(this@JobDetails)
                    .load(qrImageUrl)
                    .placeholder(R.drawable.ic_qr)
                    .error(R.drawable.ic_refer)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerInside()
                    .into(binding.ivQr)

                checkPaymentStatus()
            }

            binding.swipebtn.text = "Generate QR"
            binding.tvGenerateqr.visibility = View.VISIBLE

            resetChequeFields()
        }


        binding.tvGenerateqr.setOnClickListener {
            viewModel.generatePaymentQR(
                hashMapOf(
                    "user_id" to sharePreference.getString(PrefKeys.userId),
                    "order_id" to jobData.orderId.toString()
                )
            )
        }

        binding.rbCash.setOnClickListener {
            binding.ivQr.visibility = View.GONE
            binding.llCheque.visibility = View.GONE
            binding.mcvSwipebtn.visibility = View.VISIBLE
            binding.clProgress.visibility = View.GONE
            binding.tvGenerateqr.visibility = View.GONE
            binding.swipebtn.text = "Cash Collected"

            resetChequeFields()
            stopCheckingPaymentStatus()
        }

        binding.rbCheque.setOnClickListener {
            binding.ivQr.visibility = View.GONE
            binding.clProgress.visibility = View.GONE
            binding.llCheque.visibility = View.VISIBLE
            binding.mcvSwipebtn.visibility = View.VISIBLE
            binding.tvGenerateqr.visibility = View.GONE
            binding.swipebtn.text = "Cheque Collected"

            stopCheckingPaymentStatus()
        }

        // --- Cheque Image Capture
        binding.ivChequeimage.setOnClickListener {
            val updateImage: (Bitmap) -> Unit = { bmp ->
                binding.ivChequeimage.setImageBitmap(bmp)
            }
            launchCameraforCheque(updateImage)
        }


        // --- Swipe Actions
        binding.swipebtn.setOnSwipeListener(object : SwipeButton.OnSwipeListener {
            override fun onButtonTouched() {}
            override fun onButtonReleased() {}

            override fun onSwipeConfirm() {
                if (!CommonMethods.isCheckNetwork(context)) {
                    binding.swipebtn.showResultIcon(false, true)
                    CommonMethods.alertErrorOrValidationDialog(context, "No internet connection")
                    return
                }

                when (binding.swipebtn.text) {
                    "Cash Collected" -> {
                        payment_dialog = dialog
                        viewModel.jobFinish(map)
                        lifecycleScope.launch {
                            delay(800)
                            binding.swipebtn.showResultIcon(false, true)
                        }
                    }

                    "Cheque Collected" -> {
                        payment_dialog = dialog
                        chequeno = viewModel.chequeno.value.orEmpty()
                        chequeBitmap = chequeImage

                        when {
                            chequeno.length < 4 -> {
                                CommonMethods.getToast(context, "Please Enter Cheque No.")
                                binding.swipebtn.showResultIcon(false, true)
                            }

                            chequeBitmap == null -> {
                                CommonMethods.getToast(context, "Upload Cheque Image")
                                binding.swipebtn.showResultIcon(false, true)
                            }

                            else -> {
                                map["type"] = "cheque".toRequestBody("text/plain".toMediaTypeOrNull())
                                map["cheque_number"] = chequeno.toRequestBody("text/plain".toMediaTypeOrNull())

                                chequeBitmap?.let { bmp ->
                                    val chequePart = CommonMethods.prepareImagePart("cheque_images", bmp)
                                    viewModel.jobFinishWhenCheque(map, chequePart)
                                }
                            }
                        }
                    }
                }
            }
        })

        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

    fun showSignatureBottomsheet(): Dialog {


        val dialog = BottomSheetDialog(this)
        val binding = BottomsheetSignatureBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(binding.root)

        signatureView = binding.signatureView

        binding.cardSaveSignature.setOnClickListener {

            if (!signatureView.hasSignature()) {
                Toast.makeText(this, "Please take customer signature", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val savedSignature = withContext(Dispatchers.IO) {
                    signatureView.saveSignatureToFile()
                }

                if (savedSignature != null) {
                    signature = savedSignature
                    dialog.dismiss()
                    showPaymentSheetAfterCheckingPaymentStatus()
                } else {
                    Toast.makeText(this@JobDetails, "Please Re-take signature", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cardClearSignature.setOnClickListener {
            signatureView.clearSignature()
        }


        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }




    fun showFeedbackBottomsheet(context: Activity) {
        val dialog = BottomSheetDialog(context)

        val binding = BottomsheetFeetbackBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.tvSkip.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        binding.mcvSwipebtn.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun observeTimer(tvTimer: TextView) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect { time ->
                    tvTimer.text = time
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.canResend.collect { canResend ->
                    tvTimer.isEnabled = canResend
                }
            }
        }
    }


    private fun setOtpListeners(binding: BottomsheetOtpBinding) {
        val otpFields = listOf(
            binding.otpDigit1,
            binding.otpDigit2,
            binding.otpDigit3,
            binding.otpDigit4,
            binding.otpDigit5,
            binding.otpDigit6
        )

        for (i in otpFields.indices) {
            val current = otpFields[i]
            val next = otpFields.getOrNull(i + 1)
            val prev = otpFields.getOrNull(i - 1)

            current.addTextChangedListener(GenericTextWatcher(current, next, prev,binding))
            setBackspaceListener(current, prev)
        }
    }

    private fun setBackspaceListener(editText: EditText, previousView: View?) {
        editText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL &&
                event.action == KeyEvent.ACTION_DOWN &&
                editText.text.isEmpty()
            ) {
                previousView?.requestFocus()
                true
            } else {
                false
            }
        }
    }

    private inner class GenericTextWatcher(
        private val currentView: View,
        private val nextView: View?,
        private val previousView: View?,
        private val binding: BottomsheetOtpBinding
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (s?.length == 1) {
                nextView?.requestFocus()
            } else if (s?.isEmpty() == true) {
                previousView?.requestFocus()
            }

            val otp = listOf(
                binding.otpDigit1.text.toString(),
                binding.otpDigit2.text.toString(),
                binding.otpDigit3.text.toString(),
                binding.otpDigit4.text.toString(),
                binding.otpDigit5.text.toString(),
                binding.otpDigit6.text.toString()
            ).joinToString("")
            viewModel.updateOtp(otp)
        }
    }


    fun generateQrCodeFromUrl(paymentUrl: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(paymentUrl, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

//    GET https://api.razorpay.com/v1/payment_links/{plink_id}





    fun observSendOTP(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateSendOTP.collect{ state ->
                    when(state){
                        is UIState.Loading->{
                            ProgressDialogUtil.showLoadingProgress(this@JobDetails,lifecycleScope)
                        }
                        is UIState.Success->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIStatesentOnsiteotp()
                            viewModel.startOtpTimer()
//                            showOTPBottomsheet(this@JobDetails)
                            CommonMethods.getToast(this@JobDetails,"OTP Sent Successfully.")
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIStatesentOnsiteotp()
                            CommonMethods.getToast(this@JobDetails,state.message)
                        }
                        is UIState.Idle->{

                        }

                    }
                }
            }
        }
    }

    fun observServiceStart_OTPVerified(dialog: BottomSheetDialog, swipeBtn: SwipeButton) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{ state ->
                    when(state){
                        is UIState.Loading->{
                            ProgressDialogUtil.showLoadingProgress(this@JobDetails,lifecycleScope)
                        }
                        is UIState.Success->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()
                            dialog.dismiss()
                            CommonMethods.getToast(this@JobDetails,"Service Started Successfylly")
                            binding.statusAutocomplete.setText("On-Site")
                            serviceStartAt.value = state.data.onsite_updated_at  //CommonMethods.getCurrentDateTime() //state.data
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()
                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                            swipeBtn.showResultIcon(false, true)
                        }
                        is UIState.Idle->{

                        }

                    }
                }
            }
        }
    }


    private fun showCustomDialog() {
        val parent = dialogView.parent as? ViewGroup
        parent?.removeView(dialogView) // Remove it from its existing parent if present
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        alertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun takeSelfie() {
        if (!CommonMethods.isCameraPermissionGranted(this@JobDetails)) {
            CommonMethods.requestCameraPermission(this@JobDetails)
        } else if (CommonMethods.isCameraPermissionDinead(this@JobDetails)) {
            showCameraPermissionDialog(this@JobDetails)
        } else {
            openCamera()
        }
    }
    fun takeChequeImage() {
        if (!CommonMethods.isCameraPermissionGranted(this@JobDetails)) {
            CommonMethods.requestCameraPermission(this@JobDetails)
        } else if (CommonMethods.isCameraPermissionDinead(this@JobDetails)) {
            showCameraPermissionDialog(this@JobDetails)
        } else {
            openCameraforCheque()
        }
    }

    fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        }
    }
    fun openCameraforCheque() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncherForCheque.launch(intent)
        }
    }

    fun showCameraPermissionDialog(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Camera Permission Required")
            .setMessage("This app requires camera access to take pictures. Please allow camera permission.")
            .setPositiveButton("OK") { _, _ ->
                CommonMethods.openAppSettingsforcamera(this@JobDetails)
            }
            .setCancelable(false)
            .show()
    }


    private fun observeDuration() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeStart.collect { time ->
                    binding.tvDuration.text = time
                }
            }
        }
    }

    private fun observeStartTime() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startTime.collect { time ->
                    binding.tvStartTime.text = time
                }
            }
        }
    }


    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            CommonMethods.getToast(this@JobDetails, "Back is disabled on this screen")
        } else {
            // For API 33+, handled by OnBackInvokedDispatcher
        }
    }



    fun observeJobFinish(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateFinishJob.collect{ state->
                    when(state){
                        is UIState.Idle ->{

                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Loading...","Please wait while we are checking and finishing the job.")
                        }
                        is UIState.Success ->{
                            if (payment_dialog?.isShowing == true){
                                payment_dialog?.dismiss()
                            }
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIJobFinish()
                            showFeedbackBottomsheet(this@JobDetails)
                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIJobFinish()
                            binding.swipebtn.showResultIcon(false, true)
                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                        }
                    }
                }
            }
        }
    }

    fun observeGeneratePaymentQR(paymentboottomsheetui: BottomsheetPaymentBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateGenerateQR.collect{ state->
                    when(state){
                        is UIState.Idle ->{

                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Loading...","Please wait while we are generating QR Code")
                        }
                        is UIState.Success ->{
                            val paymentResponse = state.data

                            val requestOptions = RequestOptions()
                                .placeholder(R.drawable.ic_qr)
                                .error(R.drawable.ic_refer)

                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerInside()

                            paymentboottomsheetui.ivQr.visibility = View.VISIBLE
                            Glide.with(this@JobDetails)
                                .load(paymentResponse.data?.qrImageUrl)
                                .apply(requestOptions)
                                .thumbnail(
                                    Glide.with(this@JobDetails)
                                        .load(paymentResponse.data?.qrImageUrl)
                                        .apply(RequestOptions().override(150, 150).centerInside())
                                )
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .into(paymentboottomsheetui.ivQr)

                            paymentboottomsheetui.clProgress.visibility = View.VISIBLE
                            paymentboottomsheetui.tvGenerateqr.text = "Re-Generate QR"

                            paymentboottomsheetui.ivQr.visibility = View.VISIBLE
                            paymentboottomsheetui.clProgress.visibility = View.VISIBLE

                            paymentResponse.data?.qrImageUrl?.let { viewModel.updateQRImage(it) }

                            viewModel.resetUIStateGeneratePaymentQR()
                            ProgressDialogUtil.dismiss()

                            checkPaymentStatus()

                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIStateGeneratePaymentQR()
                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                        }
                    }
                }
            }
        }
    }

    fun observePaymentStatus(map: HashMap<String, RequestBody>, payment_dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStatePaymentStatus.collect{ state->
                    when(state){
                        is UIState.Idle ->{

                        }
                        is UIState.Loading ->{

                        }
                        is UIState.Success ->{
                            val paymentStatusResponse = state.data

                            if (paymentStatusResponse.status==1){
                                if (paymentStatusResponse.data?.payment_status == 1){
                                    Toast.makeText(this@JobDetails,"Payment Successfully Collected",Toast.LENGTH_SHORT).show()
                                    payment_dialog.dismiss()
                                    stopCheckingPaymentStatus()
                                    viewModel.jobFinish(map)
                                }else{

                                }
                            }else{

                            }

                            viewModel.resetUIStatePaymentStatus()


                        }
                        is UIState.Error ->{
                            viewModel.resetUIStatePaymentStatus()
//                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                        }
                    }
                }
            }
        }
    }

    private fun generateQRCode(text: String): Bitmap {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun checkPaymentStatus() {
        check_paymentStatusJob?.cancel()

        val hashMap = HashMap<String,String>()
        hashMap["order_id"] = jobData.orderId.toString()

        check_paymentStatusJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(10000)
                    viewModel.checkPaymentStatus(hashMap)
                }
            }
        }
    }


    fun stopCheckingPaymentStatus() {
        check_paymentStatusJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckingPaymentStatus()
    }

    // Lambda to pass callback from BottomSheet
    private var cameraResultCallbackForCheque: ((Bitmap) -> Unit)? = null

    private fun launchCameraforCheque(callback: (Bitmap) -> Unit) {
        cameraResultCallbackForCheque = callback
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncherForCheque.launch(intent)
    }

    private fun setupServiceListAdapter(recyclerView: RecyclerView, serviceList: List<ServiceModel>) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = JobDetailsServiceAdapter(serviceList)
        }
    }

}
