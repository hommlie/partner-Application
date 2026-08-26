package com.hommlie.partner.ui.capturephoto

import android.graphics.Bitmap
import android.net.Uri

interface PhotoStorage {

    suspend fun savePhoto(
        bitmap: Bitmap,
        fileName: String
    ): Uri
}