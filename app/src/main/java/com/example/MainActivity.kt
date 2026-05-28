package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdminControlSection
import com.example.ui.components.CustomWebView
import com.example.ui.components.NotificationsScreen
import com.example.ui.components.SkeletalLoader
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppLayout() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    
    // Core VM states
    val curScreen by viewModel.activeScreen.collectAsState()
    val curUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadProgress by viewModel.loadProgress.collectAsState()
    val feedList by viewModel.allNotifications.collectAsState()

    // Reference pointer to the persistent WebView
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Track browser back/forward capabilities dynamically
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Request notification permission launchpad (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Cloud Alerts notifications configured successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        AppLauncherSplash {
            showSplash = false
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .clickable {
                                    if (curScreen == AppScreen.ADMIN_PANEL) {
                                        viewModel.navigateTo(AppScreen.WEB_VIEW)
                                    } else {
                                        viewModel.navigateTo(AppScreen.ADMIN_PANEL)
                                    }
                                }
                        ) {
                            Text(
                                "MKEKA WAKO APP",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // All action icons removed as they come directly from the web app
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.statusBarsPadding()
                )

                // Inline loading progress bar
                AnimatedVisibility(visible = curScreen == AppScreen.WEB_VIEW && isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .testTag("web_loading_progress_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Keep WebView always alive inside tree to retain scroll focus and history stack, 
            // but conditionally show and hide it in the viewport relative to active tab screen.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (curScreen == AppScreen.WEB_VIEW) 1f else 0f)
            ) {
                CustomWebView(
                    url = curUrl,
                    onProgressChanged = { progress ->
                        viewModel.setWebProgress(progress)
                    },
                    onPageStarted = {
                        viewModel.setWebLoading(true)
                    },
                    onPageFinished = {
                        viewModel.setWebLoading(false)
                        // Retrieve dynamic web backstack capabilities
                        canGoBack = webViewRef?.canGoBack() ?: false
                        canGoForward = webViewRef?.canGoForward() ?: false
                    },
                    onWebViewCreated = { webView ->
                        webViewRef = webView
                    }
                )

                // Display material shimmer skeletal loader overlay exclusively while WebView is loading the HTML/JS resources!
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SkeletalLoader(modifier = Modifier.fillMaxSize())
                }
            }

            // Screen 2: Notification inbox logs
            AnimatedVisibility(
                visible = curScreen == AppScreen.NOTIFICATIONS,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 })
            ) {
                NotificationsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Screen 3: Config Master Administration
            AnimatedVisibility(
                visible = curScreen == AppScreen.ADMIN_PANEL,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 })
            ) {
                AdminControlSection(
                    viewModel = viewModel,
                    webView = webViewRef,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Modern Floating Notification Button at Bottom Right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (curScreen == AppScreen.NOTIFICATIONS) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable {
                            if (curScreen == AppScreen.NOTIFICATIONS) {
                                viewModel.navigateTo(AppScreen.WEB_VIEW)
                            } else {
                                viewModel.navigateTo(AppScreen.NOTIFICATIONS)
                            }
                        }
                        .testTag("floating_notification_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (curScreen == AppScreen.NOTIFICATIONS) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = if (curScreen == AppScreen.NOTIFICATIONS) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    if (curScreen != AppScreen.NOTIFICATIONS && feedList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                        ) {
                            Badge {
                                Text(
                                    text = feedList.size.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.Red, shape = CircleShape)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides Color.White
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
fun AppLauncherSplash(onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrance spring and fade animation
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = LinearOutSlowInEasing)
            )
        }
        // Let user see the stunning golden launcher icon
        delay(2500)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, easing = FastOutLinearInEasing)
        )
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303)),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow aura matching Mkeka's neon green
        Box(
            modifier = Modifier
                .size(350.dp)
                .graphicsLayer {
                    scaleX = scale.value * 1.6f
                    scaleY = scale.value * 1.6f
                    this.alpha = alpha.value * 0.18f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
        ) {
            // High-contrast gold highlighted frame holding our brand logo image
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFF10B981), Color(0xFFFFD700))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(3.dp)
                    .background(Color.Black, shape = RoundedCornerShape(29.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_1779967034974),
                    contentDescription = "MKEKA WAKO launcher icon",
                    modifier = Modifier.size(148.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = "MKEKA WAKO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            Text(
                text = "BET SMART • WIN BIG",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
                modifier = Modifier.alpha(0.85f)
            )
        }
    }
}



