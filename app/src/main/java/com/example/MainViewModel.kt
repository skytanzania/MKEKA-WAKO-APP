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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

enum class AppScreen {
    WEB_VIEW,
    NOTIFICATIONS,
    ADMIN_PANEL,
    SETTINGS,
    PRIVACY_POLICY,
    SLIP_BUILDER
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

    private val _fcmServerKeySetting = MutableStateFlow("")
    val fcmServerKeySetting: StateFlow<String> = _fcmServerKeySetting.asStateFlow()

    private val _fcmServiceAccountJsonSetting = MutableStateFlow("")
    val fcmServiceAccountJsonSetting: StateFlow<String> = _fcmServiceAccountJsonSetting.asStateFlow()

    private val _fcmBroadcastStatus = MutableStateFlow("Idle")
    val fcmBroadcastStatus: StateFlow<String> = _fcmBroadcastStatus.asStateFlow()

    // Subscription topics list
    private val _subscribedTopics = MutableStateFlow(setOf("General Betting", "Prematch Analytics", "Results Announcements"))
    val subscribedTopics: StateFlow<Set<String>> = _subscribedTopics.asStateFlow()

    // Distributed Cloud database sync states
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastCloudSyncStatus = MutableStateFlow("Tap to sync with live pool")
    val lastCloudSyncStatus: StateFlow<String> = _lastCloudSyncStatus.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // --- Mikeka REST API States ---
    private val _userProfile = MutableStateFlow<com.example.network.UserProfile?>(null)
    val userProfile: StateFlow<com.example.network.UserProfile?> = _userProfile.asStateFlow()

    private val _userSubscription = MutableStateFlow<com.example.network.UserSubscription?>(null)
    val userSubscription: StateFlow<com.example.network.UserSubscription?> = _userSubscription.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registrationActionNeeded = MutableStateFlow(false)
    val registrationActionNeeded: StateFlow<Boolean> = _registrationActionNeeded.asStateFlow()

    private val _registrationPhone = MutableStateFlow("")
    val registrationPhone: StateFlow<String> = _registrationPhone.asStateFlow()

    // Premium Slips State
    private val _premiumSlips = MutableStateFlow<List<com.example.network.PremiumSlip>>(emptyList())
    val premiumSlips: StateFlow<List<com.example.network.PremiumSlip>> = _premiumSlips.asStateFlow()

    private val _freePreviewSlip = MutableStateFlow<com.example.network.PremiumSlip?>(null)
    val freePreviewSlip: StateFlow<com.example.network.PremiumSlip?> = _freePreviewSlip.asStateFlow()

    private val _isSlipsLoading = MutableStateFlow(false)
    val isSlipsLoading: StateFlow<Boolean> = _isSlipsLoading.asStateFlow()

    private val _slipsError = MutableStateFlow<String?>(null)
    val slipsError: StateFlow<String?> = _slipsError.asStateFlow()

    // Portal Configuration State
    private val _portalPackages = MutableStateFlow<List<com.example.network.AppConfigPackage>>(emptyList())
    val portalPackages: StateFlow<List<com.example.network.AppConfigPackage>> = _portalPackages.asStateFlow()

    private val _supportPhone = MutableStateFlow("+255700000000")
    val supportPhone: StateFlow<String> = _supportPhone.asStateFlow()

    private val _supportEmail = MutableStateFlow("admin@mikekaapp.co.tz")
    val supportEmail: StateFlow<String> = _supportEmail.asStateFlow()

    // Checkout/Order State
    private val _activeOrder = MutableStateFlow<com.example.network.CreateOrderResponse?>(null)
    val activeOrder: StateFlow<com.example.network.CreateOrderResponse?> = _activeOrder.asStateFlow()

    private val _isPaymentChecking = MutableStateFlow(false)
    val isPaymentChecking: StateFlow<Boolean> = _isPaymentChecking.asStateFlow()

    private val _paymentSuccess = MutableStateFlow<Boolean?>(null)
    val paymentSuccess: StateFlow<Boolean?> = _paymentSuccess.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    // Tipster Explorer State
    private val _tipsterLeaderboard = MutableStateFlow<List<com.example.network.TipsterListItem>>(emptyList())
    val tipsterLeaderboard: StateFlow<List<com.example.network.TipsterListItem>> = _tipsterLeaderboard.asStateFlow()

    private val _selectedTipsterDetail = MutableStateFlow<com.example.network.TipsterDetailResponse?>(null)
    val selectedTipsterDetail: StateFlow<com.example.network.TipsterDetailResponse?> = _selectedTipsterDetail.asStateFlow()

