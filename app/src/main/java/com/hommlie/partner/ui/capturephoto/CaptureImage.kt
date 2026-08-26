package com.hommlie.partner.ui.capturephoto

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityCaptureImageBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@AndroidEntryPoint
class CaptureImage : AppCompatActivity() {
    private lateinit var binding : ActivityCaptureImageBinding
    private val viewModel : CaptureImageViewModel by viewModels()

    companion object {
        const val EXTRA_CAPTURE_MODE = "extra_capture_mode"
        const val MODE_SELFIE = "selfie"
        const val IS_TAKE_PHOTO = "isTakePhoto"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }
    private var isSelfieMode = false
    private var isTakePhoto = false

    private lateinit var imageCapture: ImageCapture

    private var hasRequestedPermissions = false
    private var isFlashOn = false
    private var camera: Camera? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCaptureImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        CommonMethods.setStatusBarColor(this,R.color.ub__transparent,false)

        val statusBarHeight = CommonMethods.getStatusBarHeight(this)
        val layoutParams = binding.viewStatusBar.layoutParams
        layoutParams.height = statusBarHeight
        binding.viewStatusBar.layoutParams = layoutParams

        setupClicks()
        observeState()

        isSelfieMode = intent.getStringExtra(EXTRA_CAPTURE_MODE) == MODE_SELFIE
        isTakePhoto = intent.getStringExtra(EXTRA_CAPTURE_MODE) == IS_TAKE_PHOTO

        if (isSelfieMode) {
            binding.ivFlash.visibility = View.GONE
            binding.tvTitle.text = "Take Selfie"
        } else {
            binding.ivFlash.visibility = View.VISIBLE
            binding.tvTitle.text = "Capture Photo"
        }



