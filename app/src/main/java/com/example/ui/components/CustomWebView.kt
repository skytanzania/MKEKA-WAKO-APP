package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomWebView(
    url: String,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Remember the WebView instance so that it retains history, cookies and local storage 
    // seamlessly on composition transitions, configuration changes, or tab navigations!
    val rememberedWebView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Setup cache directories & cookie storage to stay logged in and run hot
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
                
                // Optimized long-term caching
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Adaptive layout controls
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Security policy
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onPageStarted()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onPageFinished()
                    CookieManager.getInstance().flush() // Flush state so users stay remembered for a long time
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val targetUrl = request?.url?.toString() ?: return false
                    
                    // Handle non-http actions (WhatsApp support links, dialing telephone, emailing admin)
                    if (targetUrl.startsWith("tel:") || 
                        targetUrl.startsWith("whatsapp:") || 
                        targetUrl.startsWith("mailto:") || 
                        targetUrl.startsWith("sms:")
                    ) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            return false
                        }
                    }
                    
                    // Force the link to render natively within our WebView frame
                    view?.loadUrl(targetUrl)
                    return true
                }
            }

            onWebViewCreated(this)
        }
    }

    // Load initial URL or React to admin overrides dynamically
    LaunchedEffect(url) {
        if (rememberedWebView.url != url) {
            rememberedWebView.loadUrl(url)
        }
    }

    AndroidView(
        factory = { rememberedWebView },
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_webview")
    )
}
