package com.hommlie.partner.ui.setting

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityIdcardBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toCapwords
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IDCard : AppCompatActivity() {

    private lateinit var binding : ActivityIdcardBinding
    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIdcardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Virtual ID Card", this, R.color.white, R.color.black)


        binding.apply {
            tvName.text = sharePreference.getString(PrefKeys.userName).replace(",", "").toCapwords()
            tvEmpcode.text = sharePreference.getString(PrefKeys.emp_code)
            tvPhone.text = sharePreference.getString(PrefKeys.userMobile).replace("+91", "+91 ")
            tvDesignation.text = sharePreference.getString(PrefKeys.Designation, "").takeIf { !it.isNullOrEmpty() } ?: "Designation"
            binding.tvBloodgroup.text = sharePreference.getString(PrefKeys.BloodGroup)
            binding.tvdob.text = CommonMethods.formatDateToReadable(sharePreference.getString(PrefKeys.DOB))
        }

        Glide.with(this)
            .load(sharePreference.getString(PrefKeys.userProfile))
            .placeholder(R.drawable.ic_dummy_profile)
            .into(binding.ivProfile)

    }

}