package com.hommlie.partner.ui.dashboard

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.FragmentDashboardBinding
import com.hommlie.partner.ui.wallet.WalletViewModel
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.OnNearByServicesClickListener
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Dashboard : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel : WalletViewModel by activityViewModels()

    lateinit var clickListener: OnNearByServicesClickListener

    private var hashmapUserId = HashMap<String,String>()
    private lateinit var indicatorAdapter: BannerIndicatorAdapter
    private var autoScrollJob: Job? = null
    private var progressAnimator: ValueAnimator? = null
    private var isUserSwiping = false

    private lateinit var rewardAdapter: RewardListAdapter


    @Inject
    lateinit var sharePreference : SharePreference


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNearByServicesClickListener) {
            clickListener = context
        } else {
            throw RuntimeException("$context must implement OnNearByServicesClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusBarHeight = CommonMethods.getStatusBarHeight(requireContext())

        val viewStatusBar = binding.viewStatusbar.layoutParams
        viewStatusBar.height = statusBarHeight
        binding.viewStatusbar.layoutParams = viewStatusBar

        hashmapUserId["user_id"] = sharePreference.getString(PrefKeys.userId)

        setupRewardRecyclerView()
        observeCoinData()
        observeGetRewardItems()
        observeClickRedeem()

        setupBanner()

        viewModel.getCoinBalance(hashmapUserId)
        viewModel.getRewardItems()

    }

    override fun onResume() {
        super.onResume()

    }

    private fun observeCoinData(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.coinData.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(requireActivity(),viewLifecycleOwner.lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()

                            val balance = state.data.substringBefore(".").toInt()
                            viewModel.setCoinBalance(balance)
                            binding.tvTotalCoin.text = balance.toString()
                            rewardAdapter.updateUserCoinBalance(balance)


                            viewModel.reset_getCoinBalance()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_getCoinBalance()
                        }
                    }
                }
            }
        }
    }
    private fun observeGetRewardItems(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getRewardItems.collect { state ->
                    when(state){
                        is UIState.Idle -> Unit
                        is UIState.Success -> {
                            rewardAdapter.submitRewardList(state.data)
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_getRewardItem()
                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_getRewardItem()
                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showLoadingProgress(requireActivity(),viewLifecycleOwner.lifecycleScope)
                        }

                    }
                }
            }
        }
    }
    private fun observeClickRedeem(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clickRedeem.collect { state ->
                    when(state){
                        is UIState.Idle -> Unit
                        is UIState.Success -> {

                            CommonMethods.showConfirmationDialog(requireContext(),"Hurrah !",state.data.message,false,false,"OK"){ dialogInterface ->
                                dialogInterface.dismiss()
                                viewModel.getCoinBalance(hashmapUserId)
                                viewModel.getRewardItems()
                            }
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_clickRedeem()
                        }
                        is UIState.Error ->{
                            CommonMethods.showConfirmationDialog(requireContext(),"Alert !",state.message,false,false,"OK"){ dialogInterface ->
                                dialogInterface.dismiss()
                            }
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_clickRedeem()
                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showLoadingProgress(requireActivity(),viewLifecycleOwner.lifecycleScope)
                        }

                    }
                }
            }
        }
    }

    private fun observeWalletData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.walletList.collect { state ->
                    when (state) {
                        is UIState.Loading -> {
                            // show loader
                        }

                        is UIState.Success -> {

                        }

                        is UIState.Error -> {
                            // show error
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    private fun setupBanner() {

        val banners = listOf("https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/66faf3950cda0b7a.png?q=60", "https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/d8fd3693165a6ee9.png?q=60", "https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/1f9c9ad24c2bc37b.jpg?q=60","https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/d8fd3693165a6ee9.png?q=60","https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/d8fd3693165a6ee9.png?q=60","https://rukminim2.flixcart.com/fk-p-flap/3240/540/image/d8fd3693165a6ee9.png?q=60",)

        binding.vpBanners.adapter = BannerAdapter(banners)

        indicatorAdapter = BannerIndicatorAdapter(banners.size)
        binding.rvIndicator.adapter = indicatorAdapter
        binding.rvIndicator.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        binding.vpBanners.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    val realPos = position % indicatorAdapter.itemCount
                    indicatorAdapter.setActive(realPos)

                    // 🔥 reset progress whenever page changes
                    indicatorAdapter.updateProgress(0)
                }



                override fun onPageScrollStateChanged(state: Int) {

                    when (state) {
                        ViewPager2.SCROLL_STATE_DRAGGING -> {
                            // 🔥 user started manual swipe
                            isUserSwiping = true
                            autoScrollJob?.cancel()
                            indicatorAdapter.updateProgress(0)
                        }

                        ViewPager2.SCROLL_STATE_IDLE -> {
                            val realCount = indicatorAdapter.itemCount
                            val current = binding.vpBanners.currentItem

                            // fake last handling
                            if (current == realCount) {
                                binding.vpBanners.setCurrentItem(0, false)
                            }

                            // 🔥 restart auto-scroll fresh
                            isUserSwiping = false
                            restartAutoScroll()
                        }
                    }
                }


            }
        )

        startAutoScroll()
    }
    private fun startAutoScroll() {
        autoScrollJob?.cancel()

        autoScrollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {

                // 🔥 always start progress from 0
                indicatorAdapter.updateProgress(0)

                for (progress in 0..100 step 2) {

                    // ❗ if user swiped, stop immediately
                    if (isUserSwiping) return@launch

                    delay(60)
                    indicatorAdapter.updateProgress(progress)
                }

                // move banner only if auto-scroll
                if (!isUserSwiping) {
                    binding.vpBanners.setCurrentItem(
                        binding.vpBanners.currentItem + 1,
                        true
                    )
                }
            }
        }
    }

    private fun restartAutoScroll() {
        autoScrollJob?.cancel()
        startAutoScroll()
    }
    override fun onStop() {
        super.onStop()
        autoScrollJob?.cancel()
        progressAnimator?.cancel()
    }
    override fun onStart() {
        super.onStart()
        startAutoScroll()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRewardRecyclerView() {
        rewardAdapter = RewardListAdapter{ item ->
            CommonMethods.showConfirmationDialog(requireContext(),"Confirmation !","Are you sure want to redeem this item\n${item.requiredCoin} coins will be debited from your total coins.",false,true,"Yes"){ dialogInterface ->
                dialogInterface.dismiss()
                val hashMap = HashMap<String, String>()
                hashMap["emp_id"] = sharePreference.getString(PrefKeys.userId)
                hashMap["redeemable_item_id"] = item.id.toString()
                viewModel.clickRedeem(hashMap)
            }
        }

        binding.rvRewards.apply {
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rewardAdapter
            setHasFixedSize(false)
            itemAnimator = null
        }
    }
}