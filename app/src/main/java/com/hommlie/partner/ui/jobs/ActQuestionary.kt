package com.hommlie.partner.ui.jobs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
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
import com.hommlie.partner.adapter.QuestionAdaptor
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActQuestionaryBinding
import com.hommlie.partner.model.Questions
import com.hommlie.partner.utils.CommonMethods
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
import javax.inject.Inject

@AndroidEntryPoint
class ActQuestionary : AppCompatActivity() {

    private lateinit var binding: ActivityActQuestionaryBinding
    @Inject
    lateinit var sharePreference : SharePreference

    private val viewModel : QuestionViewModel by viewModels()


    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptor: QuestionAdaptor

    private var currentImageView: ImageView? = null

    private var currentQuestionIdForImage: Int? = null

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





        recyclerView=binding.rvQuestion

        orderId=intent.getStringExtra("orderId").toString()
        questionfor=intent.getStringExtra("questionfor").toString()
        orderStatus=intent.getStringExtra("order_status").toString()


        if (orderStatus=="2"){
            orderStatus = "3"
            setupToolbar(toolbarView, "Pre Inspection", this, R.color.transparent, R.color.black)
        }else if (orderStatus=="3"){
            orderStatus = "4"
            setupToolbar(toolbarView, "Post Inspection", this, R.color.transparent, R.color.black)
        }


        hashMap["user_id"]=sharePreference.getString(PrefKeys.userId)
        hashMap["order_status"]=orderStatus
        hashMap["order_id"]=orderId


        viewModel.callApiforQuestions(hashMap,questionfor)

        observeGetQuestion(questionfor)
        observeSubmitQuestionAnwer()


        binding.btnSubmit.setOnClickListener {
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
                        is UIState.Success-> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            val serviceData = state.data.serviceData
                            if (serviceData != null) {
                                val totalquestions = serviceData.orderCount
                                if (totalquestions > 0) {

                                    val questions_data = serviceData.orderQuestions
                                    if (serviceData.orderQuestions == null) {
                                        binding.btnSubmit.visibility = View.GONE

                                    } else {

                                        var onsiteQuestions: List<Questions>? = null
                                        questions_data?.let {
                                            onsiteQuestions =
                                                it.find { question -> question.state == questionfor }?.questions
                                        }
                                        onsiteQuestions?.let { setRecylerView(it) }
                                    }
                                } else {
                                    binding.btnSubmit.visibility = View.GONE
                                    CommonMethods.alertErrorOrValidationDialog(this@ActQuestionary, "No questions found")

                                }
                            }
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()

                            // IF no question found to skipping the current task
                            if (orderStatus=="3"){
                                JobDetails.isonsiteAnswersubmit.value="1"
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
                                JobDetails.isonsiteAnswersubmit.value="1"
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
        adaptor = QuestionAdaptor(this,data)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adaptor
    }



    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.no_animation,R.anim.slide_out)
    }


    fun pickImageForQuestion(questionId: Int, image: ImageView) {
        currentQuestionIdForImage = questionId
        currentImageView = image

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            imagePickerLauncher.launch(cameraIntent)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap ?: return@registerForActivityResult
            currentQuestionIdForImage?.let { id ->
                adaptor.setImageAnswer(id, imageBitmap)
            }
            currentImageView?.setImageBitmap(imageBitmap)
        }
    }


    private fun prepareImagePart(bitmap: Bitmap, name: String): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        val requestFile = bos.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images[]", "$name.jpg", requestFile)
    }

}