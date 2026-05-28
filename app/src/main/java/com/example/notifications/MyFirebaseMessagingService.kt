package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.data.AppDatabase
import com.example.data.SavedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Firebase Cloud Messaging Token received: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"] ?: "New Update"
        val body = message.notification?.body ?: message.data["body"] ?: "Check out recent sports postings on Mikeka App"

        // Display Native Lockscreen Notification Banner
        NotificationHelper(applicationContext).showNotification(title, body)

        // Persist Notification into dynamic Room database list
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.notificationDao().insertNotification(
                    SavedNotification(
                        title = title,
                        message = body
                    )
                )
            } catch (e: Exception) {
                Log.e("FCM", "Failed to persist notification locally: ${e.message}")
            }
        }
    }
}
