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

    suspend fun getFcmServerKey(): String? {
        return configDao.getConfig("fcm_server_key")?.value
    }

    suspend fun setFcmServerKey(key: String) {
        configDao.insertConfig(AppConfig("fcm_server_key", key))
    }

    suspend fun getFcmServiceAccountJson(): String? {
        return configDao.getConfig("fcm_service_account_json")?.value
    }

    suspend fun setFcmServiceAccountJson(json: String) {
        configDao.insertConfig(AppConfig("fcm_service_account_json", json))
    }

    suspend fun getThemeSetting(): String? {
        return configDao.getConfig("theme_setting")?.value
    }

    suspend fun setThemeSetting(theme: String) {
        configDao.insertConfig(AppConfig("theme_setting", theme))
    }

    suspend fun getAuthToken(): String? {
         return configDao.getConfig("auth_token")?.value
    }

    suspend fun setAuthToken(token: String) {
         configDao.insertConfig(AppConfig("auth_token", token))
    }

    suspend fun clearAuthToken() {
         configDao.insertConfig(AppConfig("auth_token", ""))
    }

    suspend fun getUserProfileName(): String? {
         return configDao.getConfig("user_profile_name")?.value
    }

    suspend fun setUserProfileName(name: String) {
         configDao.insertConfig(AppConfig("user_profile_name", name))
    }

    suspend fun getUserProfilePhone(): String? {
         return configDao.getConfig("user_profile_phone")?.value
    }

    suspend fun setUserProfilePhone(phone: String) {
         configDao.insertConfig(AppConfig("user_profile_phone", phone))
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
