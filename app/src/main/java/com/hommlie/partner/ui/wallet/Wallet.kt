package com.hommlie.partner.ui.wallet

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityWalletBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Wallet : AppCompatActivity() {
    private lateinit var binding : ActivityWalletBinding

    @Inject
    lateinit var sharePreference: SharePreference
    private lateinit var walletAdapter: WalletAdapter
    private val viewModel: WalletViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // This is Android 15 or above

            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }

        } else {
            // This is Android 14 or below

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "H - Wallet", this, R.color.activity_bg, R.color.black)


//        binding.tvName.text = sharePreference.getString(PrefKeys.userName,"")?.replace(",", "")
//        binding.tvEmpcode.text = sharePreference.getString(PrefKeys.emp_code)
//        Glide.with(this@Wallet).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivProfile)

        // RecyclerView setup
        walletAdapter = WalletAdapter(emptyList())
        binding.rvWallet.adapter = walletAdapter
        binding.rvWallet.layoutManager = LinearLayoutManager(this@Wallet)

        // Observe ViewModel data
        viewModel.walletList.observe(this) { list ->
            if (list.isEmpty()){
                binding.tvNotransactionfound.visibility = View.VISIBLE
            }else {
                binding.tvNotransactionfound.visibility = View.GONE
                walletAdapter.updateList(list)
            }
        }

        binding.tvWithdraw.setOnClickListener {
            if (binding.tvAmount.text.toString() == "₹0"){
                CommonMethods.getToast(this@Wallet,"You don't have sufficient amount")
            }else{
                CommonMethods.getToast(this@Wallet,"Withdraw request sent successfully")
            }

        }

    }
}