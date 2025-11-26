package com.hommlie.partner.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.FragmentSettingBinding
import com.hommlie.partner.ui.addwork.ActAddWork
import com.hommlie.partner.ui.advance.Advance
import com.hommlie.partner.ui.chemical.ActMyChemical
import com.hommlie.partner.ui.chemical.ActNewChemical
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.ui.profile.AboutMe
import com.hommlie.partner.ui.profile.PaySlip
import com.hommlie.partner.ui.profile.ProfileDetails
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.OnNearByServicesClickListener
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Setting : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    lateinit var clickListener: OnNearByServicesClickListener

    @Inject
    lateinit var sharePreference : SharePreference

    private val viewModel: SettingViewModel by viewModels()

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
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusBarHeight = CommonMethods.getStatusBarHeight(requireContext())

        val viewStatusBar = binding.viewStatusbar.layoutParams
        viewStatusBar.height = statusBarHeight //
        binding.viewStatusbar.layoutParams = viewStatusBar

        binding.ivBack.setOnClickListener {
            clickListener.activateHome()
        }

        binding.clLogut.setOnClickListener {
            CommonMethods.showConfirmationDialog(
                requireActivity(),
                "Confirmation !...",
                "Are you sure want to logout from this account?",
                false,
                true
            ) { dialog ->
                dialog.dismiss()
                viewModel.logout(requireContext())
                val intent = Intent(requireContext(), Login::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }


        binding.clTotaldistance.setOnClickListener {
            val intent = Intent(requireContext(), IDCard::class.java)
            startActivity(intent)
        }

        binding.clAddwork.setOnClickListener {
            val intent = Intent(requireContext(), ActAddWork::class.java)
            startActivity(intent)
        }

        binding.clChemicalshave.setOnClickListener {
            val intent = Intent(requireContext(), ActMyChemical::class.java)
            startActivity(intent)
        }

        binding.clAdvance.setOnClickListener {
            val intent = Intent(requireContext(), Advance::class.java)
            startActivity(intent)
        }

        binding.clNewChemicals.setOnClickListener {
            val intent = Intent(requireContext(), ActNewChemical::class.java)
            startActivity(intent)
        }

        binding.card12.setOnClickListener {
            val intent = Intent(requireContext(), ProfileDetails::class.java)
            startActivity(intent)
        }

        binding.clVendorDetails.setOnClickListener {
            val intent = Intent(requireContext(), AboutMe::class.java)
            startActivity(intent)
        }

        binding.clSalaryslip.setOnClickListener {
            val intent = Intent(requireContext(), PaySlip::class.java)
            startActivity(intent)
        }

        binding.clPrivacypolicy.setOnClickListener {
            val intent = Intent(requireContext(), PrivacyPolicy::class.java)
            intent.putExtra("Type", "Policy")
            startActivity(intent)
        }

        binding.clAboutus.setOnClickListener {
            val intent = Intent(requireContext(), PrivacyPolicy::class.java)
            intent.putExtra("Type", "About")
            startActivity(intent)
        }

        binding.clHelp.setOnClickListener {
            val intent = Intent(requireContext(), PrivacyPolicy::class.java)
            intent.putExtra("Type", "About")
            startActivity(intent)
        }

        binding.clDeleteUser.setOnClickListener {
            val hashmap = HashMap<String, String>()
            hashmap["user_id"] = sharePreference.getString(PrefKeys.userId) ?: ""
            alertDeleteAccountDialog(hashmap)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userInfo.collect { user ->
                    user?.let {
//                        binding.tvUsername.text = it.name
                        binding.tvUsername.text = it.name?.replace(",", "")
                        binding.tvEmail.text = it.email
                        Glide.with(requireActivity()).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(
                            R.drawable.ic_placeholder_profile).into(binding.ivProfile)
                        if (it.isGuest) {
                            binding.tvMobile.visibility = View.INVISIBLE
                        } else {
                            binding.tvMobile.visibility = View.VISIBLE
                            binding.tvMobile.text = it.mobile
                        }
                    }
                }
            }
        }

        observeDeleteAccount()

    }



    private fun alertDeleteAccountDialog(hasmap: HashMap<String, String>) {
        CommonMethods.showConfirmationDialog(
            requireActivity(),
            "Confirmation !...",
            "Are you sure want to delete your account?",
            false,
            true
        ) { dialog ->
            dialog.dismiss()
            viewModel.deleteAccount(hasmap)
        }
    }



    private fun observeDeleteAccount() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteAccountState.collect { state ->
                    when (state) {
                        is UIState.Idle -> Unit
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(requireActivity(),viewLifecycleOwner.lifecycleScope)
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val response = state.data
                            if (response.status == 1) {
                                CommonMethods.alertErrorOrValidationDialog(
                                    requireActivity(),
                                    response.message ?: "Account deleted successfully"
                                )
                                viewModel.logout(requireContext())
                                val intent = Intent(requireActivity(),Login::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                                requireActivity().finishAffinity()
                            } else {
                                CommonMethods.alertErrorOrValidationDialog(
                                    requireActivity(),
                                    response.message ?: "Something went wrong"
                                )

                            }
                            viewModel.resetUIDeleteAccount()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.alertErrorOrValidationDialog(
                                requireActivity(),
                                state.message
                            )
                            viewModel.resetUIDeleteAccount()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}