    private val _isTipstersLoading = MutableStateFlow(false)
    val isTipstersLoading: StateFlow<Boolean> = _isTipstersLoading.asStateFlow()

    // Chat Support State
    private val _chatMessages = MutableStateFlow<List<com.example.network.ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<com.example.network.ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        
        // Dynamic internet monitoring to prevent offline user access
        val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNet)
        _isOnline.value = caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    // Trigger a check to make sure the endpoint resolves too
                    runHealthCheck()
                }

                override fun onLost(network: Network) {
                    val act = cm.activeNetwork
                    val c = cm.getNetworkCapabilities(act)
                    _isOnline.value = c != null && (
                            c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
                }
            })
        } catch (e: Exception) {
            Log.e("MainViewModel", "Could not register network callback observer: ${e.message}")
        }

        repository = AppRepository(database)
        notificationHelper = NotificationHelper(application)

        allNotifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Fetch actual firebase registration token and sync
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("MainViewModel", "Fetching FCM registration token failed", task.exception)
                    _fcmToken.value = "FCM Unavailable (No Internet or Play Services)"
                    return@addOnCompleteListener
                }
                val token = task.result
                _fcmToken.value = token
                Log.d("MainViewModel", "Real Firebase Cloud Messaging token obtained: $token")
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed initializing Firebase: ${e.message}")
            _fcmToken.value = "FCM Integration Error"
        }

        // Subscribe to initial topics
        _subscribedTopics.value.forEach { topic ->
            val normalizedTopic = topic.replace(" ", "_").lowercase()
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(normalizedTopic)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Initial subscribe failed for $normalizedTopic: ${e.message}")
            }
        }

        // Auto-subscribe user to the 'all' topic for general system broadcasts
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainViewModel", "Auto-subscribed to global 'all' FCM topic successful")
                    }
                }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed subscribing to 'all' topic: ${e.message}")
        }

        // Load persisted FCM Legacy Server Key setting
        viewModelScope.launch {
            repository.getFcmServerKey()?.let { storedKey ->
                _fcmServerKeySetting.value = storedKey
            }
        }

        // Load persisted FCM Service Account JSON setting
        viewModelScope.launch {
            repository.getFcmServiceAccountJson()?.let { storedJson ->
                _fcmServiceAccountJsonSetting.value = storedJson
            }
        }

        // Load persisted Theme mode setting
        viewModelScope.launch {
            repository.getThemeSetting()?.let { storedTheme ->
                _themeMode.value = storedTheme
            }
        }

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

        // --- Mikeka REST API Boot Check ---
        viewModelScope.launch {
            try {
                val token = repository.getAuthToken()
                if (!token.isNullOrBlank()) {
                    com.example.network.MikekaApiClient.authToken = token
                    _isUserLoggedIn.value = true
                    fetchMyProfileDetails()
                }
                
                // Preload portal config, leaderboard, free preview
                loadSubscriptionConfig()
                loadTipsterLeaderboard()
                loadFreePreview()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error booting Mikeka REST Client: ${e.message}")
            }
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
        val normalizedTopic = topic.replace(" ", "_").lowercase()
        if (currentSet.contains(topic)) {
            currentSet.remove(topic)
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic(normalizedTopic)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("MainViewModel", "Unsubscribed from FCM topic: $normalizedTopic")
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "FCM unsubscribe failed: ${e.message}")
            }
        } else {
            currentSet.add(topic)
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(normalizedTopic)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("MainViewModel", "Subscribed to FCM topic: $normalizedTopic")
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "FCM subscribe failed: ${e.message}")
            }
        }
        _subscribedTopics.value = currentSet
    }

    fun updateFcmServerKey(newKey: String) {
        viewModelScope.launch {
            _fcmServerKeySetting.value = newKey
            repository.setFcmServerKey(newKey)
        }
    }

    fun updateFcmServiceAccountJson(newJson: String) {
        viewModelScope.launch {
            _fcmServiceAccountJsonSetting.value = newJson
            repository.setFcmServiceAccountJson(newJson)
        }
    }

    fun updateThemeMode(newMode: String) {
        viewModelScope.launch {
            _themeMode.value = newMode
            repository.setThemeSetting(newMode)
        }
    }

    private fun getOAuth2AccessToken(
        serviceAccountJson: String,
        onSuccess: (String, String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val jsonObject = org.json.JSONObject(serviceAccountJson)
            val projectId = jsonObject.optString("project_id")
            val clientEmail = jsonObject.optString("client_email")
            val privateKeyPem = jsonObject.optString("private_key")

            if (projectId.isNullOrEmpty() || clientEmail.isNullOrEmpty() || privateKeyPem.isNullOrEmpty()) {
                onFailure(IllegalArgumentException("Service Account JSON is missing vital fields (project_id, client_email, private_key)"))
                return
            }

            val nowSeconds = System.currentTimeMillis() / 1000
            val expSeconds = nowSeconds + 3600

            val header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}"
            val payload = "{\"iss\":\"$clientEmail\",\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\",\"aud\":\"https://oauth2.googleapis.com/token\",\"exp\":$expSeconds,\"iat\":$nowSeconds}"

            val headerBase64 = android.util.Base64.encodeToString(
                header.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            val payloadBase64 = android.util.Base64.encodeToString(
                payload.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )

            val signatureInput = "$headerBase64.$payloadBase64"

            // Clean PEM private key
            val cleanPem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
            val keyBytes = android.util.Base64.decode(cleanPem, android.util.Base64.DEFAULT)
            
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(keySpec)

            val signature = java.security.Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(signatureInput.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()

            val signatureBase64 = android.util.Base64.encodeToString(
                signatureBytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )

            val assertion = "$signatureInput.$signatureBase64"

            val client = okhttp3.OkHttpClient()
            val formBody = okhttp3.FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", assertion)
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val responseJson = org.json.JSONObject(bodyStr)
                    val accessToken = responseJson.getString("access_token")
                    onSuccess(accessToken, projectId)
                } else {
                    onFailure(java.io.IOException("Google Token API error code ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun broadcastFCMNotification(
        title: String,
        body: String,
        targetTopicName: String = "all",
        onFinished: (Boolean, String) -> Unit
    ) {
        val serviceAccountJson = _fcmServiceAccountJsonSetting.value.trim()
        val legacyKey = _fcmServerKeySetting.value.trim()

        if (serviceAccountJson.isBlank() && legacyKey.isBlank()) {
            _fcmBroadcastStatus.value = "Error: Key/JSON Empty"
            onFinished(false, "FCM Credentials are empty. Please configure either FCM HTTP v1 JSON or Legacy Key.")
            return
        }

        val normalizedTopic = targetTopicName.replace(" ", "_").lowercase()
        _fcmBroadcastStatus.value = "Sending..."

        viewModelScope.launch(Dispatchers.IO) {
            if (serviceAccountJson.isNotBlank()) {
                try {
                    getOAuth2AccessToken(
                        serviceAccountJson = serviceAccountJson,
                        onSuccess = { accessToken, projectId ->
                            val client = okhttp3.OkHttpClient()
                            val escapedTitle = escapeJson(title)
                            val escapedBody = escapeJson(body)

                            val jsonPayload = """
                                {
                                  "message": {
                                    "topic": "$normalizedTopic",
                                    "notification": {
                                      "title": "$escapedTitle",
                                      "body": "$escapedBody"
                                    },
                                    "data": {
                                      "title": "$escapedTitle",
                                      "body": "$escapedBody"
                                    }
                                  }
                                }
                            """.trimIndent()

                            val mediaType = "application/json; charset=utf-8".toMediaType()
                            val requestBody = jsonPayload.toRequestBody(mediaType)

                            val request = Request.Builder()
                                .url("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                                .post(requestBody)
                                .addHeader("Authorization", "Bearer $accessToken")
                                .addHeader("Content-Type", "application/json")
                                .build()

                            client.newCall(request).execute().use { response ->
                                val responseBody = response.body?.string() ?: ""
                                if (response.isSuccessful) {
                                    _fcmBroadcastStatus.value = "Successful push (v1) on /topics/$normalizedTopic"
                                    Log.d("MainViewModel", "FCM v1 Broadcast success: $responseBody")
                                    
                                    saveAndBroadcastNotification(title, body)

                                    viewModelScope.launch(Dispatchers.Main) {
                                        onFinished(true, "Successfully sent broadcast push message via Firebase FCM v1 API!")
                                    }
                                } else {
                                    _fcmBroadcastStatus.value = "FCM v1 Failed: Code ${response.code}"
                                    Log.e("MainViewModel", "FCM v1 dynamic error response: ${response.code} $responseBody")
                                    viewModelScope.launch(Dispatchers.Main) {
                                        onFinished(false, "FCM v1 Server returned error ${response.code}: $responseBody")
                                    }
                                }
                            }
                        },
                        onFailure = { err ->
                            _fcmBroadcastStatus.value = "OAuth Error"
                            Log.e("MainViewModel", "OAuth Google private key handshake failed", err)
                            viewModelScope.launch(Dispatchers.Main) {
                                onFinished(false, "OAuth 2.0 handshake failed: ${err.localizedMessage ?: err.message}")
                            }
                        }
                    )
                } catch (e: Exception) {
                    _fcmBroadcastStatus.value = "Error"
                    Log.e("MainViewModel", "FCM HTTP v1 outer exception", e)
                    viewModelScope.launch(Dispatchers.Main) {
                        onFinished(false, "Exception occurred: ${e.message}")
                    }
                }
            } else {
                val client = okhttp3.OkHttpClient()
                val escapedTitle = escapeJson(title)
                val escapedBody = escapeJson(body)

                val jsonPayload = """
                    {
                      "to": "/topics/$normalizedTopic",
                      "notification": {
                        "title": "$escapedTitle",
                        "body": "$escapedBody",
                        "sound": "default"
                      },
                      "data": {
                        "title": "$escapedTitle",
                        "body": "$escapedBody"
                      },
                      "priority": "high"
                    }
                """.trimIndent()

                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = jsonPayload.toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("https://fcm.googleapis.com/fcm/send")
                        .post(requestBody)
                        .addHeader("Authorization", "key=$legacyKey")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            _fcmBroadcastStatus.value = "Successful push (legacy) on /topics/$normalizedTopic"
                            Log.d("MainViewModel", "Legacy FCM Broadcast success: $responseBody")
                            
                            saveAndBroadcastNotification(title, body)

                            viewModelScope.launch(Dispatchers.Main) {
                                onFinished(true, "Successfully sent legacy broadcast push message via deprecated FCM endpoint!")
                            }
                        } else {
                            _fcmBroadcastStatus.value = "Legacy Failed: Code ${response.code}"
                            Log.e("MainViewModel", "Legacy FCM Broadcast failed: ${response.code} $responseBody")
                            viewModelScope.launch(Dispatchers.Main) {
                                onFinished(false, "Legacy FCM Server returned error code ${response.code}: $responseBody")
                            }
                        }
                    }
                } catch (e: Exception) {
                    _fcmBroadcastStatus.value = "Network Error"
                    Log.e("MainViewModel", "Legacy FCM network failure: ${e.message}")
                    viewModelScope.launch(Dispatchers.Main) {
                        onFinished(false, "Network error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
    }

    // ─── LOGIN AND PROFILE HANDLERS ───

    fun loginUser(phone: String) {
        _isLoggingIn.value = true
        _loginError.value = null
        _registrationActionNeeded.value = false
        _registrationPhone.value = ""
        
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.login(com.example.network.LoginRequest(phone))
                if (response.success && response.data != null) {
                    val data = response.data
                    if (data.action == "login") {
                        val token = data.token ?: ""
                        com.example.network.MikekaApiClient.authToken = token
                        repository.setAuthToken(token)
                        
                        data.user?.let { profile ->
                            _userProfile.value = profile
                            repository.setUserProfileName(profile.name)
                            repository.setUserProfilePhone(profile.phoneNumber)
                        }
                        
                        _userSubscription.value = data.subscription
                        _isUserLoggedIn.value = true
                        
                        Log.d("MainViewModel", "Login successful! Welcome ${data.user?.name}")
                        loadTodayPremiumSlips()
                    } else if (data.action == "need_confirmation") {
                        _registrationPhone.value = phone
                        _registrationActionNeeded.value = true
                    }
                } else {
                    _loginError.value = response.error ?: "Failed to log in. Please try again."
                }
            } catch (e: Exception) {
                _loginError.value = "Network error: ${e.localizedMessage ?: "Check your connection"}"
                Log.e("MainViewModel", "Login exception: ${e.message}")
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun registerUser(phone: String, name: String) {
        _isLoggingIn.value = true
        _loginError.value = null
        
        val deviceId = android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_android"
        
        val deviceName = android.os.Build.MODEL ?: "Android Device"
        val currentFcm = if (_fcmToken.value.startsWith("fcm") || _fcmToken.value.contains("loading")) null else _fcmToken.value

        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.register(
                    com.example.network.RegisterRequest(
                        phone = phone,
                        name = name,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        platform = "android",
                        fcmToken = currentFcm
                    )
                )
                if (response.success && response.data != null) {
                    val data = response.data
                    val token = data.token ?: ""
                    com.example.network.MikekaApiClient.authToken = token
                    repository.setAuthToken(token)
                    
                    data.user?.let { profile ->
                        _userProfile.value = profile
                        repository.setUserProfileName(profile.name)
                        repository.setUserProfilePhone(profile.phoneNumber)
                    }
                    _userSubscription.value = data.subscription
                    _isUserLoggedIn.value = true
                    _registrationActionNeeded.value = false
                    Log.d("MainViewModel", "Register successful! Profile: ${data.user?.name}")
                    loadTodayPremiumSlips()
                } else {
                    _loginError.value = response.error ?: "Failed to register profile. Try again."
                }
            } catch (e: Exception) {
                _loginError.value = "Registration network error: ${e.localizedMessage ?: "Verify internet connection"}"
                Log.e("MainViewModel", "Registration error: ${e.message}")
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            try {
                com.example.network.MikekaApiClient.apiService.logout()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Logout api call error: ${e.message}")
            } finally {
                com.example.network.MikekaApiClient.authToken = null
                repository.clearAuthToken()
                _userProfile.value = null
                _userSubscription.value = null
                _isUserLoggedIn.value = false
                _premiumSlips.value = emptyList()
            }
        }
    }

    fun fetchMyProfileDetails() {
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getMyProfile()
                if (response.success && response.data != null) {
                    val profileData = response.data
                    
                    profileData.user?.let { parsedProfile ->
                        _userProfile.value = parsedProfile
                        repository.setUserProfileName(parsedProfile.name)
                        repository.setUserProfilePhone(parsedProfile.phoneNumber)
                    }

                    profileData.subscription?.let { parsedSub ->
                        _userSubscription.value = parsedSub
                        if (parsedSub.paymentStatus == "completed") {
                            loadTodayPremiumSlips()
                        }
                    } ?: run {
                        _userSubscription.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch profile details error: ${e.message}")
            }
        }
    }


    // ─── PREMIUM SLIPS AND OFFERS ───

    fun loadTodayPremiumSlips() {
        _isSlipsLoading.value = true
        _slipsError.value = null
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getTodayPremiumSlips()
                if (response.success && response.data != null) {
                    _premiumSlips.value = response.data.slips
                } else {
                    _slipsError.value = response.error ?: "Slips not found or subscriber auth required."
                }
            } catch (e: Exception) {
                _slipsError.value = "Oops! Connection delayed: ${e.localizedMessage}"
                Log.e("MainViewModel", "Fetch today slips error: ${e.message}")
            } finally {
                _isSlipsLoading.value = false
            }
        }
    }

    fun loadFreePreview() {
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getFreePreviewSlip()
                if (response.success && response.data != null) {
                    _freePreviewSlip.value = response.data.slip
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Free Preview fetch error: ${e.message}")
            }
        }
    }

    fun loadSubscriptionConfig() {
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getPortalConfig()
                if (response.success && response.data != null) {
                    _portalPackages.value = response.data.packages
                    response.data.supportPhone?.let { _supportPhone.value = it }
                    response.data.supportEmail?.let { _supportEmail.value = it }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "API config load failed: ${e.message}")
            }
        }
    }


    // ─── BILLING MOBILE MONEY CHECKOUT ───

    fun triggerCheckout(phone: String, amount: Int, packageType: String) {
        _activeOrder.value = null
        _paymentSuccess.value = null
        _checkoutError.value = null
        
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.createPaymentOrder(
                    com.example.network.CreateOrderRequest(phone, amount, packageType)
                )
                if (response.success && response.data != null) {
                    _activeOrder.value = response.data
                    startPollingOrderStatus(response.data.orderId)
                } else {
                    _checkoutError.value = response.error ?: "Checkout failed to initialize."
                }
            } catch (e: Exception) {
                _checkoutError.value = "Checkout connection error: ${e.message}"
            }
        }
    }

    private fun startPollingOrderStatus(orderId: String) {
        _isPaymentChecking.value = true
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 10) {
                delay(6000)
                try {
                    val statusResponse = com.example.network.MikekaApiClient.apiService.checkPaymentStatus(
                        com.example.network.CheckStatusRequest(orderId)
                    )
                    if (statusResponse.success && statusResponse.data != null) {
                        val txData = statusResponse.data
                        if (txData.isPaymentSuccessful) {
                            _paymentSuccess.value = true
                            _userSubscription.value = txData.subscription
                            loadTodayPremiumSlips()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Status poll attempt ${attempts} error: ${e.message}")
                }
                attempts++
            }
            if (_paymentSuccess.value != true) {
                _paymentSuccess.value = false
                _checkoutError.value = "Order timed out. Please approve the mobile pay request popup or double check network."
            }
            _isPaymentChecking.value = false
        }
    }

    fun checkOrderStatusDirectly(orderId: String) {
        _isPaymentChecking.value = true
        _checkoutError.value = null
        viewModelScope.launch {
            try {
                val statusResponse = com.example.network.MikekaApiClient.apiService.checkPaymentStatus(
                    com.example.network.CheckStatusRequest(orderId)
                )
                if (statusResponse.success && statusResponse.data != null) {
                    val txData = statusResponse.data
                    if (txData.isPaymentSuccessful) {
                        _paymentSuccess.value = true
                        _userSubscription.value = txData.subscription
                        loadTodayPremiumSlips()
                    } else {
                        _checkoutError.value = "Transaction pending or failed. Please check phone alert screen."
                    }
                } else {
                    _checkoutError.value = statusResponse.error ?: "Failed checking transaction status."
                }
            } catch (e: Exception) {
                _checkoutError.value = "Status check network error: ${e.message}"
            } finally {
                _isPaymentChecking.value = false
            }
        }
    }


    // ─── FORECASTERS AND FOLLOW SYSTEM ───

    fun loadTipsterLeaderboard() {
        _isTipstersLoading.value = true
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getLeaderboard()
                if (response.success && response.data != null) {
                    _tipsterLeaderboard.value = response.data.leaderboard
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Leaderboard fetch error: ${e.message}")
            } finally {
                _isTipstersLoading.value = false
            }
        }
    }

    fun loadTipsterDetail(id: Int) {
        _isTipstersLoading.value = true
        _selectedTipsterDetail.value = null
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getTipsterDetail(id)
                if (response.success && response.data != null) {
                    _selectedTipsterDetail.value = response.data
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Tipster detail error: ${e.message}")
            } finally {
                _isTipstersLoading.value = false
            }
        }
    }

    fun toggleFollowTipster(id: Int) {
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.toggleFollow(
                    com.example.network.FollowRequest(id)
                )
                if (response.success && response.data != null) {
                    val currentList = _tipsterLeaderboard.value.map { item ->
                        if (item.id == id) {
                            val nextFollowCount = (item.followerCount ?: 0) + (if (response.data.isFollowing) 1 else -1)
                            item.copy(
                                isFollowing = response.data.isFollowing,
                                followerCount = maxOf(0, nextFollowCount)
                            )
                        } else item
                    }
                    _tipsterLeaderboard.value = currentList
                    
                    val currentDetail = _selectedTipsterDetail.value
                    if (currentDetail != null && currentDetail.tipster.id == id) {
                        val nextFollowCount = (currentDetail.tipster.followerCount ?: 0) + (if (response.data.isFollowing) 1 else -1)
                        _selectedTipsterDetail.value = currentDetail.copy(
                            tipster = currentDetail.tipster.copy(
                                isFollowing = response.data.isFollowing,
                                followerCount = maxOf(0, nextFollowCount)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Toggle follow match exception: ${e.message}")
            }
        }
    }


    // ─── CHAT CHANNELS ───

    fun loadChatMessages() {
        _isChatLoading.value = true
        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.getChatMessages(limit = 100)
                if (response.success && response.data != null) {
                    _chatMessages.value = response.data.messages
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Chat fetch error: ${e.message}")
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun sendChatMessage(msgText: String) {
        if (msgText.isBlank()) return
        
        val selfProfile = _userProfile.value
        val tempMsg = com.example.network.ChatMessage(
            id = -System.currentTimeMillis().toInt(),
            senderId = selfProfile?.id ?: 99,
            receiverId = 1,
            message = msgText,
            senderType = "user",
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        _chatMessages.value = _chatMessages.value + tempMsg
        _isChatSending.value = true

        viewModelScope.launch {
            try {
                val response = com.example.network.MikekaApiClient.apiService.sendChatMessage(
                    com.example.network.SendMessageBody(msgText)
                )
                if (response.success && response.data != null) {
                    loadChatMessages()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Send chat error: ${e.message}")
                _chatMessages.value = _chatMessages.value.filter { it.id != tempMsg.id }
            } finally {
                _isChatSending.value = false
            }
        }
    }
}
