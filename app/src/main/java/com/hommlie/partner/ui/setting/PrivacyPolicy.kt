package com.hommlie.partner.ui.setting

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.ApiClient
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityPrivacyPolicyBinding
import com.hommlie.partner.model.CmsPageResponse
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.ProgressDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
class PrivacyPolicy : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding
    private val viewModel: PrivacyPolicyViewModel by viewModels()

    private var type: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeCmsData()
        viewModel.fetchCmsData()
    }

    private fun setupUI() {
        type = intent.getStringExtra("Type")
        binding.tvTitle.text = type

        binding.ivBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out)
        }
    }

    private fun observeCmsData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cmsDataState.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@PrivacyPolicy, lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            showCmsContent(state.data)
                            viewModel.resetUICMSDataState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUICMSDataState()
                            CommonMethods.alertErrorOrValidationDialog(this@PrivacyPolicy, state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showCmsContent(response: CmsPageResponse) {
        if (response.status != 1) {
            CommonMethods.alertErrorOrValidationDialog(this, response.message ?: "Something went wrong")
            return
        }

        val content = when (type) {
            "Policy" -> response.privacypolicy
            "About" -> response.about
            "Terms Condition" -> response.termsconditions
            else -> ""
        }

        binding.tvCmsData.text = if (content.isNullOrEmpty()) {
            ""
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
            else
                Html.fromHtml(content)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.tvCmsData.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out)
    }
}
