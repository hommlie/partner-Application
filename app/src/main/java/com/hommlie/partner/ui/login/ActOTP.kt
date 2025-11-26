package com.hommlie.partner.ui.login

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActOtpBinding
import com.hommlie.partner.ui.registration.ActKycPending
import com.hommlie.partner.ui.registration.ActRegistration
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.KeyboardUtils
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.SmsBroadcastReceiver
import com.hommlie.partner.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActOTP : AppCompatActivity() {

    private lateinit var binding: ActivityActOtpBinding

    @Inject
    lateinit var sharePreference: SharePreference

    companion object {
        val autoOTP: MutableLiveData<String> by lazy { MutableLiveData() }
    }

    private lateinit var smsBroadcastReceiver: SmsBroadcastReceiver

    // Inject your ViewModel using Hilt
    private val viewModel: OtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityActOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mobileno = intent.getStringExtra("mobileno") ?: ""
        viewModel.setMobileNumber(mobileno)

        autoOTP.value = ""
        autoOTP.observe(this@ActOTP) { otpdata ->
            Log.d("OTP Observer", "Observed OTP: $otpdata")

            if (!otpdata.isNullOrEmpty() && otpdata.length == 4) {
                binding.otpDigit1.setText(otpdata[0].toString())
                binding.otpDigit2.setText(otpdata[1].toString())
                binding.otpDigit3.setText(otpdata[2].toString())
                binding.otpDigit4.setText(otpdata[3].toString())

                val hashMap = HashMap<String, String>()
                hashMap["mobile"] = "+91" + viewModel.enteredMobileNo.value
                hashMap["otp"] = otpdata //viewModel.enteredOtp.value

                viewModel.verifyOtp(hashMap)
            } else {
                Log.e("OTP Error", "OTP is invalid or empty: $otpdata")
            }
        }

        lifecycleScope.launch {
            viewModel.enteredMobileNo.collect(){ mobileno ->
                binding.tvMobileno.text = "+91 ${mobileno}"
            }
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        observeTimer()
        setOtpListeners()

        // Start the timer (e.g., when you load OTP screen)
        viewModel.startOtpTimer()


        binding.tvTimer.setOnClickListener {
            viewModel.resendOtp("phoneNumber")
        }

        // Observe entered OTP (optional)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.enteredOtp.collect { otp ->
                    val isOtpValid = otp.length == 4
                    binding.btnVerify.apply {
                        isEnabled = isOtpValid

                        if (isOtpValid){
                            backgroundTintList = ContextCompat.getColorStateList(this@ActOTP,R.color.color_primary)
                            KeyboardUtils.hideKeyboard(this)
                        }else{
                            backgroundTintList = ContextCompat.getColorStateList(this@ActOTP,R.color.disable_btn)
//                            binding.btnVerify.alpha = if (isOtpValid) 1f else 0.6f
                        }
                    }
                    Log.d("OTP", "Entered OTP: $otp")
                }
            }
        }

        binding.btnVerify.setOnClickListener {
            val hashMap = HashMap<String, String>()
            hashMap["mobile"] = "+91" + viewModel.enteredMobileNo.value
            hashMap["otp"] = viewModel.enteredOtp.value

            viewModel.verifyOtp(hashMap)
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    try {
                        when (state) {
                            is UIState.Idle -> {
                                // Do nothing
                            }

                            is UIState.Loading -> {
                                ProgressDialogUtil.showLoadingProgress(this@ActOTP, lifecycleScope)
                            }

                            is UIState.Success -> {
                                ProgressDialogUtil.dismiss()
                                val response = state.data

                                if (response.status == 1) {
                                    response.data?.let { data ->
                                        // Save user data safely
                                        sharePreference.setBoolean(PrefKeys.IS_LOGGED_IN, true)
                                        sharePreference.setString(PrefKeys.userId, data.id.toString())
                                        sharePreference.setString(PrefKeys.emp_code,data.empId ?: "")
                                        sharePreference.setString(PrefKeys.userName, data.empName ?: "")
                                        sharePreference.setString(PrefKeys.userEmail, data.empEmail ?: "")
                                        sharePreference.setString(PrefKeys.userMobile, data.mobile ?: "")
                                        sharePreference.setString(PrefKeys.userProfile, data.empPhoto ?: "")
                                        sharePreference.setString(PrefKeys.userAddress, data.empAddress ?: "")
                                        sharePreference.setString(PrefKeys.userAadhaar, data.aadharNo ?: "")
                                        sharePreference.setString(PrefKeys.userPanNO, data.panNo ?: "")
                                        sharePreference.setString(PrefKeys.Designation,data.designation ?: "Designation")
                                        sharePreference.setString(PrefKeys.BloodGroup, data.blood_group ?: "")
                                        sharePreference.setString(PrefKeys.DOB, data.dob ?: "01 Jan 1550")
                                        sharePreference.setInt(PrefKeys.is_reg_form_sub, data.is_reg_form_submit)
                                        sharePreference.setInt(PrefKeys.is_verified, data.is_verified)

                                        if (data.is_reg_form_submit == 1){
                                            if (data.is_verified == 1) {
                                                // Navigate to MainActivity
                                                val intent = Intent(this@ActOTP, MainActivity::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                }
                                                startActivity(intent)
                                                finish()
                                            }else{
                                                val intent = Intent(this@ActOTP,ActKycPending::class.java)
                                                startActivity(intent)
                                            }
                                        }else {
                                            val intent = Intent(this@ActOTP,ActRegistration::class.java)
                                            startActivity(intent)
                                        }

                                    } ?: run {
                                        // Null data from server
                                        Toast.makeText(this@ActOTP, "User data not found!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Status failed
                                    Toast.makeText(this@ActOTP, response.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                                }

                                viewModel.resetUIState()
                            }

                            is UIState.Error -> {
                                ProgressDialogUtil.dismiss()
                                Toast.makeText(this@ActOTP, state.message ?: "Unexpected error occurred", Toast.LENGTH_SHORT).show()
                                viewModel.resetUIState()
                            }
                        }
                    } catch (e: Exception) {
                        ProgressDialogUtil.dismiss()
                        viewModel.resetUIState()
                        Toast.makeText(this@ActOTP, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
        }



    }

    private fun observeTimer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect { time ->
                    binding.tvTimer.text = time
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.canResend.collect { canResend ->
                    binding.tvTimer.isEnabled = canResend
                }
            }
        }
    }



    private fun setOtpListeners() {
        val otpFields = listOf(
            binding.otpDigit1,
            binding.otpDigit2,
            binding.otpDigit3,
            binding.otpDigit4
        )

        for (i in otpFields.indices) {
            val current = otpFields[i]
            val next = otpFields.getOrNull(i + 1)
            val prev = otpFields.getOrNull(i - 1)

            current.addTextChangedListener(GenericTextWatcher(current, next, prev))
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
        private val previousView: View?
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
                binding.otpDigit4.text.toString()
            ).joinToString("")
            viewModel.updateOtp(otp)
        }
    }

    override fun onResume() {
        super.onResume()
        startSmsRetriever()

        smsBroadcastReceiver = SmsBroadcastReceiver()

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsBroadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(smsBroadcastReceiver, intentFilter)
        }
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(smsBroadcastReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        onBackPressedDispatcher.onBackPressed()
    }

    fun startSmsRetriever() {
        val client = SmsRetriever.getClient(this)
        Toast.makeText(this@ActOTP,"Auto reading otp",Toast.LENGTH_SHORT).show()
        //  Stop any existing listener before starting a new one
        client.startSmsUserConsent(null)

        val task = client.startSmsRetriever()
        task.addOnSuccessListener {
            Log.d("OTP", "✅ SMS Retriever API started successfully")
        }.addOnFailureListener {
            Log.e("OTP", "❌ Failed to start SMS Retriever API", it)
        }
    }

}
