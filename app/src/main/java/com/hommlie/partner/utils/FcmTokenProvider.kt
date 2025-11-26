package com.hommlie.partner.utils

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class FcmTokenProvider @Inject constructor() {

    suspend fun getToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    continuation.resume(task.result!!) {}
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Unable to fetch FCM token")
                    )
                }
            }
    }
}

