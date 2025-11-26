package com.hommlie.partner.ui.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hommlie.partner.R
import com.hommlie.partner.adapter.SimNumberAdapter
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityLoginBinding
import com.hommlie.partner.model.SimInfo
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.fetchSimNumbersWithLabels
import com.hommlie.partner.utils.KeyboardUtils
import com.hommlie.partner.utils.ProgressDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Login : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding

    private val viewModel : LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CommonMethods.setStatusBarColor(this, R.color.white, lightStatusBar = true)

//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        window.statusBarColor = Color.TRANSPARENT
//        window.navigationBarColor = Color.TRANSPARENT
//        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true


        binding.btnGetotp.setOnClickListener {
            val hashMap = HashMap<String, String>()
            hashMap["mobile"] = "+91" + viewModel.enteredMobileNo.value
            hashMap["token"] = viewModel.strToken.value

            viewModel.registerUser(hashMap)
        }


        binding.edtMobileno.addTextChangedListener {
            viewModel.onMobileNumberChanged(it.toString())
        }

        requestSimPermissions()

//        binding.edtMobileno.setText("9179518784")


//        binding.edtMobileno.setOnClickListener {
//            val permission = Manifest.permission.READ_PHONE_NUMBERS
//
//            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
//                showSimSelectionIfAvailable()
//            } else {
//                // Check if permanently denied
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                    // Show permission normally
//                    ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
//                } else {
//                    // Permission permanently denied → show settings dialog
//                    showPermissionSettingsDialog()
//                }
//            }
//        }



        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.enteredMobileNo.collect { mobileNo ->
                    val isMobileNoValdid = mobileNo.length == 10
                    binding.btnGetotp.apply {
                        isEnabled = isMobileNoValdid
                        backgroundTintList = ContextCompat.getColorStateList(
                            this@Login,
                            if (isMobileNoValdid) R.color.color_primary else R.color.disable_btn
                        )
                    }
                    if (isMobileNoValdid) {
                        currentFocus?.let { KeyboardUtils.hideKeyboard(it) }
                    }
                    Log.d("Login", "Mobile No: ${mobileNo.length}")
                }
            }
        }



        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            // no-op
                        }

                        is UIState.Loading -> {
                            // Show loading
                            ProgressDialogUtil.showLoadingProgress(this@Login,lifecycleScope)
                        }

                        is UIState.Success -> {
                            val userData = state.data.data
                            ProgressDialogUtil.dismiss()
                            if (state.data.message.equals("User Not Found", true) ||
                                state.data.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@Login, "Your Account Deactivated\nContact to admin", Toast.LENGTH_SHORT).show()
                                return@collect
                            }
                            val intent = Intent(this@Login,ActOTP::class.java)
                            intent.putExtra("mobileno",viewModel.enteredMobileNo.value)
                            intent.putExtra("strToken",viewModel.strToken.value)
                            startActivity(intent)

                            // Reset state
                            viewModel.resetUIState()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            Toast.makeText(this@Login, state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetUIState()
                        }
                    }
                }
            }
        }


    }


    private fun requestSimPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (notGranted.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1001)
            } else {
                // Permissions already granted
//                CommonMethods.fetchSimNumbers(this)
                showSimSelectionIfAvailable()
            }
        } else {
            // Permissions not needed below Android 6.0
            showSimSelectionIfAvailable()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1002) {
            val permission = Manifest.permission.READ_PHONE_NUMBERS
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                showSimSelectionIfAvailable()
            } else {
                showPermissionSettingsDialog() // Show again if still denied
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showSimSelectionIfAvailable()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun showSimSelectorBottomSheet(context: Context, simList: List<SimInfo>, onSelected: (SimInfo) -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottomsheet_select_number, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvNumbers)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SimNumberAdapter(simList) {
            dialog.dismiss()
            onSelected(it)
        }

        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("📱 SIM Permission Required")
            .setMessage(
                "To auto-fill your mobile number from your SIM card, we need permission to access SIM info. ✨\n\n" +
                        "You’ve previously denied this permission. Please enable it manually:\n\n" +
                        "👉 Step 1: Tap on 'Open Settings' below\n" +
                        "👉 Step 2: In the App Info screen, tap 'Permissions'\n" +
                        "👉 Step 3: Tap on 'Phone' or 'SIM access'\n" +
                        "👉 Step 4: Select 'Allow' or 'Allow while using the app'\n\n" +
                        "✅ After that, come back and tap the mobile number field again to continue!"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, 1002)
            }
            .setCancelable(false)
            .show()
    }


    private fun showSimSelectionIfAvailable() {
        val simList = fetchSimNumbersWithLabels(this)
        if (simList.isNotEmpty()) {
            showSimSelectorBottomSheet(this, simList) { selected ->
                binding.edtMobileno.setText(selected.number.replace("+91", "").trim().takeLast(10))
            }
        } else {
            Toast.makeText(this, "No SIM number found", Toast.LENGTH_SHORT).show()
        }
    }


}