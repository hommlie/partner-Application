package com.hommlie.partner.ui.jobs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.animation.AccelerateDecelerateInterpolator
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityJobDetailsBinding
import com.hommlie.partner.databinding.BottomsheetFeetbackBinding
import com.hommlie.partner.databinding.BottomsheetGelPendingBinding
import com.hommlie.partner.databinding.BottomsheetOtpBinding
import com.hommlie.partner.databinding.BottomsheetPaymentBinding
import com.hommlie.partner.databinding.BottomsheetSignatureBinding
import com.hommlie.partner.databinding.BottomsheetreferBinding
import com.hommlie.partner.model.GelServicesData
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.model.RatingOption
import com.hommlie.partner.model.ScheduleGelServiceRequest
import com.hommlie.partner.model.ServiceModel
import com.hommlie.partner.ui.capturephoto.CaptureImage
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.Constants.CHEMICAL_UPDATED
import com.hommlie.partner.utils.Constants.EXTRA_CHEMICALS
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
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
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import kotlin.collections.orEmpty
import kotlin.collections.set

@AndroidEntryPoint
class JobDetails : AppCompatActivity() {

    private lateinit var binding : ActivityJobDetailsBinding

    private val viewModel : JobDetailsViewModel by viewModels()
    private var payment_dialog : Dialog?=null
    private var referral_bottomsheet : Dialog?=null

    lateinit var signatureView: SignatureView
    var selectedRating = 0

    @Inject
    lateinit var sharePreference: SharePreference

    private var check_paymentStatusJob: Job? = null

    private lateinit var jobData: NewOrderData

    private lateinit var dialogView: View
    private lateinit var btnSubmit: Button
    private lateinit var alertDialog: AlertDialog
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncherForCheque : ActivityResultLauncher<Intent>

    private var selectedService: ServiceModel? = null
    private var selectedServicePosition: Int = -1
    private lateinit var uploadJobCardAdapter: UploadJobCardAdapter
    private lateinit var gelServiceAdapter : GelServiceAdapter
    private lateinit var jobCardImageUri: Uri
    private var pendingJobCardImageUri: Uri? = null

    val hashMap =  HashMap<String,String>()

    var isComeFromHome : Int = 0
    private lateinit var selfieImageUri: Uri

    // One entry per rating option, in order 1..5
    private lateinit var ratingOptions: List<RatingOption>
    private var selectedIndex: Int = -1   // -1 = nothing selected yet

    private var feedbackPhotoUri: Uri? = null

    private var feedbackBottomSheetBinding: BottomsheetFeetbackBinding? = null

    companion object{
        var isonsiteAnswersubmit = MutableLiveData<Int?>()
        var isOnCompleteAnswersubmit = MutableLiveData<String>()
        var isonCompleteChemicalFilled = MutableLiveData<String>()
        var OnSiteQuestions = ""
        var OnCompletedQuestions = ""
        var imagewhenStart : Bitmap?=null
        var chequeImage : Bitmap ?= null
        var serviceStartAt : MutableLiveData<String?> = MutableLiveData()
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

        isonsiteAnswersubmit.value = 0
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
        binding.tvType.text="Visit ID :- "+jobData.orderId

        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["visit_id"] = jobData.orderId.toString()

        Glide.with(this@JobDetails).load(jobData.emp_onsite_image).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivCaptureImagebeforejobstart)

        if (jobData.orderStatus=="3"){
            serviceStartAt.value = jobData.onsite_updated_at
        }else if(jobData.orderStatus =="2"){
            serviceStartAt.value = "yet to start"
            viewModel.sentOnsiteotp(hashMap)
            showOTPBottomsheet(this)
        }

        isonsiteAnswersubmit.value =  jobData.IsOniteQuestionsSubmitted
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
                    Glide.with(this)
                        .load(it)
                        .placeholder(R.drawable.ic_placeholder_profile)
                        .error(R.drawable.ic_placeholder_profile)
                        .into(binding.ivCaptureImagebeforejobstart)
//                    binding.ivCaptureImagebeforejobstart.setImageBitmap(it)
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
        observeCheckGelService()
        observeCheckVisitChemicals()
        observeReferal()

        isonsiteAnswersubmit.observe(this) { data ->
            if (data == 1){
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
            binding.root.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    takeSelfie()
                }
            }, 300)
        }

