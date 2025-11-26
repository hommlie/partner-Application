package com.hommlie.partner.ui.advance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityAddAdvanceRequestBinding
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.colorAsterisk
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddAdvanceRequest : AppCompatActivity() {

    private lateinit var binding : ActivityAddAdvanceRequestBinding

    private val viewModel : AdvanceViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference
    private var hashmap = HashMap<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddAdvanceRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Request Advance", this, R.color.ub__transparent, R.color.black)

        binding.tv2.colorAsterisk(R.color.purple)
        binding.tvReason.colorAsterisk(R.color.purple)

        hashmap["user_id"] = sharePreference.getString(PrefKeys.userId)

        observeAddAdavce()

        binding.btnAdd.setOnClickListener {
            val amount = binding.edtAmount.text.toString().trim()
            val reason = binding.edtReason.text.toString().trim()
            val requestDate = CommonMethods.getCurrentDateFormatted()

            when {
                amount.isEmpty() -> {
                    CommonMethods.showErrorFullMsg(
                        this@AddAdvanceRequest,
                        "Please enter an amount"
                    )
                }
                reason.isEmpty() -> {
                    CommonMethods.showErrorFullMsg(
                        this@AddAdvanceRequest,
                        "Please enter a reason"
                    )
                }
                !CommonMethods.isCheckNetwork(this) -> {
                    CommonMethods.showErrorFullMsg(
                        this@AddAdvanceRequest,
                        getString(R.string.no_internet)
                    )
                }
                else -> {
                    hashmap.apply {
                        put("requested_amount", amount)
                        put("reason", reason)
                        put("request_date", requestDate)
                    }

                    viewModel.addAdvanceRequest(hashmap)
                }
            }
        }


    }

    private fun observeAddAdavce(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addAdvanceState.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@AddAdvanceRequest,lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetAddAdvanceState()

                            if (state.data.status==1) {
                                CommonMethods.showConfirmationDialog(
                                    this@AddAdvanceRequest,
                                    "Successful !...",
                                    "Your request send successfully.",
                                    false,
                                    false
                                ) { dialog ->
                                    dialog.dismiss()
                                    finish()
                                }
                            }else{
                                CommonMethods.alertErrorOrValidationDialog(this@AddAdvanceRequest,state.data.message)
                            }
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetAddAdvanceState()
                        }
                    }
                }
            }
        }

    }
}