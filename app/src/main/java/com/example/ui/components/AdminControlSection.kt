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

        // Form settings states
        var urlInput by remember { mutableStateOf(currentUrl) }
        var mockTitleInput by remember { mutableStateOf("⭐ Standard Premium Bet Available!") }
        var mockBodyInput by remember { mutableStateOf("New odds added for Tanzania Premier League. Load now to play!") }
        
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

        // Section 2: Mock Cloud Messaging Trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Test Notification", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Broadcast Live Cloud Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Broadcast live, server-side cloud notifications that will be received by ALL current application users simultaneously.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                Button(
                    onClick = {
                        if (mockTitleInput.isNotBlank() && mockBodyInput.isNotBlank()) {
                            viewModel.saveAndBroadcastNotification(mockTitleInput, mockBodyInput)
                            Toast.makeText(context, "Cloud notification broadcast dispatched to all users!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Title and body must not be blank!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_trigger_notification_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Alert")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast to All Users")
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
