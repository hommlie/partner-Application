package com.hommlie.partner.fcmnotification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hommlie.partner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            // Check if message contains data payload
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "New Notification"
                val body = remoteMessage.data["body"] ?: ""
                val imageUrl = remoteMessage.data["imageUrl"] // optional

                if (!imageUrl.isNullOrEmpty()) {
                    showRichNotificationWithImage(applicationContext, title, body, imageUrl)
                } else {
                    showTextOnlyNotification(applicationContext, title, body)
                }
            }
            // Check if message contains notification payload (simple)
            remoteMessage.notification?.let {
                showTextOnlyNotification(applicationContext, it.title ?: "New Notification", it.body ?: "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewToken(token: String) {
        // Send token to your server
        super.onNewToken(token)
    }

    @SuppressLint("MissingPermission")
    private fun showTextOnlyNotification(context: Context, title: String, body: String) {
        val channelId = "default_channel"
        val manager = NotificationManagerCompat.from(context)

        // Create notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun showRichNotificationWithImage(context: Context, title: String, body: String, imageUrl: String) {
        val channelId = "offer_channel"
        val manager = NotificationManagerCompat.from(context)

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Offers",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Download image in background
        serviceScope.launch {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()
                val input = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(input)

                withContext(Dispatchers.Main) {
                    val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.app_logo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setLargeIcon(bitmap)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    manager.notify(System.currentTimeMillis().toInt(), notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to text notification if image fails
                withContext(Dispatchers.Main) {
                    showTextOnlyNotification(context, title, body)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
