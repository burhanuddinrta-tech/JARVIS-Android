package com.burhanuddin.jarvis

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val JARVIS_WEB_APP = "https://jarvis-ai-independent.vercel.app"

class MainActivity : ComponentActivity() {
    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadUrl(JARVIS_WEB_APP)
                    }
                }
            )
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        // The embedded web app owns its navigation; exit normally when no history remains.
        super.onBackPressed()
    }
}
