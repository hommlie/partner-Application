package com.hommlie.partner.ui.leaderboard

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityLeaderboardBinding
import com.hommlie.partner.model.LeaderBoardData
import com.hommlie.partner.utils.CommonMethods.formatCoins
import com.hommlie.partner.utils.CommonMethods.toCapwords
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Leaderboard : AppCompatActivity() {
    private lateinit var binding : ActivityLeaderboardBinding
    private lateinit var adapter: LeaderBoardAdapter

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
        setupToolbar(toolbarView, "Leaderboard", this, R.color.bk_dark,R.color.white,R.color.color_536724)


        observeClicks()
        observeLeaderBoardResposne()
        setUpLeaderBoardRecylerView()
    }

    private fun observeLeaderBoardResposne() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getLeaderboardUiState.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@Leaderboard,lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val restresponse = state.data

                            setUIData(restresponse.leaderboard)

                            viewModel.reset_getLeaderboardUiState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_getLeaderboardUiState()
                        }
                    }
                }
            }
        }
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

    private fun setUpLeaderBoardRecylerView(){
        adapter = LeaderBoardAdapter()
        binding.recylerView.apply {
            layoutManager = LinearLayoutManager(this@Leaderboard)
            setHasFixedSize(true)
            adapter = this@Leaderboard.adapter
        }
    }
    private fun setUIData(leaderboard: List<LeaderBoardData>?) {

        if (leaderboard.isNullOrEmpty()) {
            adapter.submitList(emptyList())
            return
        }

        // --- Top 3 users ---
        val first = leaderboard.getOrNull(0)
        val second = leaderboard.getOrNull(1)
        val third = leaderboard.getOrNull(2)

        bindTopUser(first, binding.tvFirstname, binding.tvFirstpoint,binding.ivFirstProfile)
        bindTopUser(second, binding.tvSecondname, binding.tvSecondpoint,binding.ivSecondProfile)
        bindTopUser(third, binding.tvThirdname, binding.tvThirdpoint,binding.ivThirdProfile)

        // --- RecyclerView logic ---
        val remainingList =
            if (leaderboard.size > 3) {
                leaderboard.subList(3, leaderboard.size) // 4th to last
            } else {
                emptyList() // agar sirf 3 ya kam hai
            }

        adapter.submitList(remainingList)
    }


    private fun bindTopUser(
        user: LeaderBoardData?,
        nameView: TextView,
        pointView: TextView,
        profile : ImageView
    ) {
        if (user == null) {
            nameView.text = "-"
            pointView.text = "0 pts"
            return
        }

        nameView.text = user.emp_name?.substringBefore(",")?.toCapwords() ?: "-"
        pointView.text = "${user.total_coins?.formatCoins() ?: "0"} pts"
        Glide.with(this@Leaderboard).load(user.profile).placeholder(R.drawable.ic_placeholder_profile).into(profile)
    }

}