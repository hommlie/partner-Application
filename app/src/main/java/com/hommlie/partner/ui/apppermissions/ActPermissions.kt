package com.hommlie.partner.ui.apppermissions

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityActPermissionsBinding
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.ui.registration.ActKycPending
import com.hommlie.partner.ui.registration.ActRegistration
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import com.hommlie.partner.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActPermissions : AppCompatActivity() {

    private lateinit var binding : ActivityActPermissionsBinding

    @Inject
    lateinit var sharePreference: SharePreference

    private var userid: String? = null
    private var isLogin: Boolean? = false

    private var isoverlayGranted = false
    private var isbackgrounLocationGranted = false
    private var isBatteryOptimizationGranted = false
    private var isNotifcationGranted = false

    private val REQUEST_BACKGROUND_LOCATION_PERMISSION = 101
    private val REQUEST_NOTIFICATION_PERMISSION = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        binding = ActivityActPermissionsBinding.inflate(layoutInflater)

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
        setupToolbar(toolbarView, "App Permissions", this, R.color.transparent, R.color.black)

        userid = sharePreference.getString(PrefKeys.userId)
        isLogin = sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)


        binding.card1.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission(this)
            }
        }

        binding.card2.setOnClickListener {
            requestBackgroundLocationPermission(this,REQUEST_BACKGROUND_LOCATION_PERMISSION)
        }

        binding.card3.setOnClickListener {
            requestBatteryOptimizationPermission(this)
        }

        binding.card4.setOnClickListener {
            checkAndRequestNotificationPermission(this,REQUEST_NOTIFICATION_PERMISSION)
        }

        binding.btnContinue.setOnClickListener{
            moveToNextScreen()
        }

    }


    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners != null && enabledListeners.contains(packageName)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    }


    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33 and higher)
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }


    fun checkAndRequestNotificationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if notification permission is already granted
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCode
                )
            } else {
                // Permission already granted
                println("Notification permission is already granted!")
            }
        } else {
            // For Android 12 and below
            if (NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
                println("Notifications are enabled!")
            } else {
                println("Notifications are disabled! Guide user to enable them in settings.")
                notificationPermissionDialog()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                println("Notification permission granted!")
            } else {
                // Permission denied
                println("Notification permission denied!")
            }
        }
        if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                println("Background location permission granted!")
            } else {
                // Permission denied
//                Toast.makeText(this, "Background location permission denied!", Toast.LENGTH_SHORT).show()
                showBackgroundPermissionRationale()
            }
        }
    }


    private fun notificationPermissionDialog() {
        try {
            val dialog = Dialog(this, R.style.DialogCustomTheme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.disclaimer_dialog)

            val tvDisclaimer = dialog.findViewById<TextView>(R.id.tvDisclaimer)
            val tvAccept = dialog.findViewById<TextView>(R.id.tvAccept)

            tvDisclaimer.setText("Notification permission is required, please allow the permission to get booking notifications.")
            tvAccept.setText("SETTINGS")

            tvAccept.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(intent)
                dialog.dismiss()
            }

            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)

            if (!dialog.isShowing)
                dialog.show()

        } catch (e: Exception) {
            Log.i("TAG", "notificationPermissionDialog: Error=${e.localizedMessage}")
        }

    }

    fun requestBatteryOptimizationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = context.packageName
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)
            } else {
                // App is already excluded from battery optimizations
                println("Battery optimization already disabled for this app.")
            }
        } else {
            // No need for battery optimization permission for devices below Android 6.0
            println("Battery optimization settings not required for this Android version.")

        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        // For devices below Android 6.0, battery optimization does not apply
        return true
    }

    fun requestBackgroundLocationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if background location permission is granted
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request background location permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    requestCode
                )
            }
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

    private fun allPermissionsGranted(): Boolean {
        return isBatteryOptimizationGranted && isbackgrounLocationGranted && isNotifcationGranted  //&&isoverlayGranted
    }



    override fun onResume() {
        super.onResume()

//        if (Settings.canDrawOverlays(this)){
//            binding.cl1.setBackgroundResource(R.drawable.green_bk_with_outline)
//            binding.radioDrawover.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_permission_grant))
//            binding.ivDrawover.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
//            isoverlayGranted = true
//        }
        if (isNotificationPermissionGranted(this)){
            binding.cl4.setBackgroundResource(R.drawable.green_bk_with_outline)
            binding.radioButtonNotification.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_permission_grant))
            binding.ivNotification.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            isNotifcationGranted = true
        }
        if (isBatteryOptimizationIgnored(this)){
            binding.cl3.setBackgroundResource(R.drawable.green_bk_with_outline)
            binding.radioButtonBattery.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_permission_grant))
            binding.ivBattery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            isBatteryOptimizationGranted = true
        }
        if (isBackgroundLocationPermissionGranted(this)){
            binding.cl2.setBackgroundResource(R.drawable.green_bk_with_outline)
            binding.radioButtonAutostart.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_permission_grant))
            binding.ivAutostart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            isbackgrounLocationGranted = true
        }
        if (allPermissionsGranted()){
            binding.btnContinue.isEnabled=true
            binding.btnContinue.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_primary)
        }

    }


    private fun moveToNextScreen() {
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
                    val intent = Intent(this@ActPermissions, ActKycPending::class.java)
                    startActivity(intent)
                }
            }else {
                val intent = Intent(this@ActPermissions, ActRegistration::class.java)
                startActivity(intent)
            }
        }
    }


    private fun startMainActivity() {

        val x = Intent(applicationContext, MainActivity::class.java)
        x.putExtra("signinup", true)
        val bndlanimation = ActivityOptions.makeCustomAnimation(
            applicationContext,
            R.anim.cb_fade_in,
            R.anim.cb_face_out
        ).toBundle()
        startActivity(x, bndlanimation)
        finish()
    }

    private fun showBackgroundPermissionRationale() {
        val dialog = AlertDialog.Builder(this@ActPermissions)
            .setTitle("Background Location Permission Needed")
            .setMessage(
                "To enable background location tracking, go to:\n\n" +
                        "Permissions → Location → 'Allow all the time'."
            )
            .setPositiveButton("Go to Settings") { _, _ ->
                openLocationPermissionSettings()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancelButton.setOnClickListener {
                Toast.makeText(this@ActPermissions, "Permission is required to proceed.", Toast.LENGTH_SHORT).show()
                // Don't dismiss dialog
            }
        }

        dialog.show()
    }

    private fun openLocationPermissionSettings() {
        try {
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+ (API 30+) — fallback to app settings
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10 (API 29) — fallback
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }

                else -> {
                    // Older Android — best we can do
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()

            // Fallback if intent fails
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(this@ActPermissions, "Unable to open settings.", Toast.LENGTH_SHORT).show()
            }
        }
    }


}