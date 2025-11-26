package com.hommlie.partner.utils

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
//import com.google.android.gms.location.*
import com.hommlie.partner.R
import com.hommlie.partner.repository.LocationRepository
import com.hommlie.partner.utils.CommonMethods.isTracking
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import java.time.Duration

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var locationRepository: LocationRepository
    private lateinit var locationCallback: LocationCallback


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CommonMethods.setServiceRunning(this, true)
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Location Tracking")
            .setContentText("Tracking in background")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        CommonMethods.setServiceRunning(this, false)
      /*  if (isTracking(applicationContext)) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                val restartIntent = Intent(applicationContext, LocationService::class.java)
                ContextCompat.startForegroundService(applicationContext, restartIntent)
            }
        } else {
            Log.d("LocationService", "Tracking is OFF, not restarting service.")
        }  */

        Log.d("LocationService", "Service destroyed")

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
       /* if (isTracking(applicationContext)) {
            val restartService = Intent(applicationContext, LocationService::class.java)
            ContextCompat.startForegroundService(applicationContext, restartService)
        } else {
            Log.d("LocationService", "Tracking is OFF, not restarting service from task removed.")
        } */
        Log.d("LocationService", "Task removed")
        super.onTaskRemoved(rootIntent)
    }


    override fun onBind(intent: Intent?): IBinder? = null

   /* private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 20 * 1000             // 20 seconds
            fastestInterval = 10 * 1000      // 10 seconds (if another app is using location)
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        locationRepository.handleNewLocation(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    } */

    /*private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Duration.ofSeconds(20) // interval
        )
            .setMinUpdateIntervalMillis(10_000) // fastestInterval
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        locationRepository.handleNewLocation(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    } */

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(20)
        )
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(10)) // fastest interval
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        locationRepository.handleNewLocation(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }


}
