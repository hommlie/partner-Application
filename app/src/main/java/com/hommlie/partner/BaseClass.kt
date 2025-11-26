package com.hommlie.partner

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hommlie.partner.utils.CheckLocationWorker
import com.hommlie.partner.utils.CommonMethods.isTracking
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class BaseClass : Application() {

    companion object {
        var currentActivity : Activity?=null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BaseClass", "Hilt has been initialized")

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        // Force portrait orientation globally
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                activity.window.statusBarColor = Color.WHITE
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    activity.window.decorView.systemUiVisibility =
//                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//                }
            }
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity == activity) currentActivity = null
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }


        // ✅ Only start WorkManager if user has enabled tracking
        if (isTracking(this)) {
            val workRequest = PeriodicWorkRequestBuilder<CheckLocationWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "CheckLocationWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

            /* registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
             override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                 activity.window.statusBarColor = Color.WHITE // change as needed
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                     activity.window.decorView.systemUiVisibility =
                         View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                 }
             }
             override fun onActivityStarted(activity: Activity) {}
             override fun onActivityResumed(activity: Activity) {}
             override fun onActivityPaused(activity: Activity) {}
             override fun onActivityStopped(activity: Activity) {}
             override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
             override fun onActivityDestroyed(activity: Activity) {}
         }) */


}