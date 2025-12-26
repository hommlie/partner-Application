package com.hommlie.partner.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hommlie.partner.R
import com.hommlie.partner.model.SimInfo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.snackbar.Snackbar
import com.hommlie.partner.ui.login.Login
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone

object CommonMethods {
    private const val PREF_NAME = "location_prefs"
    private const val KEY_IS_SERVICE_RUNNING = "is_service_running"
    private const val REQUEST_CODE_OVERLAY = 101
    private val CAMERA_PERMISSION_CODE = 1003

    fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun isTracking(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean("is_tracking", false)
    }

    fun setTracking(context: Context, value: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putBoolean("is_tracking", value).apply()
    }

    fun markAutoStartRequested(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putBoolean("auto_start_done", true).apply()
    }

    fun hasRequestedAutoStart(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean("auto_start_done", false)
    }

    fun setServiceRunning(context: Context, isRunning: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_SERVICE_RUNNING, isRunning)
            .apply()
    }


    fun openAutoStartSettings(context: Context) {
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val component = when {
                Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ->
                    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")

                Build.MANUFACTURER.equals("oppo", ignoreCase = true) ->
                    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")

                Build.MANUFACTURER.equals("vivo", ignoreCase = true) ->
                    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")

                Build.MANUFACTURER.equals("realme", ignoreCase = true) ->
                    ComponentName("com.realme.securitycenter", "com.realme.securitycenter.StartupAppListActivity")

                else -> null
            }

            if (component != null) {
                intent.component = component
                if (canResolveIntent(context, intent)) {
                    context.startActivity(intent)
                    return
                }
            }

            // fallback
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(fallbackIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Please enable AutoStart manually from settings", Toast.LENGTH_LONG).show()
        }
    }


    private fun canResolveIntent(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

   /* fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == serviceClass.name
        }
    } */

    fun isServiceRunning(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_SERVICE_RUNNING, false)
    }

    fun checkAndRequestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        } else {
            Log.d("OverlayPermission", "Already granted")
        }
    }

    fun handleOverlayPermissionResult(activity: Activity, requestCode: Int) {
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(activity)) {
                Log.d("OverlayPermission", "Granted")
                Toast.makeText(activity, "Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("OverlayPermission", "Denied")
                Toast.makeText(activity, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun setStatusBarColor(activity: Activity, colorResId: Int, lightStatusBar: Boolean = true) {
        val window = activity.window

        window.statusBarColor = ContextCompat.getColor(activity, colorResId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.setSystemBarsAppearance(
                if (lightStatusBar) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (lightStatusBar) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }



    fun vibratePhone(context: Context, duration: Long = 200L, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }


    @SuppressLint("MissingPermission")
    fun fetchSimNumbersWithLabels(context: Context): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            activeSubscriptionInfoList?.forEachIndexed { index, info ->
                val simSlot = "SIM ${index + 1}"
                val carrier = info.carrierName ?: "Unknown"
                val number = info.number

                val fallbackNumber = getFallbackLine1Number(context, index)

                val finalNumber = when {
                    !number.isNullOrBlank() -> number
                    !fallbackNumber.isNullOrBlank() -> fallbackNumber
                    else -> null
                }

                if (!finalNumber.isNullOrBlank()) {
                    simList.add(SimInfo("$simSlot - $carrier", finalNumber))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return simList
    }


    @SuppressLint("MissingPermission")
    fun getFallbackLine1Number(context: Context, slotIndex: Int): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telephonyManager.createForSubscriptionId(slotIndex)?.line1Number
            } else {
                telephonyManager.line1Number
            }
        } catch (e: Exception) {
            null
        }
    }


    fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "☀\uFE0F Good Morning"
            in 12..16 -> "\uD83C\uDF24 Good Afternoon"
            else -> "\uD83C\uDF19 Good Evening"
        }
    }
//    fun getGifForTime(): Int {
//        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
//        return when (hour) {
//            in 5..10 -> R.drawable.morning      // 05:00 - 10:59
//            in 11..16 -> R.drawable.day         // 11:00 - 16:59
//            in 17..20 -> R.drawable.evening     // 17:00 - 20:59
//            else -> R.drawable.night            // 21:00 - 04:59
//        }
//    }



    fun String.toFormattedDate(): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = parser.parse(this)
            formatter.format(date ?: Date())
        } catch (e: Exception) {
            this
        }
    }

    fun String.toFormattedDate_ddmmmyyyy(): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // correct pattern
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = parser.parse(this)
            formatter.format(date ?: Date())
        } catch (e: Exception) {
            this
        }
    }
    fun String.toFormattedDate_yyyymmdd(): String {
        return try {
            val parser = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) // correct pattern
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = parser.parse(this)
            formatter.format(date ?: Date())
        } catch (e: Exception) {
            this
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context as Activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun alertDialogNoInternet(act: Activity, msg: String?) {
        var dialog: Dialog? = null
        try {
            if (dialog != null) {
                dialog.dismiss()
                dialog = null
            }
            dialog = Dialog(act, R.style.AppCompatAlertDialogStyleBig)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            );
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(false)
            val mInflater = LayoutInflater.from(act)
            val mView = mInflater.inflate(R.layout.dlg_validation, null, false)
            val textDesc: TextView = mView.findViewById(R.id.tvMessage)
            textDesc.text = msg
            val tvOk: TextView = mView.findViewById(R.id.tvOk)
            val finalDialog: Dialog = dialog
            tvOk.setOnClickListener {
                finalDialog.dismiss()
                act.recreate()
            }
            dialog.setContentView(mView)
            dialog.show()
            dialog.setCancelable(false)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun isCheckNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getToast(activity: Activity, strTxtToast: String) {
        Toast.makeText(activity, strTxtToast, Toast.LENGTH_SHORT).show()
    }



    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        isCancelable: Boolean,
        show_no_btn : Boolean,
        onConfirm: (DialogInterface) -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, _ ->
                onConfirm(dialog)
            }
            .setCancelable(isCancelable)

        if (show_no_btn) {
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }

        builder.show()
    }


    fun alertErrorOrValidationDialog(act: Activity, msg: String?) {
        var dialog: Dialog? = null
        try {
            if (dialog != null) {
                dialog.dismiss()
                dialog = null
            }
            dialog = Dialog(act, R.style.AppCompatAlertDialogStyleBig)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            );
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(false)
            val mInflater = LayoutInflater.from(act)
            val mView = mInflater.inflate(R.layout.dlg_validation, null, false)
            val textDesc: TextView = mView.findViewById(R.id.tvMessage)
            textDesc.text = msg
            val tvOk: TextView = mView.findViewById(R.id.tvOk)
            val finalDialog: Dialog = dialog
            tvOk.setOnClickListener {
                finalDialog.dismiss()
            }
            dialog.setContentView(mView)
            dialog.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentDateFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return LocalDate.now().format(formatter)
    }

    fun getCurrentTime(): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault())
        return LocalDateTime.now().format(formatter).uppercase(Locale.getDefault())
    }

    fun getCurrentDateTime(): String {
        val formatter = DateTimeFormatter.ofPattern(
            "dd-MM-yyyy h:mm a",
            Locale.ENGLISH
        )
        return LocalDateTime.now().format(formatter)
    }

    fun getCurrentDateTimeWithT(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy 'T' HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }


