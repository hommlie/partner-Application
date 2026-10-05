package com.hommlie.partner.ui.jobs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.hommlie.partner.adapter.QuestionAdaptor
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActQuestionaryBinding
import com.hommlie.partner.model.DaoCollectAllQuestionsOfAllServices
import com.hommlie.partner.sharedviewmodel.UploadImageViewModel
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ActQuestionary : AppCompatActivity() {

    private lateinit var binding: ActivityActQuestionaryBinding
    @Inject
    lateinit var sharePreference : SharePreference

    private val viewModel : QuestionViewModel by viewModels()
    private val imageViewModel : UploadImageViewModel by viewModels()

    private var cameraImageUri: Uri? = null


    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptor: QuestionAdaptor

    private var currentImageView: ImageView? = null

    private var currentServiceIdForImage: Int? = null
    private var currentQuestionIdForImage: Int? = null
    private var currentImageSlot: Int = 0

    private var isAttachmentDialogShowing = false

    var orderId:String=""
    var questionfor:String=""
    var orderStatus:String=""
    val hashMap=HashMap<String,String>()

    private var inspection_type : Int = 2

    enum class QuestionFor {
        Onsite,
        OnCompleted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActQuestionaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left, systemBars.top, systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
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
        setupToolbar(toolbarView, "Inspection", this, R.color.transparent, R.color.black)

        onBackPressedDispatcher.addCallback(this) {
            finish()
            finishSlideActivity()
        }


        recyclerView = binding.rvQuestion

        orderId = intent.getStringExtra("orderId").toString()
        questionfor = intent.getStringExtra("questionfor").toString()
        orderStatus = intent.getStringExtra("order_status").toString()

        observeSubmitImage()

        setupRecyclerView()

        inspection_type = when (questionfor) {
            QuestionFor.Onsite.name -> {
                0
            }

            QuestionFor.OnCompleted.name -> {
                1
            }

            else -> {
                2
            }
        }


        if (orderStatus == "2") {
            orderStatus = "3"
            setupToolbar(toolbarView, "Pre Inspection", this, R.color.transparent, R.color.black)
        } else if (orderStatus == "3") {
            orderStatus = "4"
            setupToolbar(toolbarView, "Post Inspection", this, R.color.transparent, R.color.black)
        }


        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["visit_id"] = orderId
        hashMap["inspection_type"] = inspection_type.toString()


        viewModel.callApiforQuestions(hashMap)

        observeGetQuestion()
        observeSubmitQuestionAnwer()

        binding.btnSubmit.setOnClickListener {

            // First validate all required questions
            if (!adaptor.validateRequiredQuestions()) {

                CommonMethods.getToast(
                    this@ActQuestionary,
                    "Please attempt all required questions"
                )
                return@setOnClickListener
            }

            val services = adaptor.getServiceWiseAnswers()

            if (services.isNotEmpty()) {

                val payload = hashMapOf<String, Any>(
                    "visit_id" to orderId.toInt(),
                    "inspection_type" to inspection_type,
                    "user_id" to (
                            sharePreference.getString(PrefKeys.userId)?.toIntOrNull()
                                ?: 0
                            ),
                    "services" to services
                )

                val body = hashMapOf<String, Any>(
                    "payload" to payload
                )

                Log.d(
                    "POSTMAN_BODY",
                    Gson().toJson(body)
                )

                viewModel.submitAnswers(body)

            } else {

                CommonMethods.getToast(
                    this@ActQuestionary,
                    "Attempt required questions"
                )
            }
        }
    }

    private fun observeGetQuestion() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(
                                this@ActQuestionary,
                                lifecycleScope
                            )
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            val serviceData = state.data.serviceData

                            if (serviceData.orderCount > 0 && !serviceData.orderQuestions.isNullOrEmpty()) {

                                val filteredQuestions =
                                    mutableListOf<DaoCollectAllQuestionsOfAllServices>()

                                serviceData.orderQuestions.forEach { service ->

                                    service.questions.forEach { question ->

                                        filteredQuestions.add(
                                            DaoCollectAllQuestionsOfAllServices(
                                                service_id = service.service_id,
                                                service_name = service.service_name,
                                                id = question.id,
                                                label = question.label,
                                                type = question.type,
                                                options = question.options,
                                                required = question.required,
                                                status = question.status
                                            )
                                        )
                                    }
                                }

                                if (filteredQuestions.isNotEmpty()) {
                                    adaptor.submitList(filteredQuestions)
                                    binding.btnSubmit.visibility = View.VISIBLE
                                } else {
                                    binding.btnSubmit.visibility = View.GONE
                                    CommonMethods.showConfirmationDialog(
                                        context = this@ActQuestionary,
                                        title = "Error !",
                                        message = "No questions found for these services\n\n    1. Press 'Continue' for next step",
                                        isCancelable = false,
                                        show_no_btn = false,
                                        positiveText = "Continue",
                                        negativeText = "Cancel",
                                        onNegativeClick = {},
                                        onConfirm = {
                                            if (orderStatus == "3") {
                                                JobDetails.isonsiteAnswersubmit.value = 1
                                            }
                                            if (orderStatus == "4") {
                                                JobDetails.isOnCompleteAnswersubmit.value = "1"
                                            }
                                            finish()
                                            finishSlideActivity()
                                        },
                                    )
                                }

                            } else {
                                binding.btnSubmit.visibility = View.GONE

                                CommonMethods.showConfirmationDialog(
                                    context = this@ActQuestionary,
                                    title = "Error !",
                                    message = "No questions found for these services\n\n    1. Press 'Continue' for next step",
                                    isCancelable = false,
                                    show_no_btn = false,
                                    positiveText = "Continue",
                                    negativeText = "Cancel",
                                    onNegativeClick = {},
                                    onConfirm = {
                                        if (orderStatus == "3") {
                                            JobDetails.isonsiteAnswersubmit.value = 1
                                        }
                                        if (orderStatus == "4") {
                                            JobDetails.isOnCompleteAnswersubmit.value = "1"
                                        }
                                        finish()
                                        finishSlideActivity()
                                    },
                                )
                            }
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            // IF no question found to skipping the current task
                            if (orderStatus == "3") {
                                JobDetails.isonsiteAnswersubmit.value = 1
                            }
                            if (orderStatus == "4") {
                                JobDetails.isOnCompleteAnswersubmit.value = "1"
                            }

                            lifecycleScope.launch {
                                ProgressDialogUtil.showAleartLoadingProgress(
                                    this@ActQuestionary,
                                    lifecycleScope,
                                    "Loading...",
                                    ""
                                )
                                delay(2000)
                                ProgressDialogUtil.dismiss()
                                finish()
                            }

                        }

                        is UIState.Idle -> {

                        }

                    }
                }
            }
        }
    }

    private fun observeSubmitQuestionAnwer(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiStateSubmitAnswr.collect{ state->
                    when(state){
                        is UIState.Loading->{
                            ProgressDialogUtil.showLoadingProgress(this@ActQuestionary,lifecycleScope)
                        }
                        is UIState.Success->{
                            ProgressDialogUtil.dismiss()
                            if (orderStatus=="3"){
                                JobDetails.isonsiteAnswersubmit.value= 1
                                CommonMethods.getToast(this@ActQuestionary,"Answers submitted successfully.")
                            }
                            if (orderStatus=="4"){
                                JobDetails.isOnCompleteAnswersubmit.value="1"
                                CommonMethods.getToast(this@ActQuestionary,"Answers submitted successfully.")
                            }
                            viewModel.resetUISubmitAnswer()
                            finish()
                            overridePendingTransition(R.anim.slide_out,R.anim.no_animation)
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            CommonMethods.showConfirmationDialog(
                                context = this@ActQuestionary,
                                title = "Error",
                                message = state.message,
                                isCancelable = false,
                                show_no_btn = false,
                                positiveText = "Ok",
                                onConfirm = {}
                            )
                            viewModel.resetUISubmitAnswer()
                        }is UIState.Idle->{}
                    }
                }
            }
        }
    }
    private fun setupRecyclerView() {

        adaptor = QuestionAdaptor(this)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ActQuestionary)
            adapter = adaptor
            isNestedScrollingEnabled = true
            setItemViewCacheSize(10)
            itemAnimator = null
        }
    }


    fun pickImageForQuestion(
        serviceId: Int,
        questionId: Int,
        imageSlot: Int,
        imageView: ImageView
    ) {
        currentServiceIdForImage = serviceId
        currentQuestionIdForImage = questionId
        currentImageSlot = imageSlot
        currentImageView = imageView

        showImageSourceDialog()
    }


    private fun prepareImagePart(bitmap: Bitmap, name: String): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        val requestFile = bos.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "$name.jpg", requestFile)
    }

    private fun showImageSourceDialog() {
        if (isAttachmentDialogShowing) return

        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        isAttachmentDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        dialog.dismiss()
                        handleCameraPermission()
                    }
                    1 -> {
                        dialog.dismiss()
                        openGallery()
                    }
                    else -> dialog.dismiss()
                }
            }
            .setOnDismissListener{
                isAttachmentDialogShowing = false
            }
            .show()
    }
    private fun handleCameraPermission() {

        when {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {

                openCamera()
            }
            shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            ) -> {
                showCameraPermissionRationale()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                openCamera()
            } else {
                handleCameraPermissionDenied()
            }
        }
    private fun requestCameraPermission() {

        cameraPermissionLauncher.launch(
            Manifest.permission.CAMERA
        )
    }
    private fun handleCameraPermissionDenied() {

        if (
            shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        ) {
            showCameraPermissionRationale()
        } else {
            showCameraPermissionSettingsDialog()
        }
    }
    private fun showCameraPermissionRationale() {

        CommonMethods.showConfirmationDialog(
            context = this,
            title = "Camera Permission Required",
            message = """
            Camera permission is required to take a photo.

            Please allow camera permission to continue.
        """.trimIndent(),
            isCancelable = false,
            show_no_btn = true,
            positiveText = "Allow",
            negativeText = "Cancel",

            onNegativeClick = {
                it.dismiss()
            },

            onConfirm = {
                it.dismiss()
                requestCameraPermission()
            }
        )
    }
    private fun showCameraPermissionSettingsDialog() {

        CommonMethods.showConfirmationDialog(
            context = this,
            title = "Camera Permission Required",
            message = """
            Camera permission is disabled for this app.

            To enable it:
            1. Tap "Open Settings".
            2. Open "Permissions".
            3. Select "Camera".
            4. Choose "Allow" or "Allow only while using the app".
            5. Return to the app and try again.
        """.trimIndent(),
            isCancelable = false,
            show_no_btn = true,
            positiveText = "Open Settings",
            negativeText = "Cancel",

            onNegativeClick = {
                it.dismiss()
            },

            onConfirm = {
                it.dismiss()
                openAppSettings()
            }
        )
    }
    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {

            data = Uri.fromParts(
                "package",
                packageName,
                null
            )
        }
        startActivity(intent)
    }

    private fun openCamera() {

        try {

            val file = File.createTempFile(
                "question_image_",
                ".jpg",
                cacheDir
            )

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)

        } catch (e: Exception) {

            Log.e(
                "ImagePicker",
                "Unable to launch camera",
                e
            )

            Toast.makeText(
                this,
                "Unable to open camera",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (!success) {

                Log.e(
                    "ImagePicker",
                    "Camera capture cancelled/failed"
                )

                return@registerForActivityResult
            }

            val uri = cameraImageUri

            if (uri == null) {

                Log.e(
                    "ImagePicker",
                    "Camera returned success but URI is null"
                )

                return@registerForActivityResult
            }

            try {

                val imageBitmap =
                    contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(
                                inputStream
                            )
                        }

                if (imageBitmap == null) {

                    Log.e(
                        "ImagePicker",
                        "Camera returned success but bitmap is null"
                    )

                    Toast.makeText(
                        this,
                        "Unable to load captured image",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@registerForActivityResult
                }

                val serviceId = currentServiceIdForImage
                val questionId = currentQuestionIdForImage
                val imageSlot = currentImageSlot

                if (serviceId != null && questionId != null) {

                    currentImageView?.setImageBitmap(imageBitmap)

                    uploadQuestionImage(
                        serviceId = serviceId,
                        questionId = questionId,
                        imageSlot = imageSlot,
                        bitmap = imageBitmap
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "ImagePicker",
                    "Failed to read captured image",
                    e
                )

                Toast.makeText(
                    this,
                    "Unable to load captured image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun openGallery() {
        galleryPickerLauncher.launch(
            "image/*"
        )
    }
    private val galleryPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri ?: return@registerForActivityResult

            try {

                val imageBitmap = contentResolver
                    .openInputStream(uri)
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }

                if (imageBitmap == null) {
                    Toast.makeText(
                        this,
                        "Unable to load image",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@registerForActivityResult
                }

                val serviceId = currentServiceIdForImage
                val questionId = currentQuestionIdForImage
                val imageSlot = currentImageSlot

                if (serviceId != null && questionId != null) {

                    currentImageView?.setImageBitmap(imageBitmap)

                    uploadQuestionImage(
                        serviceId = serviceId,
                        questionId = questionId,
                        imageSlot = imageSlot,
                        bitmap = imageBitmap
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "ImagePicker",
                    "Failed to load gallery image",
                    e
                )

                Toast.makeText(
                    this,
                    "Unable to load image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun observeSubmitImage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                imageViewModel.uploadImageUiState.collect { state ->
                    when(state){
                       is UIState.Idle->{}

                        is UIState.Loading->{
                            ProgressDialogUtil.showLoadingProgress(this@ActQuestionary,lifecycleScope)
                        }

                        is UIState.Success-> {
                            ProgressDialogUtil.dismiss()

                            val imageName = state.data.imageName
                            val serviceId = currentServiceIdForImage
                            val questionId = currentQuestionIdForImage

                            if (
                                serviceId != null &&
                                questionId != null &&
                                !imageName.isNullOrBlank()
                            ) {

                                adaptor.setImageAnswer(
                                    serviceId = serviceId,
                                    questionId = questionId,
                                    imageSlot = currentImageSlot,
                                    imageName = imageName
                                )
                            }
                            imageViewModel.resetUploadImageUiState()
                        }

                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            CommonMethods.showConfirmationDialog(
                                context = this@ActQuestionary,
                                title = "Error",
                                message = state.message,
                                isCancelable = false,
                                show_no_btn = false,
                                positiveText = "Ok",
                                onConfirm = {}
                            )
                            imageViewModel.resetUploadImageUiState()
                        }
                    }
                }
            }
        }
    }
    private fun uploadQuestionImage(
        serviceId: Int,
        questionId: Int,
        imageSlot: Int,
        bitmap: Bitmap
    ) {
        currentServiceIdForImage = serviceId
        currentQuestionIdForImage = questionId
        currentImageSlot = imageSlot

        val imagePart = prepareImagePart(
            bitmap = bitmap,
            name = "question_${questionId}_${System.currentTimeMillis()}"
        )

        imageViewModel.uploadImage(imagePart)
    }
}