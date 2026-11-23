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
import android.webkit.WebResourceError
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
    private val jarvisUrl = "https://jarvis-ai-independent.vercel.app/"
    private var cacheFallbackAttempted = false
    private val reactorImageBase64 = "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"

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

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame != true || view == null) return
                if (!cacheFallbackAttempted) {
                    cacheFallbackAttempted = true
                    view.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    view.loadUrl(jarvisUrl)
                } else {
                    showOfflineFallback(view)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.startsWith(jarvisUrl) == true) {
                    view?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                    cacheFallbackAttempted = false
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

        webView.loadUrl(jarvisUrl)
        checkForUpdates()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }


    private fun showOfflineFallback(view: WebView) {
        val html = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>J.A.R.V.I.S.</title>
              <style>
                * { box-sizing: border-box; }
                body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 24px; color: #eaf7ff; background: radial-gradient(ellipse at center, #102b3b 0%, #050a10 62%, #020407 100%); font-family: system-ui, sans-serif; text-align: center; }
                main { width: 100%; max-width: 430px; }
                img { display: block; width: 132px; height: 132px; object-fit: contain; margin: 0 auto 22px; filter: drop-shadow(0 0 20px #20bce977); }
                h1 { margin: 0; letter-spacing: .22em; font-size: 22px; }
                .sub { margin: 9px 0 34px; color: #82bfd6; font-size: 11px; letter-spacing: .2em; }
                section { padding: 22px; border: 1px solid #28576c; border-radius: 18px; background: #0a1722; }
                h2 { margin: 0 0 10px; font-size: 18px; }
                p { margin: 0; color: #a4b9c5; font-size: 14px; line-height: 1.65; }
                button { margin-top: 22px; padding: 14px 22px; border: 0; border-radius: 12px; background: #23a7cf; color: #031018; font-weight: 800; font-size: 14px; }
              </style>
            </head>
            <body>
              <main>
                <img src="data:image/webp;base64,$reactorImageBase64" alt="JARVIS arc reactor">
                <h1>J.A.R.V.I.S.</h1>
                <div class="sub">PERSONAL AI SYSTEM</div>
                <section>
                  <h2>Offline mode</h2>
                  <p>The JARVIS interface is ready, but Gemini needs an internet connection. Reconnect and try again to continue your chats.</p>
                </section>
                <button onclick="location.href='$jarvisUrl'">Retry connection</button>
              </main>
            </body>
            </html>
        """.trimIndent()
        view.loadDataWithBaseURL(jarvisUrl, html, "text/html", "UTF-8", null)
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
