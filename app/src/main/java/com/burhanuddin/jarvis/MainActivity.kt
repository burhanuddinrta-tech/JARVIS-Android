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
    private val reactorImageBase64 = "$reactor"

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
