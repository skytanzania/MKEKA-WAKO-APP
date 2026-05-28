package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    private val configDao = db.appConfigDao()
    private val notificationDao = db.notificationDao()

    val allNotifications: Flow<List<SavedNotification>> = notificationDao.getAllNotifications()

    suspend fun getUrlOverride(): String? {
        return configDao.getConfig("webview_url")?.value
    }

    suspend fun setUrlOverride(url: String) {
        configDao.insertConfig(AppConfig("webview_url", url))
    }

    suspend fun insertNotification(title: String, message: String) {
        notificationDao.insertNotification(SavedNotification(title = title, message = message))
    }

    suspend fun deleteNotification(id: Int) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearNotifications() {
        notificationDao.clearAllNotifications()
    }
}
