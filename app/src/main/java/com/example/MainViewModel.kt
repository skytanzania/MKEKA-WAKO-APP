package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.SavedNotification
import com.example.notifications.NotificationHelper
import com.example.notifications.CloudNotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    WEB_VIEW,
    NOTIFICATIONS,
    ADMIN_PANEL
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    private val notificationHelper: NotificationHelper

    companion object {
        const val DEFAULT_FALLBACK_URL = "https://mikekaapp.co.tz/app"
    }

    // Live Streams
    val allNotifications: StateFlow<List<SavedNotification>>
    
    private val _currentUrl = MutableStateFlow(DEFAULT_FALLBACK_URL)
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    // Real-time server connection diagnostics
    private val _diagnosticStatus = MutableStateFlow("Live Ping: --")
    val diagnosticStatus: StateFlow<String> = _diagnosticStatus.asStateFlow()

    private val _diagnosticLevel = MutableStateFlow("checking") // "checking", "excellent", "warning", "offline"
    val diagnosticLevel: StateFlow<String> = _diagnosticLevel.asStateFlow()

    private val _activeScreen = MutableStateFlow(AppScreen.WEB_VIEW)
    val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0)
    val loadProgress: StateFlow<Int> = _loadProgress.asStateFlow()

    // Config states
    private val _fcmToken = MutableStateFlow("fcm_token_loading_or_unregistered")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val _isPushPreferencesEnabled = MutableStateFlow(true)
    val isPushPreferencesEnabled: StateFlow<Boolean> = _isPushPreferencesEnabled.asStateFlow()

    // Subscription topics list
    private val _subscribedTopics = MutableStateFlow(setOf("General Betting", "Prematch Analytics", "Results Announcements"))
    val subscribedTopics: StateFlow<Set<String>> = _subscribedTopics.asStateFlow()

    // Distributed Cloud database sync states
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastCloudSyncStatus = MutableStateFlow("Tap to sync with live pool")
    val lastCloudSyncStatus: StateFlow<String> = _lastCloudSyncStatus.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        notificationHelper = NotificationHelper(application)

        allNotifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load persisted override URL if any
        viewModelScope.launch {
            repository.getUrlOverride()?.let { savedUrl ->
                if (savedUrl.isNotBlank()) {
                    _currentUrl.value = savedUrl
                }
            }
            // Trigger health check once URL is loaded
            runHealthCheck()
        }

        // Start live continuous background sync from decentralized cloud storage
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000) // Small warm-up delay
            while (isActive) {
                syncNotificationsFromCloud()
                delay(15000) // Poll every 15 seconds for hot live alerts
            }
        }
    }

    fun syncNotificationsFromCloud() {
        if (_isCloudSyncing.value) return
        _isCloudSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudItems = CloudNotificationService.fetchCloudNotifications()
                val localItems = allNotifications.value
                
                var insertedAny = false
                // Filter and insert items we don't already have (comparing by title, message, and timestamp)
                cloudItems.forEach { cloudItem ->
                    val alreadySaved = localItems.any { localItem ->
                        localItem.title == cloudItem.title &&
                        localItem.message == cloudItem.message &&
                        localItem.timestamp == cloudItem.timestamp
                    }
                    if (!alreadySaved) {
                        repository.insertNotification(cloudItem.title, cloudItem.message)
                        // Trigger actual native Android popup notification banner
                        if (_isPushPreferencesEnabled.value) {
                            notificationHelper.showNotification(cloudItem.title, cloudItem.message)
                        }
                        insertedAny = true
                    }
                }
                
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                _lastCloudSyncStatus.value = "Synced: ${sdf.format(Date())}"
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cloud sync failure: ${e.message}")
                _lastCloudSyncStatus.value = "Last sync failed"
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun runHealthCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _diagnosticLevel.value = "checking"
            _diagnosticStatus.value = "Testing connection..."
            val startTime = System.currentTimeMillis()
            try {
                // If the override URL is empty/invalid, bypass or use fallback
                val target = if (_currentUrl.value.startsWith("http://") || _currentUrl.value.startsWith("https://")) _currentUrl.value else DEFAULT_FALLBACK_URL
                val url = URL(target)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "HEAD"
                val responseCode = connection.responseCode
                val duration = System.currentTimeMillis() - startTime
                if (responseCode in 200..399) {
                    _diagnosticStatus.value = "Live Ping: ${duration}ms"
                    _diagnosticLevel.value = "excellent"
                } else {
                    _diagnosticStatus.value = "Server Busy (${responseCode})"
                    _diagnosticLevel.value = "warning"
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                _diagnosticStatus.value = "Offline / Delayed (${duration}ms)"
                _diagnosticLevel.value = "offline"
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _activeScreen.value = screen
    }

    fun setWebLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setWebProgress(progress: Int) {
        _loadProgress.value = progress
        if (progress >= 100) {
            _isLoading.value = false
        }
    }

    fun updateUrlOverride(newUrl: String) {
        viewModelScope.launch {
            _currentUrl.value = newUrl
            repository.setUrlOverride(newUrl)
        }
    }

    fun resetUrlToDefault() {
        viewModelScope.launch {
            _currentUrl.value = DEFAULT_FALLBACK_URL
            repository.setUrlOverride(DEFAULT_FALLBACK_URL)
        }
    }

    fun saveAndBroadcastNotification(title: String, body: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isCloudSyncing.value = true
            _lastCloudSyncStatus.value = "Broadcasting..."
            try {
                // 1. Fetch current cloud notifications from decentralized serverless database to append
                val currentCloudItems = CloudNotificationService.fetchCloudNotifications().toMutableList()
                
                // 2. Add our new broadcast notification
                val newNotification = SavedNotification(
                    title = title,
                    message = body,
                    timestamp = System.currentTimeMillis()
                )
                currentCloudItems.add(newNotification)
                
                // 3. Upload the full list back to the serverless storage
                val success = CloudNotificationService.broadcastCloudNotification(currentCloudItems)
                if (success) {
                    // 4. Force immediate update sync for local client to display
                    syncNotificationsFromCloud()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to broadcast notification: ${e.message}")
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun togglePushPreferences() {
        _isPushPreferencesEnabled.value = !_isPushPreferencesEnabled.value
    }

    fun toggleTopicSubscription(topic: String) {
        val currentSet = _subscribedTopics.value.toMutableSet()
        if (currentSet.contains(topic)) {
            currentSet.remove(topic)
        } else {
            currentSet.add(topic)
        }
        _subscribedTopics.value = currentSet
    }
}
