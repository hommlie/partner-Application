package com.hommlie.partner.repository

import android.location.Location

interface LocationRepository {
    suspend fun handleNewLocation(location: Location)
    suspend fun getLastLocationTimestamp(): Long
    fun update_empOnlineStatus(isOnline: Boolean)
}