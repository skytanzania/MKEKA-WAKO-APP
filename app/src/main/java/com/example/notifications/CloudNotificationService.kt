package com.example.notifications

import android.util.Log
import com.example.data.SavedNotification
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object CloudNotificationService {
    private const val TAG = "CloudNotification"
    private const val ENDPOINT = "https://kvdb.io/MikekaWakoAppBucket_v3/notifications"
    
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val notificationListType = Types.newParameterizedType(List::class.java, SavedNotification::class.java)
    private val listAdapter = moshi.adapter<List<SavedNotification>>(notificationListType)

    fun fetchCloudNotifications(): List<SavedNotification> {
        val request = Request.Builder()
            .url(ENDPOINT)
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        return listAdapter.fromJson(bodyString) ?: emptyList()
                    }
                } else if (response.code == 404) {
                    Log.d(TAG, "No notifications found on cloud (404), returning empty list")
                } else {
                    Log.e(TAG, "Unsuccessful response from cloud: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from cloud: ${e.message}")
        }
        return emptyList()
    }

    fun broadcastCloudNotification(notificationList: List<SavedNotification>): Boolean {
        val json = listAdapter.toJson(notificationList)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully broadcasted notifications to cloud")
                    return true
                } else {
                    Log.e(TAG, "Failed update on cloud: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting to cloud: ${e.message}")
        }
        return false
    }
}
