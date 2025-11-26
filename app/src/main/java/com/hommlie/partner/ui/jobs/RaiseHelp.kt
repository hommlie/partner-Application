package com.hommlie.partner.ui.jobs

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityRaiseHelpBinding
import com.hommlie.partner.model.NewOrderData
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.showToast
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RaiseHelp : AppCompatActivity() {
    private lateinit var binding : ActivityRaiseHelpBinding

    private val viewModel : RaiseHelpViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference
    private lateinit var user_id : String
    private lateinit var jobData: NewOrderData

    private var latitude: String? = null
    private var longitude: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    val hashMap = HashMap<String,String>()

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRaiseHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Raise Service Issue", this, R.color.white, R.color.black)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        user_id = sharePreference.getString(PrefKeys.userId).toString()
        val json = intent.getStringExtra("job_data")
        if (!json.isNullOrEmpty()) {
            jobData = Gson().fromJson(json, NewOrderData::class.java)
        } else {
            Log.e("JobDetails", "Job data is missing in intent!")
            return
        }

        setupUI(jobData)
        observeViewModel()
    }

    private fun setupUI(jobData: NewOrderData) {

        binding.tvSrid.text = jobData.orderId.toString()
        binding.tvOrderid.text = jobData.orderNo
        binding.tvScheduletime.text = "Time : ${jobData.desiredTime?:"-"} | ${jobData.desiredDate?:"-"}"
        binding.tvCustomername.text = jobData.name
        binding.tvContact.text = jobData.mobile
        binding.tvAddress.text = jobData.address


        val helpOptions = listOf("Customer Not Answering", "Address / Location Issue","On-Site OTP issue","Equipment / Tools Problem", "Technical Error", "Service Not Feasible","Other")

        val adapter = ArrayAdapter(this, R.layout.text_list_item, R.id.tv_text, helpOptions)
        binding.autoCompleteType.setAdapter(adapter)

        binding.autoCompleteType.setOnClickListener {
            binding.autoCompleteType.showDropDown()
            binding.autoCompleteType.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(this, R.drawable.up_arrow),
                null
            )
        }
        binding.autoCompleteType.setOnDismissListener {
            binding.autoCompleteType.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(this, R.drawable.down_arrow),
                null
            )
        }
        binding.autoCompleteType.setOnItemClickListener { _, _, position, _ ->
            val selected = helpOptions[position]
            binding.etOtherReason.visibility =
                if (selected == "Other") View.VISIBLE else View.GONE
        }

        binding.btnSubmit.setOnClickListener {
            val selectedType = binding.autoCompleteType.text.toString()
            val message = if (selectedType == "Other")
                binding.etOtherReason.text.toString().trim()
            else selectedType

            if (message.isEmpty()) {
                showToast("Please select or enter a help reason")
                return@setOnClickListener
            }

            hashMap["user_id"] = user_id
            hashMap["order_id"]= jobData.orderId.toString()
            hashMap["order_number"] = jobData.orderNo
            hashMap["title"] = selectedType
            hashMap["message"] = message
            getLastLocation()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@RaiseHelp,lifecycleScope,"Raise Ticket...","Please wait we are submitting your request.")
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            val data = state.data
                            CommonMethods.showConfirmationDialog(
                                this@RaiseHelp,
                                "Successful !...",
                                data.message ?: "Ticket raised successfully",
                                false,
                                false
                            ) { dialog ->
                                dialog.dismiss()
                                finish()
                            }
                            viewModel.reset_uistate()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.alertErrorOrValidationDialog(this@RaiseHelp,state.message)
                            viewModel.reset_uistate()
                        }
                    }
                }
            }
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                this@RaiseHelp,
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

                        hashMap["lat_lng"]= "${latitude},${longitude}"
                        viewModel.raiseTicket(hashMap)

                    } else {
//                        CommonMethods.alertErrorOrValidationDialog(this, "Please try after some time")
                        viewModel.raiseTicket(hashMap)
                    }
                } else {
                    ProgressDialogUtil.showLoadingProgress(this,lifecycleScope)
                    requestNewLocationData()
                }
            }
    }
    private fun requestNewLocationData() {
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
                        hashMap["lat_lng"]= "${latitude},${longitude}"
                        viewModel.raiseTicket(hashMap)
                    } else {
                        CommonMethods.alertErrorOrValidationDialog(this@RaiseHelp, "Please try again later.")
                        viewModel.raiseTicket(hashMap)
                    }
                } else {
                    Toast.makeText(this@RaiseHelp, "Still unable to get location", Toast.LENGTH_SHORT).show()
                    viewModel.raiseTicket(hashMap)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ProgressDialogUtil.dismiss()
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


}