//        orderLastUpdated_at = intent.getStringExtra("updated_at").toString()

        observSendOTP()

        viewModel.updateOrderStatus(jobData.orderStatus.toString())

        viewModel.orderStatus.observe(this) { status ->

            if (viewModel.orderStatus.value == "2"){
//                binding.swipebtn.text = "Start Pre-Inspection"
                binding.mcvSwipebtn.visibility = View.VISIBLE

                if (OnSiteQuestions == "1"){
                    if (isonsiteAnswersubmit.value == 1){
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

                            // showSignatureBottomsheet()
                            viewModel.checkVisitChemical(
                                hashMapOf(
                                    "user_id" to sharePreference.getString(PrefKeys.userId),
                                    "visit_id" to jobData.orderId.toString()
                                )
                            )
                        } else {
                            binding.swipebtn.text = "Start Post-Inspection"
                            binding.swipebtn.showResultIcon(false, true)
                            binding.mcvSwipebtn.visibility = View.VISIBLE
                        }
                    } else {
                        binding.swipebtn.text = "Completed"
                        binding.mcvSwipebtn.visibility = View.GONE

                        // showSignatureBottomsheet()
                        viewModel.checkVisitChemical(
                            hashMapOf(
                                "user_id" to sharePreference.getString(PrefKeys.userId),
                                "visit_id" to jobData.orderId.toString()
                            )
                        )
                    }
                    binding.statusAutocomplete.setText("Complete")
                }else{

                    if (OnSiteQuestions == "1"){
                        if (isonsiteAnswersubmit.value == 1){
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
                if (CommonMethods.isInternetAvailable(applicationContext)) {
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
                       // showSignatureBottomsheet()
                        viewModel.checkVisitChemical(
                            hashMapOf(
                                "user_id" to sharePreference.getString(PrefKeys.userId),
                                "visit_id" to jobData.orderId.toString()
                            )
                        )
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

        binding.tvNotedesc.text="OTP sent to on customer's provided mobile number ${jobData.mobile}"

        /**
         * Send otp to alternate mobile number only for B2B Visits
         */
        if (jobData.orderMode == 1){
            binding.tvClickHere.visibility = View.VISIBLE
            binding.tvClickhereDesc.visibility = View.VISIBLE
        }else{
            binding.tvClickHere.visibility = View.GONE
            binding.tvClickhereDesc.visibility = View.GONE
        }

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

                if (CommonMethods.isInternetAvailable(applicationContext)) {
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
//                            val imagePart = CommonMethods.prepareImagePart("emp_onsite_image", imageBitmap)
                            val imagePart = bitmapToCompressedMultipart(imageBitmap, "emp_onsite_image")
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
        binding.tvClickHere.setOnClickListener {
            if(binding.edtNewNumber.isVisible && binding.btnGetNewOTP.isVisible)return@setOnClickListener
            binding.edtNewNumber.visibility = View.VISIBLE
            binding.btnGetNewOTP.visibility = View.VISIBLE
        }
        binding.edtNewNumber.addTextChangedListener {
            viewModel.onNewMobileNumberChanged(it.toString())
        }

        observeBtnGetNewOTpState(binding)

        binding.btnGetNewOTP.setOnClickListener {
            if (!binding.btnGetNewOTP.isEnabled) return@setOnClickListener
            hashMap["alternate_mobile"] = viewModel.enteredNewMobileNo.value
            binding.tvNotedesc.text="OTP sent to on customer's provided mobile number ${viewModel.enteredNewMobileNo.value}"
            viewModel.sentOnsiteotp(hashMap)
        }


        observeTimer(binding.tvTimer)
        setOtpListeners(binding)
        dialog.setCancelable(false)
        dialog.show()
    }
    private fun observeBtnGetNewOTpState(binding1: BottomsheetOtpBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.enteredNewMobileNo.collect { mobileNo ->
                    val isMobileNoValdid = mobileNo.length == 10
                    binding1.btnGetNewOTP.apply {
                        isEnabled = isMobileNoValdid
                        backgroundTintList = ContextCompat.getColorStateList(
                            this@JobDetails,
                            if (isMobileNoValdid) R.color.color_primary else R.color.disable_btn
                        )
                    }
                    if (isMobileNoValdid) {
                        binding1.edtNewNumber.clearFocus()
                        KeyboardUtils.hideKeyboard(binding1.edtNewNumber)
                    }
                    Log.d("Login", "Mobile No: ${mobileNo.length}")
                }
            }
        }
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
                    "visit_id" to jobData.orderId.toString()
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
                if (!CommonMethods.isInternetAvailable(context)) {
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

        binding.btnConfirmJobcardUploaded.visibility = View.GONE

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
        observeUploadJobCardImage(binding.btnConfirmJobcardUploaded)
        setupUploadJobCardAdapter(binding.rvJobcard)

        binding.signatureView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Jab touch shuru ho, scroll view ko disable kar do
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    // Jab touch khatam ho, scroll view ko wapas enable kar do
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            // Ye line zaroori hai taaki signature view apna drawing logic chala sake
            false
        }
        binding.btnConfirmJobcardUploaded.setOnClickListener {
            binding.llJobcards.visibility = View.GONE
            binding.llSignature.visibility = View.VISIBLE
        }

        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

    fun showReferalBottomsheet(context: Activity) {
        val dialog = BottomSheetDialog(context)

        val binding = BottomsheetreferBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        referral_bottomsheet = dialog

        binding.edtMobileno.doOnTextChanged { text, _, _, _ ->
            if (text?.toString()?.trim()?.length == 10) {
                KeyboardUtils.hideKeyboard(binding.edtMobileno)
                binding.edtMobileno.clearFocus()
            }
        }

        binding.tvSkip.setOnClickListener {
            dialog.dismiss()
            if (jobData.orderMode == 0){
                showFeedbackBottomsheet(this@JobDetails)
            }else{
                finish()
                finishSlideActivity()
            }
        }
        binding.mcvSwipebtn.setOnClickListener {
            val selectedId = binding.radioGroup.checkedRadioButtonId

            if (selectedId == -1) {
                Toast.makeText(context, "Please select Yes or No", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val explainProcess = when (selectedId) {
                R.id.rd_yes -> "YES"
                R.id.rd_no -> "NO"
                else -> "NO"
            }
            val hashMap = HashMap<String, String>()
            hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
            hashMap["explain_process"] = explainProcess
            hashMap["customer_mobile"] = binding.edtMobileno.text.toString().trim()
            hashMap["customer_name"] = binding.edtName.text.toString().trim()
            viewModel.submitRefferal(hashMap)
        }
        dialog.setCancelable(false)
        dialog.show()
    }
    fun showFeedbackBottomsheet(context: Activity) {
        val dialog = BottomSheetDialog(context)

        val binding = BottomsheetFeetbackBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        feedbackBottomSheetBinding = binding

        binding.tvSkip.setOnClickListener {
            dialog.dismiss()
            finish()
            finishSlideActivity()
        }
        binding.btnSubmitFeedback.setOnClickListener {
            if (selectedIndex == -1) return@setOnClickListener

            val chosen = ratingOptions[selectedIndex]

            // TODO: replace with your real submit call, e.g.:
            // viewModel.submitCustomerFeedback(orderId = orderId, rating = chosen.ratingValue, key = chosen.ratingKey)

            Toast.makeText(
                this,
                "Feedback submitted: ${chosen.ratingValue} - ${chosen.ratingKey}",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
            finish()
            finishSlideActivity()
        }
        binding.mcvFeedbackPhoto.setOnClickListener {
            takeFeedbackPhoto()
        }
        setupRatingSelector(binding)
        dialog.setCancelable(false)
        dialog.setOnDismissListener {
            feedbackBottomSheetBinding = null
        }
        dialog.show()
    }
    private fun takeFeedbackPhoto() {
        showImageSourceDialogForFeedbackPhoto()
    }
    private fun showImageSourceDialogForFeedbackPhoto() {

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        dialog.dismiss()
                        if (!CommonMethods.isCameraPermissionGranted(this)) {
                            CommonMethods.requestCameraPermission(this)
                        } else if (CommonMethods.isCameraPermissionDinead(this)) {
                            showCameraPermissionDialog(this)
                        } else {
                            openOurCameraForFeedbackPhoto()
                        }
                    }
                    1 -> {
                        dialog.dismiss()
                        openGalleryForFeedBackPhoto()
                    }
                    else -> dialog.dismiss()
                }
            }
            .setOnDismissListener{

            }
            .show()
    }
    private fun openOurCameraForFeedbackPhoto() {

        val intent = Intent(this, CaptureImage::class.java).apply {
            putExtra(
                CaptureImage.EXTRA_CAPTURE_MODE,
                CaptureImage.IS_TAKE_PHOTO
            )
        }
        feedbackPhotoLauncher.launch(intent)
    }
    private val feedbackPhotoLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }

            val uriString =
                result.data?.getStringExtra(
                    CaptureImage.EXTRA_RESULT_URI
                )

            if (uriString.isNullOrEmpty()) {
                Log.e(
                    "Feedback Camera",
                    "CaptureImage returned empty URI"
                )
                return@registerForActivityResult
            }

            try {

                val uri = Uri.parse(uriString)

                feedbackPhotoUri = uri

                feedbackBottomSheetBinding?.let { binding ->
                    showFeedbackPhoto(binding, uri)
                }
                /*
                 * Temporary file was created by CaptureImage.
                 * We don't need it anymore after decoding.
                 */
//                contentResolver.delete(uri, null, null)

            } catch (e: Exception) {

                Log.e(
                    "Feedback Camera",
                    "Failed to read Feedback image",
                    e
                )
            }
        }
    private fun openGalleryForFeedBackPhoto() {
        galleryPickerLauncherForFeedbackPhoto.launch("image/*")
    }

    private val galleryPickerLauncherForFeedbackPhoto =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri ?: return@registerForActivityResult

            try {
                feedbackPhotoUri = uri

                feedbackBottomSheetBinding?.let { binding ->
                    showFeedbackPhoto(binding, uri)
                }

            } catch (e: Exception) {

                Log.e(
                    "Feedback Gallery",
                    "Failed to load Feedback image",
                    e
                )

                Toast.makeText(
                    this,
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private fun showFeedbackPhoto(
        feedbackBinding: BottomsheetFeetbackBinding,
        uri: Uri
    ) {
        if (feedbackBinding.llPlaceHolder.isVisible){
            feedbackBinding.llPlaceHolder.visibility = View.GONE
            feedbackBinding.ivFeedbackImage.visibility = View.VISIBLE
        }
//        feedbackBinding.ivFeedbackImage.setImageBitmap(bitmap)
        Glide.with(this@JobDetails)
            .load(uri)
            .into(feedbackBinding.ivFeedbackImage)
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
//            openCamera()
            openSelfieCamera()
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
//    private fun openSelfieCamera() {
//
//        try {
//            val file = File.createTempFile(
//                "selfie_",
//                ".jpg",
//                cacheDir
//            )
//
//            selfieImageUri = FileProvider.getUriForFile(
//                this,
//                "${packageName}.fileprovider",
//                file
//            )
//
//            selfieCameraLauncher.launch(selfieImageUri)
//
//        } catch (e: Exception) {
//            Log.e(
//                "SelfieCamera",
//                "Unable to launch camera",
//                e
//            )
//
//            Toast.makeText(
//                this,
//                "Unable to open camera",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    private val selfieCameraLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.TakePicture()
//        ) { success ->
//
//            if (success) {
//                try {
//                    val bitmap = contentResolver.openInputStream(selfieImageUri)?.use {
//                        BitmapFactory.decodeStream(it)
//                    }
//
//                    if (bitmap != null) {
//                        imagewhenStart = bitmap
//
//                        Glide.with(this@JobDetails)
//                            .load(bitmap)
//                            .placeholder(R.drawable.ic_placeholder_profile)
//                            .error(R.drawable.ic_placeholder_profile)
//                            .into(binding.ivCaptureImagebeforejobstart)
//
//                    } else {
//                        Log.e(
//                            "SelfieCamera",
//                            "Camera returned success but bitmap is null"
//                        )
//                    }
//
//                } catch (e: Exception) {
//                    Log.e(
//                        "SelfieCamera",
//                        "Failed to read captured image",
//                        e
//                    )
//                }
//            } else {
//                Log.e(
//                    "SelfieCamera",
//                    "Camera capture cancelled/failed"
//                )
//            }
//        }

    private fun openSelfieCamera() {

        val intent = Intent(this, CaptureImage::class.java).apply {
            putExtra(
                CaptureImage.EXTRA_CAPTURE_MODE,
                CaptureImage.MODE_SELFIE
            )
        }
        selfieCaptureLauncher.launch(intent)
    }
    private val selfieCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }

            val uriString =
                result.data?.getStringExtra(
                    CaptureImage.EXTRA_RESULT_URI
                )

            if (uriString.isNullOrEmpty()) {
                Log.e(
                    "SelfieCamera",
                    "CaptureImage returned empty URI"
                )
                return@registerForActivityResult
            }

            try {

                val uri = Uri.parse(uriString)

                val bitmap =
                    contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }

                if (bitmap == null) {

                    Log.e(
                        "SelfieCamera",
                        "Unable to decode selfie image"
                    )

                    return@registerForActivityResult
                }

                imagewhenStart = bitmap

                Glide.with(this@JobDetails)
                    .load(bitmap)
                    .placeholder(
                        R.drawable.ic_placeholder_profile
                    )
                    .error(
                        R.drawable.ic_placeholder_profile
                    )
                    .into(
                        binding.ivCaptureImagebeforejobstart
                    )

                /*
                 * Temporary file was created by CaptureImage.
                 * We don't need it anymore after decoding.
                 */
                contentResolver.delete(uri, null, null)

            } catch (e: Exception) {

                Log.e(
                    "SelfieCamera",
                    "Failed to read captured selfie",
                    e
                )
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
                            ProgressDialogUtil.dismiss()
                            if (payment_dialog?.isShowing == true){
                                payment_dialog?.dismiss()
                            }
                            viewModel.resetUIJobFinish()
                            if (jobData.orderMode == 0 ){
                                viewModel.checkGelService(hashMapOf("visit_id" to jobData.orderId.toString() , "user_id" to sharePreference.getString(PrefKeys.userId)))
                            }else{
                               showReferalBottomsheet(this@JobDetails)
                            }

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
    fun observeReferal(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateReferal.collect{ state->
                    when(state){
                        is UIState.Idle ->{}
                        is UIState.Loading ->{
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Loading...","Please wait while we are checking and finishing the job.")
                        }
                        is UIState.Success ->{
                            ProgressDialogUtil.dismiss()
                            if (referral_bottomsheet?.isShowing == true){
                                referral_bottomsheet?.dismiss()
                            }
                            viewModel.reset_submitRefferal()
                            if (jobData.orderMode == 0) {
                                showFeedbackBottomsheet(this@JobDetails)
                            }else{
                                finish()
                                finishSlideActivity()
                            }
                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_submitRefferal()
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
        hashMap["visit_id"] = jobData.orderId.toString()

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
        stopCheckingPaymentStatus()
        super.onDestroy()
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
    private fun setupUploadJobCardAdapter(recyclerView: RecyclerView) {
        // B2B == 1 || B2C==0

        val temp = when(jobData.orderMode){
            0 ->{
                jobData.services // show all items
            }
            1 ->{
                listOf(jobData.services.first()) // show only one item
            }
            else -> {
                jobData.services
            }
        }

        uploadJobCardAdapter = UploadJobCardAdapter(temp) { service, position ->
            selectedService = service
            selectedServicePosition = position

            // existing permission + camera flow reuse
            takeJobCardPhoto()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.hasFixedSize()
        recyclerView.adapter = uploadJobCardAdapter
    }
    private fun takeJobCardPhoto() {
        showImageSourceDialogForJobCard()
    }
    private fun showImageSourceDialogForJobCard() {

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        dialog.dismiss()
                        if (!CommonMethods.isCameraPermissionGranted(this)) {
                            CommonMethods.requestCameraPermission(this)
                        } else if (CommonMethods.isCameraPermissionDinead(this)) {
                            showCameraPermissionDialog(this)
                        } else {
                            openCameraForJobCard()
                        }
                    }
                    1 -> {
                        dialog.dismiss()
                        openGalleryForJobCard()
                    }
                    else -> dialog.dismiss()
                }
            }
            .setOnDismissListener{

            }
            .show()
    }
    private fun openCameraForJobCard() {
        val file = File.createTempFile("jobcard_", ".jpg", cacheDir)

        jobCardImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        jobCardCameraLauncher.launch(jobCardImageUri)
    }
    private val jobCardCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && selectedService != null) {

                // ✅ sirf temporary rakho
                pendingJobCardImageUri = jobCardImageUri

                // ❌ UI abhi update mat karo
                uploadJobCardPhoto(jobCardImageUri)
            }
        }

    private fun uploadJobCardPhoto(uri: Uri) {

        val compressedFile = compressImageToUnder2MB(
            context = this,
            uri = uri,
            maxSizeMB = 2
        )

        val requestFile = compressedFile
            .asRequestBody("image/jpeg".toMediaTypeOrNull())

        val imagePart = MultipartBody.Part.createFormData(
            "job_card_image",
            compressedFile.name,
            requestFile
        )

        // IDs collect karne ka logic
        val idList = when(jobData.orderMode) {
            0 -> {
                // B2C: Sirf selected service ki ID
                listOf(selectedService!!.id)
            }
            1 -> {
                // B2B: Saari services ki IDs collect karo
                jobData.services.map { it.id }
            }
            else -> listOf(selectedService!!.id)
        }

        val jsonArrayString = Gson().toJson(idList) // Result: [12212] ya [1212,23433,1231]

        viewModel.uploadJobCardPhoto(
            userId = sharePreference.getString(PrefKeys.userId)
                .toRequestBody("text/plain".toMediaTypeOrNull()),
            orderNo = jobData.orderNo
                .toRequestBody("text/plain".toMediaTypeOrNull()),
            srId = jsonArrayString
                .toRequestBody("application/json".toMediaTypeOrNull()),
            profilePhoto = imagePart
        )
    }

    fun compressImageToUnder2MB(
        context: Context,
        uri: Uri,
        maxSizeMB: Int = 2
    ): File {

        val maxBytes = maxSizeMB * 1024 * 1024

        // Decode bitmap safely
        val originalBitmap = BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(uri)
        )

        // Resize (maintain aspect ratio)
        val maxDimension = 1600
        val ratio = minOf(
            maxDimension.toFloat() / originalBitmap.width,
            maxDimension.toFloat() / originalBitmap.height,
            1f
        )

        val resizedBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            (originalBitmap.width * ratio).toInt(),
            (originalBitmap.height * ratio).toInt(),
            true
        )

        var quality = 85
        var compressedFile: File
        var byteSize: Int

        do {
            compressedFile = File.createTempFile("upload_", ".jpg", context.cacheDir)

            val stream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()

            compressedFile.writeBytes(bytes)
            byteSize = bytes.size

            quality -= 5
        } while (byteSize > maxBytes && quality > 40)

        return compressedFile
    }
    fun compressBitmapToUnder2MB(
        bitmap: Bitmap,
        maxSizeMB: Int = 2
    ): ByteArray {

        val maxBytes = maxSizeMB * 1024 * 1024
        var quality = 90
        val stream = ByteArrayOutputStream()

        do {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            quality -= 5
        } while (stream.size() > maxBytes && quality > 40)

        return stream.toByteArray()
    }
    fun bitmapToCompressedMultipart(
        bitmap: Bitmap,
        partName: String
    ): MultipartBody.Part {

        val compressedBytes = compressBitmapToUnder2MB(bitmap, 2)

        val requestBody = compressedBytes.toRequestBody(
            "image/jpeg".toMediaTypeOrNull()
        )

        return MultipartBody.Part.createFormData(
            partName,
            "onsite.jpg",
            requestBody
        )
    }
    private fun openGalleryForJobCard() {
        galleryPickerLauncherForJobCard.launch("image/*")
    }

    private val galleryPickerLauncherForJobCard =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri ?: return@registerForActivityResult

            pendingJobCardImageUri = uri

            uploadJobCardPhoto(uri)
        }



    private fun setupRatingSelector(binding: BottomsheetFeetbackBinding) {

        updateSubmitButtonState(binding.btnSubmitFeedback)
        
        ratingOptions = listOf(
            RatingOption(
                container = binding.item1,
                card = binding.badge1,
                numberText = binding.num1,
                icon = binding.icon1,
                label = binding.label1,
                accentColor = ContextCompat.getColor(this,R.color.rate_rude),
                tintColor = ContextCompat.getColor(this,R.color.rate_rude_tint),
                ratingValue = 1,
                ratingKey = "rude"
            ),
            RatingOption(
                container = binding.item2,
                card = binding.badge2,
                numberText = binding.num2,
                icon = binding.icon2,
                label = binding.label2,
                accentColor = ContextCompat.getColor(this,R.color.rate_uncooperative),
                tintColor = ContextCompat.getColor(this,R.color.rate_uncooperative_tint),
                ratingValue = 2,
                ratingKey = "uncooperative"
            ),
            RatingOption(
                container = binding.item3,
                card = binding.badge3,
                numberText = binding.num3,
                icon = binding.icon3,
                label = binding.label3,
                accentColor = ContextCompat.getColor(this,R.color.rate_okay),
                tintColor = ContextCompat.getColor(this,R.color.rate_okay_tint),
                ratingValue = 3,
                ratingKey = "okay"
            ),
            RatingOption(
                container = binding.item4,
                card = binding.badge4,
                numberText = binding.num4,
                icon = binding.icon4,
                label = binding.label4,
                accentColor = ContextCompat.getColor(this,R.color.rate_friendly),
                tintColor = ContextCompat.getColor(this,R.color.rate_friendly_tint),
                ratingValue = 4,
                ratingKey = "friendly"
            ),
            RatingOption(
                container = binding.item5,
                card = binding.badge5,
                numberText = binding.num5,
                icon = binding.icon5,
                label = binding.label5,
                accentColor = ContextCompat.getColor(this,R.color.rate_cooperative),
                tintColor = ContextCompat.getColor(this,R.color.rate_cooperative_tint),
                ratingValue = 5,
                ratingKey = "cooperative"
            )
        )
        ratingOptions.forEachIndexed { index, option ->
            option.container.setOnClickListener { selectRating(index, binding.btnSubmitFeedback)}
        }
        // Render initial (unselected) state
        renderAll()

    }
    private fun selectRating(index: Int, btnSubmitFeedback: TextView) {
        selectedIndex = index
        renderAll()
        updateSubmitButtonState(btnSubmitFeedback)
    }
    private fun updateSubmitButtonState(btnSubmitFeedback: TextView) {
        val hasSelection = selectedIndex != -1
        btnSubmitFeedback.isEnabled = hasSelection
        btnSubmitFeedback.alpha = if (hasSelection) 1f else 0.5f
    }

    private fun renderAll() {
        ratingOptions.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            if (isSelected) {
                option.card.strokeColor = option.accentColor
                option.card.setCardBackgroundColor(option.tintColor)
                option.numberText.setTextColor(option.accentColor)
                option.label.setTextColor(color(R.color.ink))
                option.card.cardElevation = 6f
            } else {
                option.card.strokeColor = color(R.color.line_default)
                option.card.setCardBackgroundColor(color(R.color.card_white))
                option.numberText.setTextColor(color(R.color.muted))
                option.label.setTextColor(color(R.color.muted))
                option.card.cardElevation = 0f
            }
        }
    }
    private fun color(resId: Int): Int = ContextCompat.getColor(this, resId)

    private fun moveSelector(selector: View, target: View) {
        // Match selector width to clicked emoji
        selector.layoutParams = selector.layoutParams.apply {
            width = target.width
        }
        selector.requestLayout()

        // Animate X using translation (better than raw x)
        selector.animate()
            .translationX(target.left.toFloat())
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun observeUploadJobCardImage(btnConfirmJobcardUploaded: TextView) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadJobCardPhoto.collect { state ->
                    when(state){

                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(
                                this@JobDetails,
                                lifecycleScope
                            )
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()

                            // ✅ API SUCCESS → ab image dikhao
                            selectedService?.localImageUri = pendingJobCardImageUri
                            uploadJobCardAdapter.notifyItemChanged(selectedServicePosition)

                            if (uploadJobCardAdapter.isEverythingUploaded()) {
                                // Agar sab ho gaya toh button dikhao
                                btnConfirmJobcardUploaded.visibility = View.VISIBLE
                            }

                            // cleanup
                            pendingJobCardImageUri = null
                            viewModel.reset_uploadJobCardPhoto()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()

                            // ❌ API FAIL → image nahi dikhegi
                            pendingJobCardImageUri = null

                            Toast.makeText(
                                this@JobDetails,
                                "Image upload failed. Please upload again.",
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.reset_uploadJobCardPhoto()
                        }

                        is UIState.Idle -> Unit
                    }
                }
            }
        }
    }


    fun showGelPendingServiceBottomSheet(context: Activity, gelServiceData: GelServicesData): Dialog {

        Log.e(
            "GEL_TEST",
            "BottomSheet Size = ${gelServiceData.services?.size}"
        )

        gelServiceData.services?.forEach {
            Log.e(
                "GEL_TEST",
                "BottomSheet Service = ${it.productName}"
            )
        }

        val dialog = BottomSheetDialog(context)
        val binding = BottomsheetGelPendingBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        setupGelServiceRecyclerViewAdapter(binding.rvServices,gelServiceData)

        binding.tvSkip.setOnClickListener {
            dialog.dismiss()
            showReferalBottomsheet(this@JobDetails)
        }
        binding.mcvBtnScheduleGel.setOnClickListener {
            val request = ScheduleGelServiceRequest(
                services = gelServiceAdapter.getScheduleRequest()
            )
            if (request.services.size != gelServiceData.services.orEmpty().size){
                CommonMethods.alertErrorOrValidationDialog(this@JobDetails,"Please select the date and time for all services")
            }else{
                observeScheduleGelService(dialog)
                viewModel.scheduleGelService(request)
            }

        }

        dialog.setCancelable(false)
        dialog.setOnShowListener {

            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.layoutParams?.height =
                ViewGroup.LayoutParams.MATCH_PARENT

            bottomSheet?.requestLayout()

            val behavior = BottomSheetBehavior.from(bottomSheet!!)

            behavior.peekHeight =
                Resources.getSystem().displayMetrics.heightPixels

            behavior.state =
                BottomSheetBehavior.STATE_EXPANDED

            behavior.skipCollapsed = true
        }
        dialog.show()
        return dialog
    }
    private fun setupGelServiceRecyclerViewAdapter(recyclerView: RecyclerView, serviceList: GelServicesData) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            gelServiceAdapter = GelServiceAdapter(serviceList.services.orEmpty() , serviceList.timeslots.orEmpty())
            adapter = gelServiceAdapter
            isNestedScrollingEnabled = false
        }
    }


    private fun observeCheckGelService() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiStateCheckGelService.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Please wait!...","Please wait we are checking gel service")
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val data = state.data
                            Log.e(
                                "GEL_TEST",
                                "Observer Size = ${data.services?.size}"
                            )

                            data.services?.forEach {
                                Log.e(
                                    "GEL_TEST",
                                    "Observer Service = ${it.productName}"
                                )
                            }
                            showGelPendingServiceBottomSheet(this@JobDetails,data)
                            viewModel.resetUIStateCheckGelService()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            showReferalBottomsheet(this@JobDetails)