        if (hasRequiredPermissions()) {
            startCamera()
        } else {
            handlePermissionRequest()
        }

    }

    private fun setupClicks() {

        binding.ivBack.setOnClickListener {
            turnOffFlash()
            finish()
            finishSlideActivity()
        }
        onBackPressedDispatcher.addCallback(this){
            turnOffFlash()
            finish()
            finishSlideActivity()
        }

        binding.ivCapture.setOnClickListener {
            capturePhoto()
        }
        binding.ivFlash.setOnClickListener {
            setFlash()
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    Preview.Builder()
                        .build()
                        .also {

                            it.surfaceProvider =
                                binding.previewView
                                    .surfaceProvider
                        }

                imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .setTargetRotation(
                            binding.previewView.display.rotation
                        )
                        .setFlashMode(
                            ImageCapture.FLASH_MODE_OFF
                        )
                        .build()

                val cameraSelector =
                    if (isSelfieMode) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {

                Log.e(
                    "CaptureImage",
                    "Camera initialization failed",
                    e
                )

                Toast.makeText(
                    this,
                    "Unable to start camera",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {

        if (!::imageCapture.isInitialized) {

            Toast.makeText(
                this,
                "Camera is not ready",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.ivCapture.isEnabled = false

        val photoFile = try {

            File.createTempFile(
                "capture_",
                ".jpg",
                cacheDir
            )

        } catch (e: IOException) {

            binding.ivCapture.isEnabled = true

            Toast.makeText(
                this,
                "Unable to prepare photo | ${e.message}",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                photoFile
            ).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults:
                    ImageCapture.OutputFileResults
                ) {

                    runOnUiThread {

                        binding.ivCapture.isEnabled = true

                        if (isSelfieMode || isTakePhoto) {
                            returnSelfieResult(photoFile)
                        } else {
                            handleCapturedPhoto(photoFile)
                        }
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Log.e(
                        "CaptureImage",
                        "Photo capture failed",
                        exception
                    )

                    photoFile.delete()

                    runOnUiThread {

                        binding.ivCapture.isEnabled =
                            true

                        Toast.makeText(
                            this@CaptureImage,
                            "Failed to capture photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun handleCapturedPhoto(
        photoFile: File
    ) {

        val currentDateTime = SimpleDateFormat(
            "dd/MM/yyyy hh:mm:ss a",
            Locale.US
        ).format(Date())

        val metadata = buildString {

            append("Captured: ")
            append(currentDateTime)

            // Future me yahan add kar sakte ho:
            // append("\nJob ID: $jobId")
            // append("\nLocation: $location")
        }

        val fileName =
            "IMG_${SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())}.jpg"

        viewModel.processAndSavePhoto(
            photoFile = photoFile,
            metadata = metadata,
            fileName = fileName
        )
    }

    private fun observeState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.captureState.collect { state ->

                    when (state) {

                        is UIState.Idle -> {}

                        is UIState.Loading -> {
                            binding.ivCapture.isEnabled = false
                            binding.ivCapture.visibility = View.GONE
                            binding.progressBar.visibility = View.VISIBLE
                        }

                        is UIState.Success -> {

                            binding.ivCapture.isEnabled = true
                            binding.ivCapture.visibility = View.VISIBLE
                            binding.progressBar.visibility = View.GONE

                            Toast.makeText(
                                this@CaptureImage,
                                "Photo saved to Gallery",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.d(
                                "CaptureImage",
                                "Saved URI: ${state.data}"
                            )
                            viewModel.resetCaptureState()
                        }

                        is UIState.Error -> {

                            binding.ivCapture.isEnabled = true
                            binding.ivCapture.visibility = View.VISIBLE
                            binding.progressBar.visibility = View.GONE

                            Toast.makeText(
                                this@CaptureImage,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetCaptureState()
                        }
                    }
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (!cameraGranted) {
            return false
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            val storageGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

            if (!storageGranted) {
                return false
            }
        }

        return true
    }
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted =
                permissions.values.all { it }

            if (allGranted) {

                startCamera()

            } else {

                handlePermissionDenied()
            }
        }
    private fun requestRequiredPermissions() {

        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            permissions.add(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }
    private fun handlePermissionRequest() {

        if (hasRequiredPermissions()) {
            startCamera()
            return
        }

        if (!hasRequestedPermissions) {

            hasRequestedPermissions = true

            requestRequiredPermissions()

            return
        }

        /*
         * Permission was already requested before.
         * If Android allows rationale → show rationale.
         * Otherwise → Settings.
         */
        if (shouldShowPermissionRationale()) {

            showPermissionRationale()

        } else {

            showPermissionSettingsDialog()
        }
    }
    private fun shouldShowPermissionRationale(): Boolean {

        if (
            shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        ) {
            return true
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            if (
                shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                return true
            }
        }

        return false
    }
    private fun handlePermissionDenied() {

        if (hasRequiredPermissions()) {
            startCamera()
            return
        }

        val cameraPermission =
            Manifest.permission.CAMERA

        if (
            shouldShowRequestPermissionRationale(
                cameraPermission
            )
        ) {
            showPermissionRationale()
        } else {

            /*
             * Permission is permanently denied
             * or user has disabled it from Settings.
             */
            showPermissionSettingsDialog()
        }
    }

    private fun showPermissionRationale() {

        CommonMethods.showConfirmationDialog(
            context = this,
            title = "Permission Required",
            message = "Camera permission is required to capture photos. Please allow the permission to continue.",
            isCancelable = false,
            show_no_btn = true,
            positiveText = "Allow",
            negativeText = "Cancel",

            onNegativeClick = {
                it.dismiss()
                finish()
                finishSlideActivity()
            },

            onConfirm = {
                it.dismiss()
                requestRequiredPermissions()
            }
        )
    }
    private fun showPermissionSettingsDialog() {

        CommonMethods.showConfirmationDialog(
            context = this,
            title = "Permission Required",
            message = """
Camera permission is required to capture photos.

To enable it:
1. Tap "Open Settings".
2. Open "Permissions".
3. Select "Camera".
4. Choose "Allow" or "Allow only while using the app".
5. Return to the app and try again.
""".trimIndent(),
            isCancelable = false,
            show_no_btn = true,
            positiveText = "Open Settings",
            negativeText = "Cancel",

            onNegativeClick = {
                it.dismiss()
                finish()
                finishSlideActivity()
            },

            onConfirm = {
                it.dismiss()
                openAppSettings()
            }
        )
    }
    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {

            data = Uri.fromParts(
                "package",
                packageName,
                null
            )
        }

        startActivity(intent)
    }
    private fun setFlash() {

        val currentCamera = camera
            ?: return

        if (!currentCamera.cameraInfo.hasFlashUnit()) {

            Toast.makeText(
                this,
                "Flash is not available on this device",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        isFlashOn = !isFlashOn

        currentCamera.cameraControl.enableTorch(
            isFlashOn
        )

        binding.ivFlash.setImageResource(
            if (isFlashOn) {
                R.drawable.ic_flash_on
            } else {
                R.drawable.ic_flash_off
            }
        )
    }
    private fun turnOffFlash() {

        camera?.cameraControl?.enableTorch(false)

        isFlashOn = false

        binding.ivFlash.setImageResource(
            R.drawable.ic_flash_off
        )
    }

    override fun onResume() {
        super.onResume()

        if (
            !::imageCapture.isInitialized &&
            hasRequiredPermissions()
        ) {
            startCamera()
        }
    }

    private fun returnSelfieResult(
        photoFile: File
    ) {
        val uri = Uri.fromFile(photoFile)

        setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_RESULT_URI, uri.toString())
            }
        )
        finish()
        finishSlideActivity()
    }

    override fun onDestroy() {
        turnOffFlash()
        cameraExecutor.shutdown()

        super.onDestroy()
    }

}