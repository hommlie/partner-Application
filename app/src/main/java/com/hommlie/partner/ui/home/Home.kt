package com.hommlie.partner.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.FragmentHomeBinding
import com.hommlie.partner.repository.LocationRepository
import com.hommlie.partner.ui.attendence.ActAttendance
import com.hommlie.partner.ui.jobs.ActTodaysJob
import com.hommlie.partner.ui.jobs.JobDetails
import com.hommlie.partner.ui.refer.ReferEarn
import com.hommlie.partner.ui.registration.ActRegistration
import com.hommlie.partner.ui.travellog.ActTravelLogs
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.isCheckNetwork
import com.hommlie.partner.utils.CommonMethods.isServiceRunning
import com.hommlie.partner.utils.CommonMethods.setTracking
import com.hommlie.partner.utils.CommonMethods.showToast
import com.hommlie.partner.utils.FourSideStrokeDrawable
import com.hommlie.partner.utils.LocationService
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class Home : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    @Inject
    lateinit var sharePreference: SharePreference

    @Inject
    lateinit var locationRepository: LocationRepository

    private val viewModel: HomeViewModel by viewModels()

    private var bateryLevel: String? = ""
    private var deviceModel: String? = ""
    private var deviceName: String? = ""
    private var latitude: String? = null
    private var longitude: String? = null

    private var hasShownBackgroundDeniedDialog = false
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lottieAnimation = binding.lottieAnimationView

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

//        lottieAnimation.setAnimation(R.raw.fingerprint_scanning)
        lottieAnimation.playAnimation()

        val statusBarHeight = CommonMethods.getStatusBarHeight(requireContext())

        val viewStatusBar = binding.viewStatusbar.layoutParams
        viewStatusBar.height = statusBarHeight
        binding.viewStatusbar.layoutParams = viewStatusBar

        val clTop = binding.clTop
        val tvGreeting = binding.tvGreeting

        //  1. Increase height of cl_top
        val clLayoutParams = clTop.layoutParams
        clLayoutParams.height += statusBarHeight //resources.getDimensionPixelSize(R.dimen.cl)
        clTop.layoutParams = clLayoutParams

        //  2. Increase top margin of tv_greeting
        val tvLayoutParams = tvGreeting.layoutParams as ConstraintLayout.LayoutParams
        tvLayoutParams.topMargin += statusBarHeight //resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._42sdp)
        tvGreeting.layoutParams = tvLayoutParams

        val mcvTop = binding.mcvTop //findViewById<ConstraintLayout>(R.id.mcv_top)

        // Update top margin (e.g., to 60dp)
        val layoutParams = mcvTop.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin += statusBarHeight //resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._73sdp)
        mcvTop.layoutParams = layoutParams


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // This is Android 15 or above

        } else {
            // This is Android 14 or below

        }

        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)

        observeIsOnsiteOrder()
        observeGoOnlineOffline()
        observHomeOptions()
        observerDailyPuchLogs()
        observOnsiteJob()
        observeUIStateJobData()
//      observWeather()

//      getLastLocation("3")


        binding.swipeRefresh.setOnRefreshListener {
            viewModel.getUserJobData()
            binding.swipeRefresh.isRefreshing = false

        }

        binding.llTotaljob.setOnClickListener {
            if (sharePreference.getString(PrefKeys.Punch_Status) == "1") {
                val intent = Intent(requireActivity(),ActTodaysJob::class.java)
                intent.putExtra("title","Today's Total Jobs")
                startActivity(intent)
            }else{
                CommonMethods.alertErrorOrValidationDialog(requireActivity(),"Please punch-in to explore jobs")
            }
        }
        binding.llPendingjob.setOnClickListener {
            if (sharePreference.getString(PrefKeys.Punch_Status) == "1") {
                val intent = Intent(requireActivity(),ActTodaysJob::class.java)
                intent.putExtra("title","Today's Pending Jobs")
                startActivity(intent)
            }else{
                CommonMethods.alertErrorOrValidationDialog(requireActivity(),"Please punch-in to explore jobs")
            }
        }
        binding.llCompletedjob.setOnClickListener {
            val intent = Intent(requireActivity(),ActTodaysJob::class.java)
            intent.putExtra("title","Today's Completed Jobs")
            startActivity(intent)
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerText.collect { formattedTime ->
                    binding.tvSpenthrs.text = formattedTime
                }
            }
        }

        binding.mcvReferal.setOnClickListener {
            val intent = Intent(requireActivity(),ReferEarn::class.java)
            startActivity(intent)
        }

