package com.hommlie.partner.ui.jobs

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
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
import com.hommlie.partner.model.Questions
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
    private var cameraImageUri: Uri? = null


    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptor: QuestionAdaptor

    private var currentImageView: ImageView? = null

    private var currentQuestionIdForImage: Int? = null
    private var isAttachmentDialogShowing = false

    var orderId:String=""
    var questionfor:String=""
    var orderStatus:String=""
    val hashMap=HashMap<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityActQuestionaryBinding.inflate(layoutInflater)
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
        setupToolbar(toolbarView, "Inspection", this, R.color.transparent, R.color.black)

        onBackPressedDispatcher.addCallback(this){
            finish()
            finishSlideActivity()
        }


        recyclerView = binding.rvQuestion

        orderId = intent.getStringExtra("orderId").toString()
        questionfor = intent.getStringExtra("questionfor").toString()
        orderStatus = intent.getStringExtra("order_status").toString()


        if (orderStatus == "2") {
            orderStatus = "3"
            setupToolbar(toolbarView, "Pre Inspection", this, R.color.transparent, R.color.black)
        } else if (orderStatus == "3") {
            orderStatus = "4"
            setupToolbar(toolbarView, "Post Inspection", this, R.color.transparent, R.color.black)
        }


        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["order_status"] = orderStatus
        hashMap["visit_id"] = orderId


        viewModel.callApiforQuestions(hashMap, questionfor)

        observeGetQuestion(questionfor)
        observeSubmitQuestionAnwer()


        /*binding.btnSubmit.setOnClickListener {
            val answers = adaptor.getAnswers()
            if (answers != null && answers.isNotEmpty()) {
                val gson = Gson()
                val answersJson = gson.toJson(answers)

                hashMap["answers"] = answersJson

                // Handle images
                val imageParts = mutableListOf<MultipartBody.Part>()
                for ((questionId, bitmap) in adaptor.getImageAnswers()) {
                    bitmap?.let {
                        val imagePart = prepareImagePart(bitmap, "question_image_$questionId")
                        imageParts.add(imagePart)
                    }
                }
                val requestMap = hashMap.mapValues {
                    it.value.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                viewModel.submitAnswers(requestMap, imageParts)
            } else {
                CommonMethods.getToast(this@ActQuestionary, "Attempt required questions")
            }
        } */
        binding.btnSubmit.setOnClickListener {

            val services = adaptor.getServiceWiseAnswers()

            if (services.isNotEmpty()) {

                //  FINAL BODY (backend expectation)
                val body = mapOf(
                    "visit_id" to orderId,
                    "order_status" to orderStatus,
                    "user_id" to sharePreference.getString(PrefKeys.userId),
                    "services" to services
                )

                val json = Gson().toJson(body)

                //  POSTMAN / API LOG
                Log.d("POSTMAN_BODY", json)

                // ---------------- IMAGE PART ----------------
                val imageParts = mutableListOf<MultipartBody.Part>()

                Log.d("API_SEND", "------ IMAGE PARAMS ------")
                for ((questionId, bitmaps) in adaptor.getImageAnswers()) {
                    bitmaps.forEachIndexed { index, bitmap ->
                        val imagePart = prepareImagePart(
                            bitmap,
                            "question_${questionId}_image_$index"
                        )
                        imageParts.add(imagePart)

                        Log.d(
                            "API_SEND",
                            "Sending image → questionId=$questionId index=$index"
                        )
                    }
                }

                Log.d("API_SEND", "Total images = ${imageParts.size}")

                //  TEXT BODY AS REQUEST MAP (agar multipart chahiye)
                val requestMap = mapOf(
                    "payload" to json
                ).mapValues {
                    it.value.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                //  FINAL API CALL
                 viewModel.submitAnswers(requestMap, imageParts)

            } else {
                CommonMethods.getToast(
                    this@ActQuestionary,
                    "Attempt required questions"
                )
            }
        }
    }
        private fun observeGetQuestion(questionfor: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{ state->
                    when(state){
                        is UIState.Loading->{
                            ProgressDialogUtil.showLoadingProgress(this@ActQuestionary,lifecycleScope)
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            val serviceData = state.data.serviceData

                            if (serviceData.orderCount > 0 && !serviceData.orderQuestions.isNullOrEmpty()) {

                                val filteredQuestions = mutableListOf<Questions>()

                                serviceData.orderQuestions.forEach { service ->
                                    service.questions
                                        .find { it.state == questionfor }   // 🔥 STATE FILTER
                                        ?.questions
                                        ?.let { filteredQuestions.addAll(it) }
                                }

                                if (filteredQuestions.isNotEmpty()) {
                                    setRecylerView(filteredQuestions)
                                    binding.btnSubmit.visibility = View.VISIBLE
                                } else {
                                    binding.btnSubmit.visibility = View.GONE
                                    CommonMethods.alertErrorOrValidationDialog(
                                        this@ActQuestionary,
                                        "No $questionfor questions found"
                                    )
                                }

                            } else {
                                binding.btnSubmit.visibility = View.GONE
                                CommonMethods.alertErrorOrValidationDialog(
                                    this@ActQuestionary,
                                    "No questions found"
                                )
                            }
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            // IF no question found to skipping the current task
                            if (orderStatus=="3"){
                                JobDetails.isonsiteAnswersubmit.value= 1
                            }
                            if (orderStatus=="4"){
                                JobDetails.isOnCompleteAnswersubmit.value="1"
                            }

                            lifecycleScope.launch {
                                ProgressDialogUtil.showAleartLoadingProgress(this@ActQuestionary,lifecycleScope,"Loading...","")
                                delay(2000)
                                ProgressDialogUtil.dismiss()
                                finish()
                            }

                        }
                        is UIState.Idle->{

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
                            viewModel.resetUISubmitAnswer()
                            if (orderStatus=="3"){
                                JobDetails.isonsiteAnswersubmit.value= 1
                                CommonMethods.getToast(this@ActQuestionary,"Answers submitted successfully.")
                            }
                            if (orderStatus=="4"){
                                JobDetails.isOnCompleteAnswersubmit.value="1"
                                CommonMethods.getToast(this@ActQuestionary,"Answers submitted successfully.")
                            }
                            finish()
                            overridePendingTransition(R.anim.slide_out,R.anim.no_animation)
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUISubmitAnswer()
                        }is UIState.Idle->{

                    }

                    }
                }
            }
        }
    }

    private fun setRecylerView(data: List<Questions>) {
        adaptor = QuestionAdaptor(this) // only context
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adaptor
        adaptor.submitList(data) // submit data here
    }


    fun pickImageForQuestion(questionId: Int, image: ImageView) {
        currentQuestionIdForImage = questionId
        currentImageView = image

        showImageSourceDialog()

//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (cameraIntent.resolveActivity(packageManager) != null) {
//            imagePickerLauncher.launch(cameraIntent)
//        }
    }

//    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            val imageBitmap = result.data?.extras?.get("data") as? Bitmap ?: return@registerForActivityResult
//            currentQuestionIdForImage?.let { id ->
//                adaptor.setImageAnswer(id, imageBitmap)
//            }
//            currentImageView?.setImageBitmap(imageBitmap)
//        }
//    }



    private fun prepareImagePart(bitmap: Bitmap, name: String): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        val requestFile = bos.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images[]", "$name.jpg", requestFile)
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

                currentQuestionIdForImage?.let { id ->

                    adaptor.setImageAnswer(
                        id,
                        imageBitmap
                    )
                }

                currentImageView?.setImageBitmap(
                    imageBitmap
                )

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

                currentQuestionIdForImage?.let { id ->
                    adaptor.setImageAnswer(
                        id,
                        imageBitmap
                    )
                }

                currentImageView?.setImageBitmap(
                    imageBitmap
                )

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
}