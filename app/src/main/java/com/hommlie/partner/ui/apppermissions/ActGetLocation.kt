package com.hommlie.partner.ui.apppermissions

import android.Manifest
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityActGetLocationBinding
import com.hommlie.partner.ui.login.Login
import com.hommlie.partner.ui.registration.ActKycPending
import com.hommlie.partner.ui.registration.ActRegistration
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActGetLocation : AppCompatActivity() {

    private lateinit var binding : ActivityActGetLocationBinding

    @Inject
    lateinit var sharePreference : SharePreference

    private var userid: String? = null

    private var PERMISSION_REQUEST_LOCATION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        binding = ActivityActGetLocationBinding.inflate(layoutInflater)

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


        userid = sharePreference.getString(PrefKeys.userId)

        binding.allowPermissionButton.setOnClickListener {
            requestLocationPermission()
        }



    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            PERMISSION_REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_LOCATION -> {

                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, you can start your location-related tasks
                    //    startLocationUpdates()
                    //   moveToNextScreen()
                    if (Settings.canDrawOverlays(this)) {
                        moveToNextScreen()
                    }else {
                        val intent = Intent(this, ActPermissions::class.java)
                        startActivity(intent)
                    }
                } else {
                    // Permission denied, handle appropriately
                    Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
                    showLocationPermissionDialog(this)
                    // openLocationSettings(this)

                }
                return
            }
        }
    }

    fun openLocationPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(intent)
    }

    fun showLocationPermissionDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Location Permission Required")
        builder.setMessage("Location permission is required for this app to function properly. Please allow location permission from the settings.")

        builder.setPositiveButton("OK") { dialog, _ ->
            // After the user clicks "OK", open the app's settings
            openLocationPermissionSettings(context)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Close the dialog if user clicks "Cancel"
            dialog.dismiss()
        }

        builder.show()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }



    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()){
            if (Settings.canDrawOverlays(this)) {
                moveToNextScreen()
            }else {
                val intent = Intent(this, ActPermissions::class.java)
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
//            startMainActivity()
            if (sharePreference.getInt(PrefKeys.is_reg_form_sub) == 1){
                if (sharePreference.getInt(PrefKeys.is_verified)== 1) {
                    startMainActivity()
                }else{
                    val intent = Intent(this@ActGetLocation, ActKycPending::class.java)
                    startActivity(intent)
                }
            }else {
                val intent = Intent(this@ActGetLocation, ActRegistration::class.java)
                startActivity(intent)
            }
        }
    }


}