//        sharePreference.setString(PrefKeys.Punch_Status,"1")


        /* binding.btnStartlocation.setOnClickListener {
            //  Mark tracking ON
            setTracking(requireContext(), true)

            //  Request to ignore battery optimizations
            if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(requireActivity())) {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(requireActivity())
            } else {
                requireContext().showToast("Already ignoring battery optimizations")
            }

            //  Ask for AutoStart permission only once (if not asked before)
//            if (!hasRequestedAutoStart(requireContext())) {
//                openAutoStartSettings(requireContext())
//                markAutoStartRequested(requireContext())
//            }

            // Start service only if not already running
            if (!isServiceRunning(requireActivity())) {
                val intent = Intent(requireContext(), LocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(requireContext(), intent)
                } else {
                    requireContext().startService(intent)
                }
                requireContext().showToast("Location tracking started")
            } else {
                requireContext().showToast("Tracking is already running")
            }
        }

        binding.btnStoplocation.setOnClickListener {
            setTracking(requireContext(), false)
            CommonMethods.setServiceRunning(requireContext(), false)
            requireContext().stopService(Intent(requireContext(), LocationService::class.java))
        }

        */

        binding.mcvTop.background = FourSideStrokeDrawable(requireContext())

        binding.mcvPunch.setOnClickListener {
            CommonMethods.getToast(requireActivity(),"Hold to Punch")
        }

        binding.mcvJobs.setOnClickListener {
            if (sharePreference.getString(PrefKeys.Punch_Status) == "1") {
                val intent = Intent(requireContext(), ActTodaysJob::class.java)
                startActivity(intent)
            }else{
                CommonMethods.alertErrorOrValidationDialog(requireActivity(),"Please punch-in to start the job")
            }
        }
        binding.mcvOther.setOnClickListener {
            CommonMethods.openWhatsApp(requireActivity(),sharePreference.getString(PrefKeys.contact_us),"")
        }



        binding.mcvPunch.setOnLongClickListener {
            if (isCheckNetwork(requireActivity())) {
                if (!isLocationEnabled(requireContext())) {
                    promptEnableLocation()
                } else {
                    bateryLevel = displayBatteryPercentage().toString()
                    if (binding.tvPunch.text.toString().trim() == "Punch In") {
                        getLastLocation("1")
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Attendance")
                            .setMessage("Are you sure you want to save your today's attendance?")
                            .setPositiveButton("Yes") { _, _ ->

                                viewModel.checkIfOnsiteService(sharePreference.getString(PrefKeys.userId))

//                                viewModel.isOnsiteService.collect { isSuccess ->
//                                    if (isSuccess) {
//                                        CommonMethods.alertErrorOrValidationDialog(
//                                            requireActivity(),
//                                            "Please complete your current task."
//                                        )
//                                    } else {
//                                        getLastLocation("0")
//                                    }
//                                }

                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            } else {
                CommonMethods.alertErrorOrValidationDialog(
                    requireActivity(),
                    resources.getString(R.string.no_internet)
                )
            }
            true
        }

       /* binding.mcvPunch.setOnClickListener {

            if (binding.tvPunch.text == "Punch In"){
                //  Mark tracking ON
                setTracking(requireContext(), true)

                //  Request to ignore battery optimizations
                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(requireActivity())) {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(requireActivity())
                } else {
                    requireContext().showToast("Already ignoring battery optimizations")
                }

                //  Ask for AutoStart permission only once (if not asked before)
//            if (!hasRequestedAutoStart(requireContext())) {
//                openAutoStartSettings(requireContext())
//                markAutoStartRequested(requireContext())
//            }

                // Start service only if not already running
                if (!isServiceRunning(requireActivity())) {
                    val intent = Intent(requireContext(), LocationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(requireContext(), intent)
                    } else {
                        requireContext().startService(intent)
                    }
                    requireContext().showToast("Location tracking started")
                } else {
                    requireContext().showToast("Tracking is already running")
                }
            }else if (binding.tvPunch.text == "Punch Out"){
                setTracking(requireContext(), false)
                CommonMethods.setServiceRunning(requireContext(), false)
                requireContext().stopService(Intent(requireContext(), LocationService::class.java))
            }else{

            }
        }  */


        Glide.with(requireContext()).load("https://www.hommlie.com/panel/public/storage/app/public/images/banner/topbanner-67dc24b0f2df3.png").thumbnail(0.1f).into(binding.ivRefer)


        binding.tvViewall.setOnClickListener {
            val intent = Intent(requireActivity(),ActAttendance::class.java)
            startActivity(intent)
        }

        binding.mcvApplyLeave.setOnClickListener {
            val intent = Intent(requireActivity(),ActRegistration::class.java)
            startActivity(intent)
        }




    }

    private fun observeGoOnlineOffline(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(requireActivity(), lifecycleScope)

                        is UIState.Success -> {

                            val response = state.data

                            if (response.status == 1 ){

                                response.data?.let { data->
                                    val onlineStatus = data.statusCode

                                    sharePreference.setString(PrefKeys.Punch_Status, data.statusCode.toString())
                                    sharePreference.setString(PrefKeys.lastSwipe,CommonMethods.getCurrentTimeDate())
                                    binding.tvLastSwipe.text = "Last Swipe : "+sharePreference.getString(PrefKeys.lastSwipe)

                                    if (onlineStatus == 1 ){
                                        sharePreference.setInt(PrefKeys.todayOnlineId, data.attendance_id)

                                        binding.tvPunch.text = "Punch Out"
                                        binding.ivPunch.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red_logout))
                                        binding.tvPunch.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_logout))
                                        binding.mcvPunch.strokeColor = ContextCompat.getColor(requireContext(), R.color.red_logout)
                                        binding.mcvPunch.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_logout_light))

                                        // Start service only if not already running
                                        if (!isServiceRunning(requireActivity())) {
                                            val intent = Intent(requireContext(), LocationService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                ContextCompat.startForegroundService(requireContext(), intent)
                                            } else {
                                                requireContext().startService(intent)
                                            }
                                            requireContext().showToast("Location tracking started")
                                        } else {

                                            requireContext().showToast("Tracking is already running")

                                            setTracking(requireContext(), false)
                                            CommonMethods.setServiceRunning(requireContext(), false)
                                            requireContext().stopService(Intent(requireContext(), LocationService::class.java))

                                            val intent = Intent(requireContext(), LocationService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                ContextCompat.startForegroundService(requireContext(), intent)
                                            } else {
                                                requireContext().startService(intent)
                                            }

                                            requireContext().showToast("Location tracking started")
                                        }

                                        locationRepository.update_empOnlineStatus(true)

                                        val hashMap = HashMap<String,String>()
                                        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
                                        hashMap["date"] = CommonMethods.getCurrentDateFormatted()
                                        viewModel.getDailyPuchLog(hashMap)


                                        viewModel.getOnsiteJob(sharePreference.getString(PrefKeys.userId))

                                    }else{
//                                        sharePreference.setInt(PrefKeys.todayOnlineId, "totaldistance", "0")

                                        binding.tvPunch.text = "Punch In"
                                        binding.ivPunch.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green))
                                        binding.tvPunch.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                                        binding.mcvPunch.strokeColor = ContextCompat.getColor(requireContext(), R.color.green)
                                        binding.mcvPunch.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light))

                                        setTracking(requireContext(), false)
                                        CommonMethods.setServiceRunning(requireContext(), false)
                                        requireContext().stopService(Intent(requireContext(), LocationService::class.java))

                                        viewModel.stopTimer()

