package com.hommlie.partner.ui.registration

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActKycPendingBinding
import com.hommlie.partner.repository.AuthRepository
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActKycPending : AppCompatActivity() {

    private lateinit var  binding : ActivityActKycPendingBinding

    private val viewModel : KYCViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActKycPendingBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true //  false for light theme
                isAppearanceLightNavigationBars = true
            }
        } else {
            // This is Android 14 or below
        }

        observeStatus()


        binding.btnContactUs.setOnClickListener {
            CommonMethods.openDialPad(this@ActKycPending,sharePreference.getString(PrefKeys.contact_us))
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this@ActKycPending, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }


    }

    private fun observeStatus(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{ state ->
                    when(state){
                        is UIState.Loading->{
                            ProgressDialogUtil.showAleartLoadingProgress(this@ActKycPending,lifecycleScope,"Loading...","Please wait while we are checking your current profile status.")
                        }
                        is UIState.Success->{
                            viewModel.resetUIState()
                            ProgressDialogUtil.dismiss()

                            if (state.data.status ==1){
                                sharePreference.setInt(PrefKeys.is_verified,1)
//                                sharePreference.setInt(PrefKeys.is_reg_form_sub,1)
//                                sharePreference.setString(PrefKeys.userId, data.id.toString())
//                                sharePreference.setString(PrefKeys.emp_code,data.empId ?: "")
//                                sharePreference.setString(PrefKeys.userName, data.empName ?: "")
//                                sharePreference.setString(PrefKeys.userEmail, data.empEmail ?: "")
//                                sharePreference.setString(PrefKeys.userMobile, data.mobile ?: "")
//                                sharePreference.setString(PrefKeys.userProfile, data.empPhoto ?: "")
//                                sharePreference.setString(PrefKeys.userAddress, data.empAddress ?: "")
//                                sharePreference.setString(PrefKeys.userAadhaar, data.aadharNo ?: "")
//                                sharePreference.setString(PrefKeys.userPanNO, data.panNo ?: "")
//                                sharePreference.setString(PrefKeys.Designation,data.designation ?: "Designation")
//                                sharePreference.setString(PrefKeys.BloodGroup, data.blood_group ?: "+-")
//                                sharePreference.setString(PrefKeys.DOB, data.dob ?: "01 Jan 1550")
//                                sharePreference.setInt(PrefKeys.is_reg_form_sub, data.is_reg_form_submit)
//                                sharePreference.setInt(PrefKeys.is_verified, data.is_verified)

                                val intent = Intent(this@ActKycPending, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)

                            }else{
//                                CommonMethods.alertErrorOrValidationDialog(this@ActKycPending,"Please wait your profile is under review.")
                            }
                        }
                        is UIState.Error->{
                            viewModel.resetUIState()
                            ProgressDialogUtil.dismiss()
//                            CommonMethods.alertErrorOrValidationDialog(this@ActKycPending,"Please wait your profile is under review.")
                        }
                        is UIState.Idle->{

                        }
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        val hashMap = HashMap<String,String>()
        hashMap["user_id"] =  sharePreference.getString(PrefKeys.userId)
        viewModel.checkStatus(hashMap)
    }

}