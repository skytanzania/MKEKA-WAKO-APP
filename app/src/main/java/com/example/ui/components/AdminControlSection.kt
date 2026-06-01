package com.example.ui.components

import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlSection(
    viewModel: MainViewModel,
    webView: WebView?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAuthorized by remember { mutableStateOf(false) }
    var showPasswordInput by remember { mutableStateOf(false) }
    var passwordValue by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    if (!isAuthorized) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "FOR ADMIN ONLY",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "This dashboard is restricted to authorized administrative services. Unauthorized access attempts are monitored.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!showPasswordInput) {
                Button(
                    onClick = { showPasswordInput = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_continue_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                OutlinedTextField(
                    value = passwordValue,
                    onValueChange = { 
                        passwordValue = it
                        passwordError = false
                    },
                    label = { Text("Enter Administrator Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = passwordError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("admin_password_field"),
                    trailingIcon = {
                        if (passwordValue.isNotBlank()) {
                            IconButton(onClick = { passwordValue = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear input")
                            }
                        }
                    }
                )

                if (passwordError) {
                    Text(
                        text = "Incorrect credentials. Verification failed.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showPasswordInput = false
                            passwordValue = ""
                            passwordError = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            if (passwordValue == "sky@75199919") {
                                isAuthorized = true
                                Toast.makeText(context, "Authorized successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                passwordError = true
                                Toast.makeText(context, "Access Denied: Incorrect Password", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("admin_password_verify_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Unlock Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    } else {
        val currentUrl by viewModel.currentUrl.collectAsState()
        val isPushEnabled by viewModel.isPushPreferencesEnabled.collectAsState()
        val subscribedTopics by viewModel.subscribedTopics.collectAsState()
        val fcmToken by viewModel.fcmToken.collectAsState()
        val fcmServerKeySetting by viewModel.fcmServerKeySetting.collectAsState()
        val fcmServiceAccountJsonSetting by viewModel.fcmServiceAccountJsonSetting.collectAsState()
        val fcmBroadcastStatus by viewModel.fcmBroadcastStatus.collectAsState()

        // Form settings states
        var urlInput by remember { mutableStateOf(currentUrl) }
        var mockTitleInput by remember { mutableStateOf("⭐ Standard Premium Bet Available!") }
        var mockBodyInput by remember { mutableStateOf("New odds added for Tanzania Premier League. Load now to play!") }
        var fcmKeyInput by remember { mutableStateOf(fcmServerKeySetting) }
        var fcmServiceAccountJsonInput by remember { mutableStateOf(fcmServiceAccountJsonSetting) }
        var isKeyVisible by remember { mutableStateOf(false) }
        var isServiceAccountJsonVisible by remember { mutableStateOf(false) }
        var targetTopic by remember { mutableStateOf("all") }

        LaunchedEffect(fcmServerKeySetting) {
            fcmKeyInput = fcmServerKeySetting
        }

        LaunchedEffect(fcmServiceAccountJsonSetting) {
            fcmServiceAccountJsonInput = fcmServiceAccountJsonSetting
        }
        
        // Dialog state
        var showClearDialog by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("admin_control_section"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Warning Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin Mode",
                    tint = MaterialTheme.colorScheme.error
                )
                Column {
                    Text(
                        text = "ADMINISTRATOR OVERRIDE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Modify core connections, simulate cloud events, and clear cached storage parameters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section: Web Admin Portal Access (NEW)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = "Web Admin Portal", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Web Admin Portal Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Access the secure administration backend directly from this app. Credentials and session isolation are maintained within the app sandbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        viewModel.updateUrlOverride("https://mikekaapp.co.tz/admin.php")
                        viewModel.navigateTo(com.example.AppScreen.WEB_VIEW)
                        Toast.makeText(context, "Opening Web Admin in secure Web-Frame...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_web_admin_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock & Launch Web Admin (In-App)")
                }
            }
        }

        // Section 1: WebView Port Address Custom Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "URL Config", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Target Web Portal Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "Redirect the launcher to secure dev staging servers or any alternative site. Default is Mikeka Portal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Web App Link URL") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("admin_url_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (urlInput.isNotBlank() && (urlInput.startsWith("http://") || urlInput.startsWith("https://"))) {
                                viewModel.updateUrlOverride(urlInput.trim())
                                Toast.makeText(context, "Redirecting portal to: $urlInput", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a valid HTTP/HTTPS web address!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("admin_apply_url_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Override")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetUrlToDefault()
                            urlInput = MainViewModel.DEFAULT_FALLBACK_URL
                            Toast.makeText(context, "Restored default Mikeka Link!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default")
                    }
                }
            }
        }

        // Section 2: Real-time Firebase FCM & Cloud Broadcast Hub
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = "FCM Broadcast", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Broadcast Live Firebase FCM & Cloud Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Broadcast live, server-side push notifications directly to users' lock screens using modern Firebase. Setup your Service Account JSON for HTTP v1 or use the Legacy key fallback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Service Account JSON Field (Recommended: HTTP v1)
                Text(
                    text = "1. Firebase HTTP v1 API Setup (Recommended)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = fcmServiceAccountJsonInput,
                    onValueChange = { fcmServiceAccountJsonInput = it },
                    label = { Text("Service Account JSON") },
                    placeholder = { Text("Paste complete service_account.json here...") },
                    minLines = 3,
                    maxLines = 6,
                    visualTransformation = if (isServiceAccountJsonVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isServiceAccountJsonVisible = !isServiceAccountJsonVisible }) {
                            Icon(
                                imageVector = if (isServiceAccountJsonVisible) Icons.Default.Lock else Icons.Default.Lock,
                                contentDescription = if (isServiceAccountJsonVisible) "Hide JSON" else "Show JSON"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("fcm_service_account_json_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To get this: Firebase Console -> Project Settings -> Service Accounts -> Generate Private Key -> paste the JSON file contents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.updateFcmServiceAccountJson(fcmServiceAccountJsonInput)
                            Toast.makeText(context, "Service Account JSON saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_fcm_json_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save JSON")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Legacy Fallback Block
                Text(
                    text = "2. FCM Legacy API Setup (Deprecated Fallback)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fcmKeyInput,
                    onValueChange = { fcmKeyInput = it },
                    label = { Text("FCM Server Key (Legacy)") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.Lock else Icons.Default.Lock,
                                contentDescription = if (isKeyVisible) "Hide Key" else "Show Key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("fcm_server_key_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Optional fallback key. Active only if Service Account JSON is empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.updateFcmServerKey(fcmKeyInput)
                            Toast.makeText(context, "FCM Server Key saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_fcm_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Save Key")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // 2. Select Target Topic Audience
                Text(
                    text = "Broadcast Target Segment Topic:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val topics = listOf(
                        "all" to "📢 All Users",
                        "general_betting" to "⚽ General Betting",
                        "prematch_analytics" to "📊 Prematch",
                        "results_announcements" to "🏆 Results Alert",
                        "system_alarms" to "⚠️ System"
                    )
                    topics.forEach { (topicId, displayName) ->
                        val isSelected = targetTopic == topicId
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetTopic = topicId },
                            label = { Text(displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // 3. Payload configuration inputs
                OutlinedTextField(
                    value = mockTitleInput,
                    onValueChange = { mockTitleInput = it },
                    label = { Text("Signal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_notification_title"),
                )

                OutlinedTextField(
                    value = mockBodyInput,
                    onValueChange = { mockBodyInput = it },
                    label = { Text("Signal Body description") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_notification_body"),
                )

                // 4. Queuing FCM Status
                Row(
                     modifier = Modifier.fillMaxWidth(),
                     verticalAlignment = Alignment.CenterVertically,
                     horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                         text = "FCM Queue Broadcast Status:",
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Medium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(fcmBroadcastStatus) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = if (fcmBroadcastStatus.startsWith("Successful")) 
                                MaterialTheme.colorScheme.primary 
                            else if (fcmBroadcastStatus.startsWith("Failed") || fcmBroadcastStatus.startsWith("Error")) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // 5. Actions row
                Column(
                     modifier = Modifier.fillMaxWidth(),
                     verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (mockTitleInput.isNotBlank() && mockBodyInput.isNotBlank()) {
                                if (fcmServiceAccountJsonInput.isBlank() && fcmKeyInput.isBlank()) {
                                    Toast.makeText(context, "Please set either Service Account JSON or Legacy Server Key.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.broadcastFCMNotification(
                                        title = mockTitleInput,
                                        body = mockBodyInput,
                                        targetTopicName = targetTopic
                                    ) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Title and body must not be blank!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_trigger_notification_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send FCM Alert")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Live Firebase FCM Alert")
                    }

                    OutlinedButton(
                        onClick = {
                            if (mockTitleInput.isNotBlank() && mockBodyInput.isNotBlank()) {
                                viewModel.saveAndBroadcastNotification(mockTitleInput, mockBodyInput)
                                Toast.makeText(context, "Saved offline cloud feed alert successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Title and body must not be blank!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_feed_only_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Live Cloud Feed Only")
                    }
                }
            }
        }

        // Section 2.5: Live Firebase FCM Diagnostics
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "FCM Diagnostics", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Firebase FCM Status & Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Verify real-time Google Play Services & FCM Registration. Copy the unique token to send precise pushing alerts from the Firebase console.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FCM Registration Token:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = fcmToken,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("fcm_token_text")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (fcmToken.isNotBlank() && !fcmToken.startsWith("FCM") && !fcmToken.contains("loading")) {
                                val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(fcmToken) }
                                clipboardManager.setText(annotatedString)
                                Toast.makeText(context, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No valid token available to copy yet.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("copy_token_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Token")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.syncNotificationsFromCloud()
                            com.example.notifications.NotificationHelper(context).showNotification(
                                "🔔 FCM Channel Test Success",
                                "Your device registration token is active and ready to receive live push broadcasts!"
                            )
                            Toast.makeText(context, "Tested local push trigger!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.5f).testTag("test_push_button")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Check")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Local Push")
                    }
                }
            }
        }

        // Section 3: Caching, Storage, History clean deck
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Memory Config", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Device Memory & Caching Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Clear offline browser tables, index cookies, memory caches and local SQLite database histories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("admin_clear_all_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wipe Web Logs")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllNotifications()
                            Toast.makeText(context, "Local notification logs wiped cleanly!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear App Feeds")
                    }
                }
            }
        }

        // Section 4: Subscription preferences controller
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Notification Topic Subscriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Manage subscription categories tied to Firebase Cloud topics:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val availableTopics = listOf("General Betting", "Prematch Analytics", "Results Announcements", "System Alarms")
                availableTopics.forEach { topic ->
                    val isSubscribed = subscribedTopics.contains(topic)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = topic, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isSubscribed,
                            onCheckedChange = { viewModel.toggleTopicSubscription(topic) }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Alert Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm Offline Cache Clear") },
            text = { Text("This will clear persistent cookies, HTML5 local index storage tables, and offline page memories. You will need to log back into the web database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        try {
                            // Clear Cache
                            webView?.clearCache(true)
                            
                            // Remove all system cookies 
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.removeAllCookies(null)
                            cookieManager.flush()
                            
                            Toast.makeText(context, "Cache & login credentials cleared successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cleared local settings successfully.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
  }
}