//    fun convertToIndiaTime(dateTimeStr: String): String {
//        return try {
//            // 1. Formatter for input string (without timezone)
//            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//
//            // 2. Parse as LocalDateTime (since no timezone in input)
//            val localDateTime = LocalDateTime.parse(dateTimeStr, inputFormatter)
//
//            // 3. Assume input is UTC, convert to ZonedDateTime
//            val utcDateTime = localDateTime.atZone(ZoneId.of("UTC"))
//
//            // 4. Convert to India Time
//            val indiaTime = utcDateTime.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
//
//            // 5. Format back to same format
//            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//            indiaTime.format(outputFormatter)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            ""
//        }
//    }

    fun convertToIndiaTime(dateTimeStr: String): String {
        return try {
            val odt = OffsetDateTime.parse(dateTimeStr)
            val indiaTime = odt.atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH)
            indiaTime.format(outputFormatter).uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun showErrorFullMsg(activity: Activity, message: String) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        )

        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams

        val topMarginPx = (activity.resources.displayMetrics.density * 50).toInt() // 50dp margin

        when (params) {
            is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topMarginPx
                snackbarView.layoutParams = params
            }
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topMarginPx
                snackbarView.layoutParams = params
            }
        }

        snackbarView.setBackgroundColor(ContextCompat.getColor(activity, R.color.red_logout))
        snackbar.setTextColor(Color.WHITE)
        snackbarView.backgroundTintList = null
        snackbar.show()
    }

    fun showSuccessFullMsg(activity: Activity, message: String) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        )

        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams

        val topMarginPx = (activity.resources.displayMetrics.density * 50).toInt() // 50dp margin

        when (params) {
            is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topMarginPx
                snackbarView.layoutParams = params
            }
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = topMarginPx
                snackbarView.layoutParams = params
            }
        }

        snackbarView.setBackgroundColor(ContextCompat.getColor(activity, R.color.parrotgreen))
        snackbar.setTextColor(Color.WHITE)
        snackbarView.backgroundTintList = null
        snackbar.show()
    }


    fun logOut(sharePreference: SharePreference, context: Context) {
        sharePreference.setBoolean(PrefKeys.IS_LOGGED_IN, false)
        sharePreference.clearAll()
        val intent = Intent(context, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }

    fun getAppVersionCode(context: Context): Int {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun animateFadeHide(context: Context, view: View?) {
        if (view != null && view.visibility == View.VISIBLE) {
            val animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

            view.startAnimation(animFadeOut)
            view.visibility = View.GONE
        }
    }

    fun animateFadeShow(context: Context, view: View) {
        if (view.visibility != View.VISIBLE) {
            val animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)

            view.startAnimation(animFadeIn)
            view.visibility = View.VISIBLE
        }
    }


    fun prepareImagePart(partName: String, bitmap: Bitmap): MultipartBody.Part {
        // Convert bitmap to byte array
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        // Create RequestBody from byte array with correct media type
        val requestBody: RequestBody = byteArray.toRequestBody("image/png".toMediaTypeOrNull())

        // Return MultipartBody.Part using the given partName
        return MultipartBody.Part.createFormData(partName, "image.png", requestBody)
    }


    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isCameraPermissionDinead(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_DENIED
    }

    fun requestCameraPermission(activity: Activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            // Show an explanation to the user why this permission is needed
            Toast.makeText(activity, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }

        // Request camera permission
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    fun openAppSettingsforcamera(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }


    fun openDialPad(context: Context, phoneNum: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNum")
        context.startActivity(intent)
    }

    fun getCurrentMonthNumber(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH)
    }

    fun getPreviousMonthName(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

    fun getCurrentYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR)
    }

    fun File.toMultipartBodyPart(fieldName: String): MultipartBody.Part {
        val requestFile = this.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, this.name, requestFile)
    }


    fun compressImageFromUri(context: Context, uri: Uri): Uri? {
        return try {
            // Step 1: Read EXIF data for orientation
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.let { ExifInterface(it) }
            val rotationDegrees = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )?.let { exifOrientationToDegrees(it) } ?: 0
            exifInputStream?.close()

            // Step 2: Decode bitmap
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e("CompressDebug", "❌ Bitmap is null for URI: $uri")
                return null
            }

            // Step 3: Rotate if needed
            val rotatedBitmap = if (rotationDegrees != 0) {
                rotateBitmap(originalBitmap, rotationDegrees)
            } else {
                originalBitmap
            }

            // Step 4: Resize to reasonable dimensions
            val resizedBitmap = getResizedBitmap(rotatedBitmap, 1080, 1080)

            // Step 5: Save to cache
            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            Log.d("CompressDebug", "✅ Compressed image saved: ${file.absolutePath}, size=${file.length()} bytes, rotation=$rotationDegrees°")
            Uri.fromFile(file)

        } catch (e: Exception) {
            Log.e("CompressDebug", "❌ Compression failed: ${e.message}")
            null
        }
    }



    fun exifOrientationToDegrees(orientation: Int): Int {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    fun getResizedBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = when {
            width > height -> {
                val ratio = width.toFloat() / maxWidth
                Bitmap.createScaledBitmap(bitmap, maxWidth, (height / ratio).toInt(), true)
            }
            height > width -> {
                val ratio = height.toFloat() / maxHeight
                Bitmap.createScaledBitmap(bitmap, (width / ratio).toInt(), maxHeight, true)
            }
            else -> {
                Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true)
            }
        }
        return ratioBitmap
    }

    fun prepareFilePart(fieldName: String, uri: Uri, context: Context): MultipartBody.Part {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileBytes = inputStream!!.readBytes()
        inputStream.close()

        val requestFile = fileBytes.toRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, "image_${System.currentTimeMillis()}.jpg", requestFile)
    }

    fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    fun openWhatsApp(context: Context, phoneNum: String, defaultMessage: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$phoneNum&text=${URLEncoder.encode(defaultMessage, "UTF-8")}"
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle exceptions, e.g., if WhatsApp is not installed on the device
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            openDialPad(context,phoneNum)
        }
    }
    fun formatDateToDayAndWeek(dateString: String): String {
        return try {
            // Parse input string (expected format: yyyy-MM-dd)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            // Format to "dd EEEE" → example: "18 Thursday"
            val outputFormat = SimpleDateFormat("dd EEEE", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            e.printStackTrace()
            "" // return empty string if parsing fails
        }
    }

    fun safeParseInstant(dateString: String?): Instant {
        return try {
            if (dateString.isNullOrBlank()) {
                Instant.EPOCH
            } else {
                val odt = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                odt.toInstant()
            }
        } catch (e: Exception) {
            Instant.EPOCH
        }
    }

    fun formatToIST(utcTime: String?): String {
        if (utcTime == null) return "-"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(utcTime)

            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

            outputFormat.format(date!!)
        } catch (e: Exception) {
            "-"
        }
    }

    fun String.toCapwords(): String {
        return this.lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }


    fun formatDateToReadable(input: String): String {
        val inputFormats = listOf(
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "yyyy/MM/dd"
        )

        for (format in inputFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                Log.d("FormatDebug", "Trying format: $format with input: $input")

                val date = sdf.parse(input)
                if (date == null) {
                    Log.d("FormatDebug", "Parsing returned null for format: $format")
                    continue
                }

                val calendar = Calendar.getInstance().apply { time = date }
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
                val year = calendar.get(Calendar.YEAR)

                val daySuffix = getDayOfMonthSuffix(day)
                val result = "$day$daySuffix $month $year"

                return result
            } catch (e: Exception) {
                Log.e("FormatDebug", "Failed parsing with format: $format, exception: ${e.message}")
                // ignore and try next format
            }
        }
        return input
    }


    // Helper to get "st", "nd", "rd", "th" suffix
    fun getDayOfMonthSuffix(day: Int): String {
        return if (day in 11..13) {
            "th"
        } else {
            when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    }

    fun TextView.colorAsterisk(colorResId: Int) {
        val color = ContextCompat.getColor(context, colorResId)
        val textValue = text.toString()

        if (textValue.contains("*")) {
            val startIndex = textValue.indexOf("*")
            val endIndex = startIndex + 1
            val spannable = SpannableString(textValue)
            spannable.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text = spannable
        }
    }


    suspend fun getAddressFromLatLngSafe(
        context: Context,
        latitude: String,
        longitude: String
    ): String = withContext(Dispatchers.IO) {
        getAddressFromLatLng(context, latitude, longitude)
    }

    private fun getAddressFromLatLng(
        context: Context,
        latitude: String,
        longitude: String
    ): String {

        return try {
            val lat = latitude.toDoubleOrNull()
            val lng = longitude.toDoubleOrNull()

            if (lat == null || lng == null) return ""

            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    address.subLocality?.let { append("$it, ") }
                    address.locality?.let { append("$it, ") }
//                    address.adminArea?.let { append("$it, ") }
//                    address.postalCode?.let { append("$it, ") }
//                    address.countryName?.let { append(it) }
                }.trim().trimEnd(',')
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
    fun monthNameFromZeroBased(index: Int): String {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, index.coerceIn(0, 11)) }
        return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }
    fun Double.formatCoins(): String {
        return if (this % 1.0 == 0.0) {
            this.toInt().toString()   // 10.0 -> 10
        } else {
            String.format("%.1f", this)          // 12.5 -> 12.5
        }
    }

}