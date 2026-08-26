package com.hommlie.partner.ui.home

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityGoogleReviewFeedbackBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GoogleReviewFeedback : AppCompatActivity() {
    private lateinit var binding : ActivityGoogleReviewFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGoogleReviewFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        CommonMethods.setStatusBarColor(this,R.color.ub__transparent,false)

        val statusBarHeight = CommonMethods.getStatusBarHeight(this)
        val layoutParams = binding.viewStatusBar.layoutParams
        layoutParams.height = statusBarHeight
        binding.viewStatusBar.layoutParams = layoutParams

        binding.ivBack.setOnClickListener {
            finish()
            finishSlideActivity()
        }
        onBackPressedDispatcher.addCallback(this){
            finish()
            finishSlideActivity()
        }
    }

}