//                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                            viewModel.resetUIStateCheckGelService()
                        }
                    }
                }
            }
        }
    }
    private fun observeScheduleGelService(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiStateScheduleGelService.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Please wait!...","Please wait we are scheduling gel service")
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_uiStateScheduleGelService()
                            dialog.dismiss()
                            showReferalBottomsheet(this@JobDetails)
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.alertErrorOrValidationDialog(this@JobDetails,state.message)
                            viewModel.reset_uiStateScheduleGelService()
                        }
                    }
                }
            }
        }
    }


    private fun observeCheckVisitChemicals() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiStateCheckVisitChemical.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@JobDetails,lifecycleScope,"Please wait!...","Please wait we are checking gel service")
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIStateCheckVisitChemical()

                            val json = Gson().toJson(state.data)

                            Intent(this@JobDetails, EntryUsedChemical::class.java).apply {
                                putExtra("visit_id",jobData.orderId.toString())
                                putExtra(EXTRA_CHEMICALS, json)
                            }.also {
                                chemicalLauncher.launch(it)
                            }

                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            showSignatureBottomsheet()
                            viewModel.resetUIStateCheckVisitChemical()
                        }
                    }
                }
            }
        }
    }
    private val chemicalLauncher =

        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

            if (it.resultCode == RESULT_OK) {

                if (it.resultCode == RESULT_OK &&
                    it.data?.getBooleanExtra(CHEMICAL_UPDATED, false) == true
                ) {
                    showSignatureBottomsheet()
                }
            }
        }


}