//                                        val stopIntent = Intent(requireActivity(), LocationService::class.java)
//                                        requireActivity().stopService(stopIntent)

                                        locationRepository.update_empOnlineStatus(false)

                                        binding.mcvOnsiteorder.visibility = View.GONE
                                    }
                                }

                            }else{
                                CommonMethods.alertErrorOrValidationDialog(requireActivity(), response.message ?: "Punch failed")
                            }

                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, requireContext())
                                return@collect
                            }
                            Toast.makeText(requireActivity(), state.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    private fun displayBatteryPercentage(intent: Intent? = null): Int {
        val batteryStatus = intent ?: IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            requireActivity().registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let {
            val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat() * 100
        }
        deviceName = Build.BRAND
        deviceModel = Build.MODEL
        return batteryPct?.toInt() ?: 70
    }



    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun promptEnableLocation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enable Location")
            .setMessage("Location services are disabled. Please enable them to continue using the app.")
            .setPositiveButton("Enable") { _, _ ->
                // Open system location settings
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
            .show()
            .setCancelable(false)
    }

    private fun areForegroundPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundLocationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Function to request only the background location permission (Android 10+)
    private fun requestBackgroundLocationPermission() {
        backgroundLocationPermission?.let {
            permissionLauncher.launch(arrayOf(it))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val allGranted = permissions.all { it.value }

        if (allGranted) {
            // All permissions granted
//            requestNewLocationData()
        } else {
            val permanentlyDenied = permissions.entries.any { (perm, granted) ->
                !granted && !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), perm)
            }

            if (permanentlyDenied) {
                //  Permission permanently denied (Don't ask again or system-level revoke)
                if (!hasShownBackgroundDeniedDialog) {
                    hasShownBackgroundDeniedDialog = true
                    showPermissionSettingsDialog()
                }
            } else {
                //  Temporarily denied
                Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show()
                requireActivity().finish()

            }
        }
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(
                "Location permission is permanently denied or unavailable.\n\n" +
                        "Please allow location access from Settings to continue."+
                        "Permissions → Location → 'Allow all the time'."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
//            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private val backgroundLocationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }

    private fun checkAndRequestPermissions() {
        if (!areForegroundPermissionsGranted()) {
            requestLocationPermissions()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
            // Only request background permission — no alert from here
            requestBackgroundLocationPermission()
        }
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = locationPermissions.toMutableList()

        // If background location permission is needed and not granted, request it separately
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
//            backgroundLocationPermission?.let { permissionsToRequest.add(it) }
//        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun openLocationPermissionSettings() {
        try {
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+ (API 30+) — fallback to app settings
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10 (API 29) — fallback
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                }

                else -> {
                    // Older Android — best we can do
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
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
                Toast.makeText(requireContext(), "Unable to open settings.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBackgroundPermissionRationale() {
        val dialog = AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "Permission is required to proceed.", Toast.LENGTH_SHORT).show()
                // Don't dismiss dialog
            }
        }

        dialog.show()
    }


    private fun getLastLocation(s: String) {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                    if (!latitude.isNullOrEmpty() || !longitude.isNullOrEmpty()) {


                        if (s == "3"){
                            viewModel.fetchWeather(latitude.toString().toDouble(),longitude.toString().toDouble(),"AIzaSyBmaR3DSseRPUCCvGT0Ru8aK-Jrm39NlTE")
                        }else {
                            callApiGoOnlineOffline(s)
                        }

                    } else {
                        CommonMethods.alertErrorOrValidationDialog(requireActivity(), "Please try after some time")
                    }
                } else {
                    Toast.makeText(requireActivity(), "Unable to retrieve current location, Trying to get new location ", Toast.LENGTH_SHORT).show()
                    ProgressDialogUtil.showLoadingProgress(requireActivity(),lifecycleScope)
                    requestNewLocationData(s)
                }
            }
    }

    private fun callApiGoOnlineOffline(is_active_status: String){
        val hashMap = HashMap<String, String>()
        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["latitude"] = latitude.toString()
        hashMap["longitude"] = longitude.toString()
        hashMap["battery"] = bateryLevel.toString() + "%"
        hashMap["device"] = deviceModel.toString() + ", " + deviceName
        hashMap["is_active_status"] = is_active_status
        if (is_active_status == "0") {
            hashMap["attendance_id"] = sharePreference.getInt(PrefKeys.todayOnlineId).toString()
            hashMap["distance_travelled"] = "10"
        }

        viewModel.goOnlineOffline(hashMap)
    }

    private fun requestNewLocationData(s: String) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)

                ProgressDialogUtil.dismiss()

                val location = locationResult.lastLocation
                if (location != null) {
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()

                    if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                        callApiGoOnlineOffline(s)
                    } else {
                        CommonMethods.alertErrorOrValidationDialog(requireActivity(), "Please try again later.")
                    }
                } else {
                    Toast.makeText(requireActivity(), "Still unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ProgressDialogUtil.dismiss()
            Toast.makeText(requireActivity(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }



    override fun onResume() {
        super.onResume()

//        getLastLocation("3")

        if (isAdded) {
            if (sharePreference.getString(PrefKeys.Punch_Status) == "1") {

                binding.tvPunch.text = "Punch Out"
                binding.ivPunch.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red_logout))
                binding.tvPunch.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_logout))
                binding.mcvPunch.strokeColor = ContextCompat.getColor(requireContext(), R.color.red_logout)
                binding.mcvPunch.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_logout_light))

                viewModel.getOnsiteJob(sharePreference.getString(PrefKeys.userId))

            } else {

                binding.tvPunch.text = "Punch In"
                binding.ivPunch.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green))
                binding.tvPunch.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                binding.mcvPunch.strokeColor = ContextCompat.getColor(requireContext(), R.color.green)
                binding.mcvPunch.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light))

                binding.mcvOnsiteorder.visibility = View.GONE

            }
        }
        if (sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)){
            binding.tvGreeting.text = "${CommonMethods.getGreetingMessage()} !"
            binding.tv1.text = "Hello, ${sharePreference.getString(PrefKeys.userName).substringBefore(",")?:""}"
        }else{
            binding.tvGreeting.text = "Start earning with hommlie"
        }
        Glide.with(requireContext()).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivEmpimage)


        viewModel.getUserJobData()

        binding.tvLastSwipe.text = "Last Swipe : "+sharePreference.getString(PrefKeys.lastSwipe)
        val hashMap = HashMap<String,String>()
        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        hashMap["date"] = CommonMethods.getCurrentDateFormatted()
        viewModel.getDailyPuchLog(hashMap)

        hasShownBackgroundDeniedDialog = false

        when {
            !isLocationEnabled(requireContext()) -> {
                promptEnableLocation()
            }

//            !areForegroundPermissionsGranted() -> {
//                checkAndRequestPermissions()
//            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted() -> {
                if (!hasShownBackgroundDeniedDialog) {
                    hasShownBackgroundDeniedDialog = true
                    showBackgroundPermissionRationale()
                }
            }

            else -> {
                hasShownBackgroundDeniedDialog = false // Reset flag when everything is good
//                requestNewLocationData()
            }
        }



    }

    private fun observOnsiteJob(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.onsiteJob.collect { state ->
                    when (state){
                        is UIState.Loading ->{
                            ProgressDialogUtil.showLoadingProgress(requireActivity(),lifecycleScope)
                        }
                        is UIState.Success ->{

                            binding.mcvOnsiteorder.visibility = View.VISIBLE
                            ProgressDialogUtil.dismiss()
                            viewModel.resetgetOnsiteJob()
                            val gson = Gson()
                            val json = gson.toJson(state.data.data?.firstOrNull())
                            binding.mcvOnsiteorder.setOnClickListener {
                                val intent = Intent(requireActivity(), JobDetails::class.java)
                                intent.putExtra("job_data",json)
                                intent.putExtra("isComeFromHome",1)
                                startActivity(intent)
                            }

                            binding.tvCustomername.text = state.data.data?.get(0)?.name
                            binding.tvCustomercontact.text = state.data.data?.get(0)?.mobile
                            calculateDuration(state.data.data?.get(0)?.onsite_updated_at.toString())
//                            binding.tvStarteddate.text = state.data.data?.get(0)?.onsite_updated_at
                        }
                        is UIState.Error ->{

                            binding.mcvOnsiteorder.visibility = View.GONE
                            ProgressDialogUtil.dismiss()
                            viewModel.resetgetOnsiteJob()

                        }
                        is UIState.Idle ->{

                        }

                    }
                }
            }
        }
    }


    private fun observeIsOnsiteOrder() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnsiteService.collect { isSuccess ->
                    when (isSuccess) {
                        true -> {
                            CommonMethods.alertErrorOrValidationDialog(requireActivity(), "Please complete your current task.")
                            viewModel.resetOnsiteServiceCheck()
                            ProgressDialogUtil.dismiss()
                        }
                        false -> {
                            getLastLocation("0")
                            viewModel.resetOnsiteServiceCheck()
                            ProgressDialogUtil.dismiss()
                        }
                        null -> {
//                            ProgressDialogUtil.showLoadingProgress(requireActivity(),lifecycleScope)
                        }
                    }

                }
            }

        }

    }

    private fun observHomeOptions() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.homeoptions_uiState.collect { state ->
                    when(state){
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(requireActivity(), lifecycleScope)
                        }
                        is UIState.Success -> {
                            binding.rvSrtOptions.apply {
                                layoutManager = GridLayoutManager(requireActivity(),4,LinearLayoutManager.VERTICAL,false)
                                adapter = OptionsAdapter(requireActivity(),state.data)
                            }
                            ProgressDialogUtil.dismiss()
//                            viewModel.resetHomeOptionsUIState()
                        }
                        is UIState.Error->{
                            ProgressDialogUtil.dismiss()
                            viewModel.resetHomeOptionsUIState()
                        }
                        is UIState.Idle->{

                        }

                    }
                }
            }
        }

    }


    private fun observerDailyPuchLogs(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.dailyPunchLog.collect{ state ->
                    when (state){
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(requireActivity(),lifecycleScope)
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            state.data.data?.let {
                                val isPunchedIn = sharePreference.getString(PrefKeys.Punch_Status) == "1"
                                viewModel.startTimer(it,isPunchedIn)
                            }
                            viewModel.resetDailyPunchState()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
//                            CommonMethods.alertErrorOrValidationDialog(requireActivity(),state.message)
                            viewModel.resetDailyPunchState()
                        }

                        is UIState.Idle -> {

                        }
                    }
                }
            }
        }
    }

    private fun observWeather(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.weather.collect { state->

                    when(state) {
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(requireActivity(),lifecycleScope)
                        }
                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()

                            val weather = state.data.currentWeather
                            if (weather != null) {
                                val temperature = weather.temperature ?: 0.0
                                val humidity = weather.humidity ?: 0.0
                                val windSpeed = weather.windSpeed ?: 0.0
                                val condition = weather.conditions ?: "Unknown"

                                // Example: update UI
//                                binding.tvTemperature.text = "Temperature: $temperature°C"
//                                binding.tvHumidity.text = "Humidity: ${humidity * 100}%"
//                                binding.tvWindSpeed.text = "Wind Speed: $windSpeed m/s"
//                                binding.tvCondition.text = "Condition: $condition"

                                binding.tvGreeting.text = "${condition} ${temperature}°C"

                                // Optionally: display icon or emoji
//                                binding.ivWeatherIcon.setImageResource(getIconForCondition(condition))
                            } else {
                                Toast.makeText(requireContext(), "No weather data available", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                        }
                        is UIState.Idle -> {

                        }
                    }

                }
            }
        }
    }

  /*  private fun getIconForCondition(condition: String): Int {
        return when (condition.lowercase()) {
            "clear" -> R.drawable.ic_clear
            "cloudy", "mostly cloudy" -> R.drawable.ic_cloudy
            "partly cloudy" -> R.drawable.ic_partly_cloudy
            "rain", "light rain", "showers" -> R.drawable.ic_rain
            "thunderstorm", "heavy rain" -> R.drawable.ic_thunderstorm
            "snow", "light snow", "heavy snow" -> R.drawable.ic_snow
            "fog", "haze", "smoke" -> R.drawable.ic_fog
            "windy", "breezy" -> R.drawable.ic_windy
            else -> R.drawable.ic_weather_unknown
        }
    }  */



    fun calculateDuration(startTime: String) {
        return try {
            // Define possible time formats
            val format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // "2025-03-29 13:18:35"
            val format2 = DateTimeFormatter.ISO_INSTANT // "2025-03-29T13:18:35.000000Z"

            // Parse the server time (handling both formats)
            val serverTime = try {
                LocalDateTime.parse(startTime, format1).atZone(ZoneId.of("UTC"))
            } catch (e: DateTimeParseException) {
                Instant.parse(startTime).atZone(ZoneId.of("UTC"))
            }

            // Get the current local time
            val localZone = ZoneId.systemDefault()
            val currentTime = ZonedDateTime.now(localZone)

            // Convert server time to local timezone
            val localServerTime = serverTime.withZoneSameInstant(localZone)

            // Define the desired format
            val outputDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputTimeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

            binding.tvStarteddate.text = localServerTime.format(outputDateFormat)
            binding.tvStartedtime.text = localServerTime.format(outputTimeFormat)


//            // Calculate the duration
//            val duration =
//                Duration.between(localServerTime.toLocalDateTime(), currentTime.toLocalDateTime())
//
//            // Convert to HH:mm:ss format
//            val hours = duration.toHours()
//            val minutes = duration.toMinutes() % 60
//            val seconds = duration.seconds % 60

            // String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } catch (e: DateTimeParseException) {
            // "Invalid Time Format"
            binding.tvStartedtime.text="-"
            binding.tvStarteddate.text="-"
        } catch (e: Exception) {
            //"Error: ${e.localizedMessage}"
            binding.tvStartedtime.text="-"
            binding.tvStarteddate.text="-"
        }
    }


    private fun observeUIStateJobData(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.jobDataUiState.collect{ state ->
                    when(state){
                        is UIState.Idle ->{

                        }
                        is UIState.Loading ->{
                            ProgressDialogUtil.showAleartLoadingProgress(requireActivity(),viewLifecycleOwner.lifecycleScope,"Loading...","Please wait we are fetching your current stats.")
                        }
                        is UIState.Success ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_jobDataUiState()

                            state.data.data?.let { jobSummary ->
                                binding.apply {
                                    tvTotalJob.text = jobSummary.totalJobs.toString()
                                    tvPendingjob.text = jobSummary.pendingJobs.toString()
                                    tvCompletedjob.text = jobSummary.completedJobs.toString()
                                    tvTotalAbsenceIncurrentmonth.text = "${jobSummary.absenceInThisMonth} Absence in this month"
                                    tvTotalLeaveAvailable.text = "${jobSummary.halfDayInThisMonth} Half Day"
                                    tvKmtravelled.text = "${jobSummary.kmTravelledTodays} KM Travelled Today's"
                                }
                            }
                        }
                        is UIState.Error ->{
                            ProgressDialogUtil.dismiss()
                            viewModel.reset_jobDataUiState()
                        }
                    }
                }
            }
        }
    }

}
