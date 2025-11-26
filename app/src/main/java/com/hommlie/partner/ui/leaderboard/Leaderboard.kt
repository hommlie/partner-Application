package com.hommlie.partner.ui.leaderboard

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityLeaderboardBinding
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Leaderboard : AppCompatActivity() {
    private lateinit var binding : ActivityLeaderboardBinding

    @Inject
    lateinit var sharePreference: SharePreference
    private val viewModel : LeaderBoardViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = false // or false for light theme
                isAppearanceLightNavigationBars = false
            }

        } else {
            // This is Android 14 or below
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = false // or false for light theme
                isAppearanceLightNavigationBars = false
            }

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Leaderboard", this, R.color.bk_dark,R.color.white)


        observeClicks()


    }


    private fun observeClicks(){
        binding.tvWeekly.setOnClickListener {
            binding.viewWeekly.visibility = View.VISIBLE
            binding.viewMonthly.visibility = View.GONE
            binding.viewYearly.visibility = View.GONE
        }
        binding.tvMonthly.setOnClickListener {
            binding.viewWeekly.visibility = View.GONE
            binding.viewMonthly.visibility = View.VISIBLE
            binding.viewYearly.visibility = View.GONE
        }
        binding.tvYearly.setOnClickListener {
            binding.viewWeekly.visibility = View.GONE
            binding.viewMonthly.visibility = View.GONE
            binding.viewYearly.visibility = View.VISIBLE
        }
    }

}