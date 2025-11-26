package com.hommlie.partner.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hommlie.partner.repository.LocationRepository
import com.hommlie.partner.utils.CommonMethods.isTracking
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay


@HiltWorker
class CheckLocationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepo: LocationRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val lastTime = locationRepo.getLastLocationTimestamp()
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastTime

        val isTrackingEnabled = isTracking(applicationContext)
        Log.d("CheckLocationWorker", "doWork triggered - isTracking: $isTrackingEnabled, timeDiff: $timeDiff ms")

        if (isTrackingEnabled && timeDiff > 15 * 60 * 1000) {
            delay(2000) // ⏳ Add 2 seconds delay before restarting service

            val serviceIntent = Intent(applicationContext, LocationService::class.java)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("CheckLocationWorker", "Starting foreground service")
                    applicationContext.startForegroundService(serviceIntent)
                } else {
                    Log.d("CheckLocationWorker", "Starting background service")
                    applicationContext.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("CheckLocationWorker", "Service restart failed: ${e.localizedMessage}")
                return Result.failure()
            }
        } else {
            Log.d("CheckLocationWorker", "Conditions not met — service not restarted.")
        }

        return Result.success()
    }
}
