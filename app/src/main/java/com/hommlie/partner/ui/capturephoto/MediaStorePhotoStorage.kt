package com.hommlie.partner.ui.capturephoto

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorePhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoStorage {

    override suspend fun savePhoto(
        bitmap: Bitmap,
        fileName: String
    ): Uri = withContext(Dispatchers.IO) {

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                fileName
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/YourApp"
                )

                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }
        }

        val imageUri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException(
            "Unable to create MediaStore entry"
        )

        try {

            resolver.openOutputStream(imageUri)?.use { outputStream ->

                val success = bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    95,
                    outputStream
                )

                if (!success) {
                    throw IOException(
                        "Unable to compress image"
                    )
                }
            } ?: throw IOException(
                "Unable to open output stream"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val updateValues = ContentValues().apply {
                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                }

                resolver.update(
                    imageUri,
                    updateValues,
                    null,
                    null
                )
            }

            imageUri

        } catch (e: Exception) {

            resolver.delete(
                imageUri,
                null,
                null
            )

            throw e
        }
    }
}