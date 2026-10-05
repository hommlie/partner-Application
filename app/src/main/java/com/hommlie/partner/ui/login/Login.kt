package com.hommlie.partner.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.api.ApiException
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityLoginBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.KeyboardUtils
import com.hommlie.partner.utils.ProgressDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Login : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding

    private val viewModel : LoginViewModel by viewModels()

    private var isPhoneHintShowing = false
    private var phoneHintAlreadyShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CommonMethods.setStatusBarColor(this, R.color.white, lightStatusBar = true)

        binding.btnGetotp.setOnClickListener {
            if (CommonMethods.isInternetAvailable(this)){
                val hashMap = HashMap<String, String>()
                hashMap["mobile"] = "+91" + viewModel.enteredMobileNo.value
                hashMap["token"] = viewModel.strToken.value

                viewModel.registerUser(hashMap)
            }else {
                CommonMethods.showConfirmationDialog(this,"Alert!","Please connect to internet",false,false,"Ok"){
                    it.dismiss()
                }
            }
        }
        binding.edtMobileno.setOnTouchListener { view, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val isEmpty = binding.edtMobileno.text.isNullOrBlank()

                if (isEmpty && !phoneHintAlreadyShown && !isPhoneHintShowing) {

                    phoneHintAlreadyShown = true

                    // Prevent keyboard from opening
                    view.clearFocus()

                    KeyboardUtils.hideKeyboard(view)

                    showPhoneNumberHint()

                    return@setOnTouchListener true
                }
            }

            false
        }

        binding.edtMobileno.addTextChangedListener {
            viewModel.onMobileNumberChanged(it.toString())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.enteredMobileNo.collect { mobileNo ->
                    val isMobileNoValdid = mobileNo.length == 10
                    binding.btnGetotp.apply {
                        isEnabled = isMobileNoValdid
                        backgroundTintList = ContextCompat.getColorStateList(
                            this@Login,
                            if (isMobileNoValdid) R.color.color_primary else R.color.disable_btn
                        )
                    }
                    if (isMobileNoValdid) {
                        currentFocus?.let { KeyboardUtils.hideKeyboard(it) }
                    }
                    Log.d("Login", "Mobile No: ${mobileNo.length}")
                }
            }
        }



        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            // no-op
                        }

                        is UIState.Loading -> {
                            // Show loading
                            ProgressDialogUtil.showLoadingProgress(this@Login,lifecycleScope)
                        }

                        is UIState.Success -> {
                            val userData = state.data.data
                            ProgressDialogUtil.dismiss()
                            if (state.data.message.equals("User Not Found", true) ||
                                state.data.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@Login, "Your Account Deactivated\nContact to admin", Toast.LENGTH_SHORT).show()
                                return@collect
                            }
                            val intent = Intent(this@Login,ActOTP::class.java)
                            intent.putExtra("mobileno",viewModel.enteredMobileNo.value)
                            intent.putExtra("strToken",viewModel.strToken.value)
                            startActivity(intent)

                            // Reset state
                            viewModel.resetUIState()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            Toast.makeText(this@Login, state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetUIState()
                        }
                    }
                }
            }
        }

    }


    private val phoneNumberHintLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            isPhoneHintShowing = false

            if (result.resultCode == RESULT_OK) {

                try {

                    val phoneNumber =
                        Identity
                            .getSignInClient(this)
                            .getPhoneNumberFromIntent(result.data)

                    val formattedNumber = phoneNumber
                        .replace("+91", "")
                        .replace(" ", "")
                        .takeLast(10)

                    binding.edtMobileno.setText(formattedNumber)

                    binding.edtMobileno.setSelection(
                        binding.edtMobileno.text.length
                    )

                } catch (e: ApiException) {

                    Log.e(
                        "PhoneNumberHint",
                        "Failed to get phone number",
                        e
                    )
                }
            }
        }

    private fun showPhoneNumberHint() {

        if (isPhoneHintShowing) return

        isPhoneHintShowing = true

        val request =
            GetPhoneNumberHintIntentRequest
                .builder()
                .build()

        Identity
            .getSignInClient(this)
            .getPhoneNumberHintIntent(request)
            .addOnSuccessListener { pendingIntent ->

                try {

                    phoneNumberHintLauncher.launch(
                        IntentSenderRequest.Builder(
                            pendingIntent
                        ).build()
                    )

                } catch (e: Exception) {

                    isPhoneHintShowing = false

                    Log.e(
                        "PhoneNumberHint",
                        "Unable to launch phone number hint",
                        e
                    )
                }
            }
            .addOnFailureListener { exception ->

                isPhoneHintShowing = false

                Log.e(
                    "PhoneNumberHint",
                    "Phone number hint unavailable",
                    exception
                )
            }
    }



}