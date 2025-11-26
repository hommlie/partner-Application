package com.hommlie.partner.ui.profile

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityProfileDetailsBinding
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileDetails : AppCompatActivity() {

    private lateinit var binding : ActivityProfileDetailsBinding

    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }

        } else {
            // This is Android 14 or below

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Profile Details", this, R.color.transparent, R.color.black)

        binding.llBasicdetails.setOnClickListener {
            binding.clBasicdetails.visibility = View.VISIBLE
            binding.clAttribute.visibility = View.GONE
        }
        binding.llAttributedetails.setOnClickListener {
            binding.clBasicdetails.visibility = View.GONE
            binding.clAttribute.visibility = View.VISIBLE
        }


        binding.tvEmpcode.text = sharePreference.getString(PrefKeys.emp_code)
        binding.tvEmpname.text = sharePreference.getString(PrefKeys.userName)
        binding.tvEmpemail.text = sharePreference.getString(PrefKeys.userEmail)

        var mobile = sharePreference.getString(PrefKeys.userMobile) ?: ""

        if (mobile.startsWith("+91") && !mobile.startsWith("+91 ")) {
            mobile = mobile.replaceFirst("+91", "+91 ")
        }
        binding.tvEmpmobile.text = mobile


    }
}