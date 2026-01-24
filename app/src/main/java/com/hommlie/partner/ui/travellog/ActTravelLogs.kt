package com.hommlie.partner.ui.travellog

import android.Manifest
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.json.JSONObject
import com.google.maps.android.PolyUtil
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActTravelLogsBinding
import com.hommlie.partner.model.TravelLogData
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toFormattedDate_ddmmmyyyy
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ActTravelLogs : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequestCode = 1
    private var permissionDialog: AlertDialog? = null

    private lateinit var binding : ActivityActTravelLogsBinding

    @Inject
    lateinit var sharePreference: SharePreference

    private val viewModel : TravelLogViewModel by viewModels()

    private val smallMarkerIconnn by lazy { getSmallMarkerIconn() }

    private var bikeMarker: Marker? = null
    private val bikeTrail = PolylineOptions().width(6f).color(Color.GREEN)
    private var progressPolyline: Polyline? = null

    var currentDate: String? =null
    val hashMap=HashMap<String,String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActTravelLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

       /* mapView.getMapAsync { map ->
            googleMap = map

            //  Place it right here after initializing googleMap
            try {
                val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.map_style
                    )
                )
                if (!success) {
                    Log.e("MapStyle", "Style parsing failed.")
                }
            } catch (e: Resources.NotFoundException) {
                Log.e("MapStyle", "Can't find style. Error: ", e)
            }

            // You can now safely add markers, draw routes, etc.
        } */


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
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


        observeTravelLogs()


        hashMap["user_id"]=sharePreference.getString(PrefKeys.userId)
        hashMap["order_status"]="4"
        hashMap["date"]=currentDate.toString()


        currentDate = CommonMethods.getCurrentDateFormatted()
        binding.tvTravellogof.text = "${currentDate?.toFormattedDate_ddmmmyyyy()}  "
        binding.tvTravellogfor.text = "Details of : ${currentDate?.toFormattedDate_ddmmmyyyy()}"

        viewModel.fetchTravelLogs(this,CommonMethods.getCurrentDateFormatted())


        binding.mapView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    // Stop NestedScrollView from intercepting all gestures (scroll/pinch/drag)
                    binding.nsv.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Allow NSV to resume intercepting after gesture ends
                    binding.nsv.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Let MapView handle the event
        }



        binding.tvTravellogof.setOnClickListener {

            if(CommonMethods.isInternetAvailable(this)) {

                val calendar = Calendar.getInstance()
                val year = calendar[Calendar.YEAR]
                val month = calendar[Calendar.MONTH]
                val day = calendar[Calendar.DAY_OF_MONTH]

                val datePickerDialog = DatePickerDialog(
                    this,
                    { datePicker, year, month, dayOfMonth ->
                        var d = dayOfMonth.toString()
                        var m = (month + 1).toString()
                        val y = year.toString()
                        if (d.length == 1) {
                            d = "0$d"
                        }
                        if (m.length == 1) {
                            m = "0$m"
                        }
                        val date = "$year-$m-$d"
                        val date2 = "$d-$m-$year"
                        binding.tvTravellogfor.setText("Details of : " + date2)
                        binding.tvTravellogof.setText(date.toFormattedDate_ddmmmyyyy() + "  ")
                        hashMap["date"] = date
                        currentDate = date
                        viewModel.fetchTravelLogs(this,date)
                    },
                    year, month, day
                )
                datePickerDialog.datePicker.maxDate = calendar.timeInMillis
                datePickerDialog.show()
            }else{
                CommonMethods.showConfirmationDialog(this,"Alert!","Please connect to internet",false,false,"Retry"){
                    it.dismiss()
                }
            }
        }

    }

    private fun observeTravelLogs() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Loading -> {
                            ProgressDialogUtil.showLoadingProgress(this@ActTravelLogs, lifecycleScope)
                        }
                        is UIState.Success -> {
                            state.data.data?.let { data->

                                binding.tvTotaldistance.text = "Total Distance : ${data.total_distance?:"0"} KM"
                                binding.tvTravellogof.text = "${currentDate?.toFormattedDate_ddmmmyyyy()}  "

                                drawTravelRoute(data)

                                //  Set adapter
                                binding.recyclerViewTimeline.apply {
                                    layoutManager = LinearLayoutManager(this@ActTravelLogs)
                                    adapter = data.jobs?.let { TravelTimelineAdapter(it)
                                    }
                                }
                                if (data.jobs.isNullOrEmpty()){
                                    binding.nsv.visibility = View.GONE
                                    binding.tvNodata.visibility = View.VISIBLE
                                }else{
                                    binding.nsv.visibility = View.VISIBLE
                                    binding.tvNodata.visibility = View.GONE
                                }

                            }

                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIState()
                        }
                        is UIState.Error -> {
                            binding.nsv.visibility = View.GONE
                            binding.tvNodata.visibility = View.VISIBLE
                            ProgressDialogUtil.dismiss()

                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@ActTravelLogs, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@ActTravelLogs)
                                return@collect
                            }
                            if (state.message == "No internet connection") {
                                CommonMethods.showConfirmationDialog(
                                    this@ActTravelLogs,
                                    "Alert!",
                                    state.message,
                                    false,
                                    true,
                                    "Retry",
                                    "Cancel"
                                ) {
                                    it.dismiss()
                                    viewModel.fetchTravelLogs(this@ActTravelLogs, CommonMethods.getCurrentDateFormatted())
                                }
                            }
                            viewModel.resetUIState()
                        }
                        is UIState.Idle -> {
                            // Init
                        }
                    }
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        Log.d("MAP_DEBUG", "Map object ready")

        googleMap.setOnMapLoadedCallback {
            Log.d("MAP_DEBUG", "Tiles loaded")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
        }

        (mapView?.findViewWithTag<View>("GoogleMapMyLocationButton") ?: findMyLocationButton(mapView?.parent as? ViewGroup))?.let { locationButton ->
            val parent = locationButton.parent as? RelativeLayout
            if (parent != null) {
                val params = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    setMargins(0, 0, 20, 30) // bottom and right margin
                }

                locationButton.layoutParams = params
                locationButton.requestLayout()
            }
        }

    }

    fun findMyLocationButton(viewGroup: ViewGroup?): View? {
        if (viewGroup == null) return null
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ImageView && child.drawable != null) {
                Log.d("MapUI", "Found ImageView - maybe My Location")
                return child
            } else if (child is ViewGroup) {
                val result = findMyLocationButton(child)
                if (result != null) return result
            }
        }
        return null
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))
//                    updateMarkerAndAddress(currentLatLng)
                }
            }
        }
    }

    private fun drawTravelRoute(data: TravelLogData) {
        googleMap.clear()

//        val polylineOptions = PolylineOptions().apply {
//            color(getColor(R.color.color_primary))
//            width(5f)
//        }

        val boundsBuilder = LatLngBounds.Builder()

        // Collect all LatLngs (start + jobs)
        val allLocations = mutableListOf<LatLng>()

        // Add Start Location Marker
        val startLatLng = LatLng(data.start_location.latitude, data.start_location.longitude)
//        polylineOptions.add(startLatLng)
        allLocations.add(startLatLng)
        boundsBuilder.include(startLatLng)


        googleMap.addMarker(
            MarkerOptions().apply {
                position(startLatLng)
                title("Start Location")
                snippet("Starting Point")
                icon(smallMarkerIconnn)
            }
        )

        // Add Job Locations
        data.jobs?.forEachIndexed { index, job ->
            val jobLatLng = LatLng(job.latitude, job.longitude)
            allLocations.add(jobLatLng)
            boundsBuilder.include(jobLatLng)

            googleMap.addMarker(
                MarkerOptions().apply {
                    position(jobLatLng)
                    title("Job ${index + 1}: ${job.location_name}")
                    snippet("Time: ${CommonMethods.convertToIndiaTime(job.start_time?:"")}")
                    icon(smallMarkerIconnn)
                }
            )
        }


        val fullPath = mutableListOf<LatLng>()

        lifecycleScope.launch {
            allLocations.zipWithNext().forEach { (start, end) ->
                try {
                    val path = getRoutePoints(start, end)
                    fullPath.addAll(path)
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(path)
                            .color(
                                ContextCompat.getColor(
                                    this@ActTravelLogs,
                                    R.color.color_primary
                                )
                            )
                            .width(6f)
                    )
                } catch (e: Exception) {
                    Log.e("RouteError", "Failed to draw route: ${e.localizedMessage}")
                }
                delay(300)
            }

            if (bikeMarker == null && fullPath.isNotEmpty()) {
                addBikeMarker(fullPath.first())
            }

            animateBikeAlongFullPath(fullPath)
        }

        // Move camera to fit all markers
        val bounds = boundsBuilder.build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun getSmallMarkerIconn(): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_location)  // use your own drawable
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888) // You can reduce size here
        canvas.setBitmap(bitmap)
        drawable?.setBounds(0, 0, 72, 72)
        drawable?.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    suspend fun getRoutePoints(origin: LatLng, dest: LatLng): List<LatLng> = withContext(Dispatchers.IO) {
        val apiKey = "AIzaSyBmaR3DSseRPUCCvGT0Ru8aK-Jrm39NlTE"
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&mode=driving&key=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext emptyList()

        val polyline = routes.getJSONObject(0)
            .getJSONObject("overview_polyline")
            .getString("points")

        PolyUtil.decode(polyline)
    }


    private fun addBikeMarker(position: LatLng) {
        val bitmap = getBitmapFromVectorDrawable(R.drawable.ic_bike)
        if (bitmap != null) {
            val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
            bikeMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(descriptor)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
            )
        } else {
            Log.e("MarkerError", "Bike bitmap is null")
        }
    }


    fun getBitmapFromVectorDrawable(@DrawableRes drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null

        val originalWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
        val originalHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48

        // Adjust scale factor to control size (e.g., 0.5f = 50%)
        val scaleFactor = 0.06f
        val scaledWidth = (originalWidth * scaleFactor).toInt()
        val scaledHeight = (originalHeight * scaleFactor).toInt()

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }



    private fun animateBikeAlongFullPath(path: List<LatLng>) {
        if (path.size < 2 || bikeMarker == null) return

        var index = 0
        val handler = Handler(Looper.getMainLooper())
        val durationPerSegment = 100L // time between each point

        val runnable = object : Runnable {
            override fun run() {
                if (index < path.size - 1) {
                    val start = path[index]
                    val end = path[index + 1]

                    val bearing = getBearing(start, end)
                    bikeMarker?.rotation = bearing

                    val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
                    valueAnimator.duration = durationPerSegment
                    valueAnimator.interpolator = LinearInterpolator()
                    valueAnimator.addUpdateListener { animation ->
                        val v = animation.animatedFraction
                        val lat = v * end.latitude + (1 - v) * start.latitude
                        val lng = v * end.longitude + (1 - v) * start.longitude
                        val newPos = LatLng(lat, lng)
                        bikeMarker?.position = newPos
                    }

                    valueAnimator.start()
                    index++
                    handler.postDelayed(this, durationPerSegment)
                }
            }
        }

        handler.post(runnable)
    }


    private fun getBearing(start: LatLng, end: LatLng): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }


    // Required MapView lifecycle forwarding
    override fun onResume() {
        super.onResume()
        mapView.onResume()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            showPermissionDeniedDialog()
        }else{
            permissionDialog?.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapView.getMapAsync { map ->
                    googleMap = map

                    // Place it right here after initializing googleMap
                    try {
                        val success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.map_style
                            )
                        )
                        if (!success) {
                            Log.e("MapStyle", "Style parsing failed.")
                        }
                    } catch (e: Resources.NotFoundException) {
                        Log.e("MapStyle", "Can't find style. Error: ", e)
                    }

                    // You can now safely add markers, draw routes, etc.
                }

            }
        }
    }

    private fun showPermissionDeniedDialog() {

        if (permissionDialog != null && permissionDialog?.isShowing == true) {
            return
        }

        permissionDialog = AlertDialog.Builder(this)
            .setTitle("\uD83D\uDCCD Permission Required")
            .setMessage("This app requires location permission to function properly. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                // Send user to app's settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
            .create()

        permissionDialog?.show()
        permissionDialog?.setCancelable(false)
    }


}