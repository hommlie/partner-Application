package com.hommlie.partner.repository

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val firebase: FirebaseDatabase,
    private val sharePreference: SharePreference,
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val offlineLocations = mutableListOf<Location>()
    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    private var emp_online_status : Boolean = false

    override suspend fun handleNewLocation(location: Location) {
        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to CommonMethods.getCurrentDateTimeWithT()
        )

        try {
            firebase.getReference("locations/${sharePreference.getString(PrefKeys.userId)}")
//                .push()
                .updateChildren(data)

            prefs.edit().putLong("last_location_time", System.currentTimeMillis()).apply()

        } catch (e: Exception) {
            offlineLocations.add(location)
        }
    }

    override suspend fun getLastLocationTimestamp(): Long {
        return prefs.getLong("last_location_time", 0L)
    }

    override fun update_empOnlineStatus(isOnline: Boolean) {
//            emp_online_status = isOnline
        updateOnlyOnlineStatus(isOnline)
    }


    fun updateOnlyOnlineStatus(isOnline: Boolean) {
        val statusMap = mapOf(
            "isOnline" to isOnline,
            "punchTime" to CommonMethods.getCurrentDateTimeWithT(),
            "name" to sharePreference.getString(PrefKeys.userName)
        )

        firebase.getReference("locations/${sharePreference.getString(PrefKeys.userId)}")
            .updateChildren(statusMap)
            .addOnSuccessListener {
                Log.d("FirebaseStatusUpdate", "Status updated successfully: $isOnline")
            }
            .addOnFailureListener { error ->
                Log.e("FirebaseStatusUpdate", "Failed to update status: ${error.message}")
            }
    }



}
