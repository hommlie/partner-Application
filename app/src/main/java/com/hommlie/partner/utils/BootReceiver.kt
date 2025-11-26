package com.hommlie.partner.utils

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import com.hommlie.partner.utils.CommonMethods.isServiceRunning
import com.hommlie.partner.utils.CommonMethods.isTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.Default).launch {
                delay(5000L) // Delay to let system settle after boot

                // Force update - because service is not running after boot
                CommonMethods.setServiceRunning(context, false)

                if (isTracking(context) && !isServiceRunning(context)) {
                    try {
                        val serviceIntent = Intent(context, LocationService::class.java)
                        withContext(Dispatchers.Main) {
//                            val serviceIntent = Intent(context, LocationService::class.java)
//                            ContextCompat.startForegroundService(context, serviceIntent)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Log.d("CheckLocationWorker", "Starting foreground service")
                                ContextCompat.startForegroundService(context,serviceIntent)
                            } else {
                                Log.d("CheckLocationWorker", "Starting background service")
                                context.startService(serviceIntent)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Service start failed: ${e.message}")
                    }
                } else {
                    Log.d("BootReceiver", "Tracking disabled or service already running.")
                }
            }
        }
    }
}
