package com.burhanuddin.jarvis

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val updateExecutor = Executors.newSingleThreadExecutor()
    private var updateDialogShown = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileChooserCallback?.onReceiveValue(uris.toTypedArray())
        fileChooserCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 10, 16)
        window.navigationBarColor = Color.rgb(5, 10, 16)

        val root = FrameLayout(this)
        webView = WebView(this)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(progress, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(3)
        ))
        setContentView(root)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.setBackgroundColor(Color.rgb(5, 10, 16))
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "https" && uri.host == "jarvis-ai-independent.vercel.app") return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    true
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = callback
                return try {
                    filePicker.launch(arrayOf("*/*"))
                    true
                } catch (_: Exception) {
                    fileChooserCallback = null
                    false
                }
            }
        }

        webView.loadUrl("https://jarvis-ai-independent.vercel.app/")
        checkForUpdates()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }


    private fun checkForUpdates() {
        updateExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("https://api.github.com/repos/burhanuddinrta-tech/JARVIS-Android/releases/latest").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "JARVIS-Android")
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@execute

                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(payload)
                val latestVersion = release.optString("tag_name").removePrefix("v")
                @Suppress("DEPRECATION")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
                if (latestVersion.isBlank() || !isNewerVersion(latestVersion, currentVersion)) return@execute

                var updateUrl = release.optString("html_url")
                val assets = release.optJSONArray("assets")
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        val name = asset.optString("name")
                        val url = asset.optString("browser_download_url")
                        if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                            updateUrl = url
                            break
                        }
                    }
                }
                if (!updateUrl.startsWith("https://")) return@execute

                runOnUiThread {
                    if (isFinishing || isDestroyed || updateDialogShown) return@runOnUiThread
                    updateDialogShown = true
                    AlertDialog.Builder(this)
                        .setTitle("JARVIS update available")
                        .setMessage("Version $latestVersion is ready. Open the official release to get the latest improvements.")
                        .setPositiveButton("Update") { _, _ ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                            } catch (_: Exception) {
                                // Keep the app usable if no browser can open the release link.
                            }
                        }
                        .setNegativeButton("Later", null)
                        .setOnDismissListener { updateDialogShown = false }
                        .show()
                }
            } catch (_: Exception) {
                // Update checks are best-effort and never block JARVIS from opening.
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        fun parts(value: String): List<Int> =
            value.removePrefix("v").split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

        val latestParts = parts(latest)
        val currentParts = parts(current)
        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val left = latestParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        updateExecutor.shutdownNow()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
