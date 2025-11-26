package com.hommlie.partner.view

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActSplashBinding
import com.hommlie.partner.ui.apppermissions.ActGetLocation
import com.hommlie.partner.ui.apppermissions.ActPermissions
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.ui.profile.ProfileDetails_GeneratedInjector
import com.hommlie.partner.ui.registration.ActKycPending
import com.hommlie.partner.ui.registration.ActRegistration
import com.hommlie.partner.utils.BatteryOptimizationHelper
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.showToast
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActSplash : AppCompatActivity() {
    private lateinit var binding: ActivityActSplashBinding

    @Inject
    lateinit var sharePreference: SharePreference
    val viewModel : SplashViewModel by viewModels()

    private var userid: String? = null
    private var isLogin: Boolean? = false

    private val BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    private val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActSplashBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Example: dark icons for Android 15+
            insetsController?.isAppearanceLightStatusBars = true
            insetsController?.isAppearanceLightNavigationBars = true
        } else {
            // Example: light icons for older versions
            insetsController?.isAppearanceLightStatusBars = false
            insetsController?.isAppearanceLightNavigationBars = false
        }


        sharePreference.setString(PrefKeys.contact_us,"6363865658")


        Log.d("PermissionDebug", "Requesting permissions: $LOCATION_PERMISSIONS")

        userid = sharePreference.getString(PrefKeys.userId)
        isLogin = sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)

        observeCheckVersion()

        binding.mcvUpdate.setOnClickListener {
            val appPackageName = packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }
        }


    }


    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check each permission individually
        for (permission in LOCATION_PERMISSIONS) {
            if (!hasPermission(permission)) {
                permissionsToRequest += permission
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasPermission(BACKGROUND_PERMISSION)
        ) {
            permissionsToRequest += BACKGROUND_PERMISSION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(NOTIFICATION_PERMISSION)
        ) {
            permissionsToRequest += NOTIFICATION_PERMISSION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !hasPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        ) {
            permissionsToRequest += Manifest.permission.FOREGROUND_SERVICE_LOCATION
        }

        Log.d("PermissionDebug", "Requesting: $permissionsToRequest")

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                1001
            )
        } else {
            checkBatteryOptimization()
        }
    }


    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkBatteryOptimization()
            } else {
                Toast.makeText(this, "All permissions required!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun forcePermissionTest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }


    private fun startMainActivity() {
        val x = Intent(applicationContext, MainActivity::class.java)
//        x.putExtra("signinup", true)
        val bndlanimation = ActivityOptions.makeCustomAnimation(
            applicationContext,
            R.anim.cb_fade_in,
            R.anim.cb_face_out
        ).toBundle()
        startActivity(x, bndlanimation)
        finish()
    }


    private fun moveToNextScreen() {
        if (CommonMethods.hasLocationPermission(this)) {
//            if (Settings.canDrawOverlays(this)) {
            if (isBackgroundLocationPermissionGranted(this)) {
                if (userid.isNullOrEmpty()) {
                    val x = Intent(applicationContext, Login::class.java)
                    val bndlanimation = ActivityOptions.makeCustomAnimation(
                        applicationContext,
                        R.anim.cb_fade_in,
                        R.anim.cb_face_out
                    ).toBundle()
                    startActivity(x, bndlanimation)
                    finish()
                } else {
                    if (sharePreference.getInt(PrefKeys.is_reg_form_sub) == 1){
                        if (sharePreference.getInt(PrefKeys.is_verified)== 1) {
                            startMainActivity()
                        }else{
                            val intent = Intent(this@ActSplash, ActKycPending::class.java)
                            startActivity(intent)
                        }
                    }else {
                        val intent = Intent(this@ActSplash, ActRegistration::class.java)
                        startActivity(intent)
                    }
                }
            } else {
                val intent = Intent(this, ActPermissions::class.java)
                intent.putExtra("movetoscreen", "moveToNextScreen")
                startActivity(intent)
                finish()
            }
        } else {
            val intent = Intent(this, ActGetLocation::class.java)
            intent.putExtra("movetoscreen", "moveToNextScreen")
            startActivity(intent)
            finish()
        }
    }

    fun isBackgroundLocationPermissionGranted(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if background location permission is granted
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            // No need for background location permission in Android versions below Q
            true
        }
    }


        private fun observeCheckVersion(){
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED){
                    viewModel.checkversionUIState.collect{state ->
                        when(state){
                            is UIState.Idle ->{}
                            is UIState.Loading ->{
                                ProgressDialogUtil.showLoadingProgress(this@ActSplash,lifecycleScope)
                            }
                            is UIState.Success ->{
                                ProgressDialogUtil.dismiss()
                                viewModel.reset_checkversionUIState()

                                if (state.data.status == 1) {
                                    val versionCode = CommonMethods.getAppVersionCode(this@ActSplash)
                                    val currentAppVersion = state.data.versionCode
                                    if (currentAppVersion > versionCode) {
                                        binding.clUpdate.visibility = View.VISIBLE
                                    } else {
                                        moveToNextScreen()
                                    }
                                }else{
                                    moveToNextScreen()
                                }

                            }
                            is UIState.Error ->{
                                ProgressDialogUtil.dismiss()
                                viewModel.reset_checkversionUIState()
                                moveToNextScreen()
                            }
                        }
                    }
                }
            }
        }


    override fun onResume() {
        super.onResume()
        viewModel.checkversion()
    }


}

