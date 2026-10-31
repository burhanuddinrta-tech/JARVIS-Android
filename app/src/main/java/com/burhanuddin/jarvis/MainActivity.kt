package com.burhanuddin.jarvis

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val JARVIS_API = "https://openmind-ai-ursbf6.v2.appdeploy.ai/api/chat"

data class ChatMessage(val role: String, val content: String)

private val DeepNavy = Color(0xFF050D19)
private val PanelNavy = Color(0xFF0B1B2C)
private val ReactorCyan = Color(0xFF51E7FF)
private val SoftText = Color(0xFFD9F4FF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            val messages = remember { mutableStateListOf<ChatMessage>() }
            var input by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("ONLINE") }
            var updateStatus by remember { mutableStateOf("Updates not checked") }
            var updateUrl by remember { mutableStateOf<String?>(null) }
            var checkingUpdate by remember { mutableStateOf(false) }

            MaterialTheme(colorScheme = darkColorScheme(
                primary = ReactorCyan, background = DeepNavy, surface = PanelNavy,
                onPrimary = DeepNavy, onBackground = SoftText, onSurface = SoftText
            )) {
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF0A1B2C), DeepNavy, Color(0xFF020711)))
                )) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF102D43)), contentAlignment = Alignment.Center) {
                                ArcReactor(Modifier.size(38.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("J.A.R.V.I.S.", color = ReactorCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text("PERSONAL AI SYSTEM", color = Color(0xFF8BAFC2), fontSize = 9.sp, letterSpacing = 2.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(color = Color(0xFF103044), shape = RoundedCornerShape(30.dp)) {
                                    Text("● $status", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = ReactorCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = {
                                        checkingUpdate = true
                                        updateStatus = "Checking for updates..."
                                        updateUrl = null
                                        lifecycleScope.launch {
                                            val result = withContext(Dispatchers.IO) { findJarvisUpdate() }
                                            updateStatus = result.first
                                            updateUrl = result.second
                                            checkingUpdate = false
                                        }
                                    },
                                    enabled = !checkingUpdate,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) { Text(if (checkingUpdate) "CHECKING..." else "CHECK UPDATE", color = ReactorCyan, fontSize = 9.sp) }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        if (messages.isEmpty()) {
                            Column(
                                Modifier.weight(1f).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ArcReactor(Modifier.size(190.dp))
                                Spacer(Modifier.height(24.dp))
                                Text("SYSTEM ONLINE", color = ReactorCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Good day. I'm JARVIS.", color = SoftText, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text("How may I assist you today?", color = Color(0xFF91AFC0), fontSize = 14.sp)
                                Spacer(Modifier.height(18.dp))
                                Text("ONLINE AI  •  VOICE OUTPUT", color = Color(0xFF64899E), fontSize = 10.sp, letterSpacing = 1.5.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(messages) { message ->
                                    val user = message.role == "user"
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
                                        Text(if (user) "YOU" else "JARVIS", color = ReactorCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Surface(
                                            color = if (user) Color(0xFF10364A) else Color(0xFF102235),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.widthIn(max = 320.dp)
                                        ) {
                                            Column(Modifier.padding(14.dp)) {
                                                Text(message.content, color = SoftText, fontSize = 15.sp)
                                                if (!user) {
                                                    Spacer(Modifier.height(8.dp))
                                                    TextButton(onClick = { speak(message.content) }) { Text("▶  READ ALOUD", color = ReactorCyan, fontSize = 10.sp) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (busy) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(17.dp), color = ReactorCyan, strokeWidth = 2.dp)
                                Text("  Processing your request...", color = Color(0xFF91AFC0), fontSize = 12.sp)
                            }
                        }
                        Surface(color = Color(0xFF0B1B2C), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D5369))) {
                            Column(Modifier.padding(10.dp)) {
                                OutlinedTextField(
                                    value = input, onValueChange = { input = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Ask JARVIS anything...", color = Color(0xFF7896A8)) },
                                    shape = RoundedCornerShape(14.dp), maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = SoftText, unfocusedTextColor = SoftText,
                                        cursorColor = ReactorCyan
                                    )
                                )
                                Button(
                                    onClick = {
                                        val question = input.trim()
                                        if (question.isNotEmpty() && !busy) {
                                            input = ""
                                            messages.add(ChatMessage("user", question))
                                            busy = true
                                            status = "LINKING"
                                            lifecycleScope.launch {
                                                val answer = withContext(Dispatchers.IO) { requestJarvis(messages.toList()) }
                                                messages.add(ChatMessage("assistant", answer))
                                                busy = false
                                                status = if (answer.startsWith("Connection issue:")) "OFFLINE" else "ONLINE"
                                            }
                                        }
                                    },
                                    enabled = input.isNotBlank() && !busy,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ReactorCyan, contentColor = DeepNavy),
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text("TRANSMIT  ➜", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (updateStatus != "Updates not checked") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(updateStatus, Modifier.weight(1f), color = if (updateUrl != null) ReactorCyan else Color(0xFF91AFC0), fontSize = 10.sp)
                                if (updateUrl != null) {
                                    TextButton(onClick = {
                                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))) }
                                    }) { Text("DOWNLOAD", color = ReactorCyan, fontSize = 10.sp) }
                                }
                            }
                        }
                        Text("SECURE CONNECTION  •  INTERNET REQUIRED  •  OFFLINE AI NOT INSTALLED", Modifier.fillMaxWidth(), color = Color(0xFF58788C), fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }

    private fun findJarvisUpdate(): Pair<String, String?> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL("https://api.github.com/repos/burhanuddinrta-tech/JARVIS-Android/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "JARVIS-Android")
            }
            if (connection.responseCode !in 200..299) return "Unable to check updates right now." to null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latest = json.optString("tag_name").removePrefix("v")
            val current = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty().removePrefix("v")
            fun parts(value: String) = value.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
            val a = parts(latest)
            val b = parts(current)
            val newer = (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { i ->
                val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
                if (diff == 0) null else diff > 0
            } ?: false
            val assets = json.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
                    if (apkUrl != null) break
                }
            }
            when {
                !newer -> "JARVIS is up to date (v$current)." to null
                apkUrl == null -> "A new version is listed, but its APK is not available yet." to null
                else -> "Update available: v$latest" to apkUrl
            }
        } catch (_: Exception) {
            "Update check failed. Please try again later." to null
        } finally {
            connection?.disconnect()
        }
    }

    private fun requestJarvis(history: List<ChatMessage>): String {
        var connection: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("messages", JSONArray().apply {
                    history.takeLast(12).forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
                })
                put("style", "Balanced")
                put("mode", "Chat")
            }
            connection = (URL(JARVIS_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val trimmed = response.trimStart()
            if (!trimmed.startsWith("{")) {
                val preview = trimmed.take(100).replace("\n", " ")
                return "Connection issue: The JARVIS server returned a webpage instead of JSON (HTTP $code). Response: $preview"
            }
            val json = try { JSONObject(response) } catch (_: JSONException) {
                return "Connection issue: The server response was not valid JSON (HTTP $code)."
            }
            if (code in 200..299) {
                json.optString("reply").ifBlank { "I received an empty response from the AI service." }
            } else {
                "Connection issue: " + json.optString("error", "The AI service returned error $code.")
            }
        } catch (e: Exception) {
            "Connection issue: " + (e.message ?: "Unable to reach the JARVIS service.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }
    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.UK
    }
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun ArcReactor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "reactor")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "outerRotation"
    )
    val counterRotation by transition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(6200, easing = LinearEasing)),
        label = "innerRotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corePulse"
    )
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.43f
        val ring = androidx.compose.ui.geometry.Size(r * 2, r * 2)
        val top = Offset(c.x - r, c.y - r)

        // Machined outer housing and concentric energy channels.
        drawCircle(Color(0xFF142D40), r * 1.12f, c, style = Stroke(r * 0.13f))
        drawCircle(Color(0xFF42677A), r * 1.04f, c, style = Stroke(r * 0.025f))
        drawCircle(ReactorCyan.copy(alpha = 0.20f), r * 0.94f, c, style = Stroke(r * 0.035f))
        drawCircle(Color(0xFF1B536B), r * 0.78f, c, style = Stroke(r * 0.11f))
        drawCircle(ReactorCyan.copy(alpha = 0.24f), r * 0.61f, c, style = Stroke(r * 0.035f))

        // Segmented rotating turbine rings.
        rotate(rotation, c) {
            for (i in 0 until 12) {
                drawArc(
                    color = if (i % 3 == 0) ReactorCyan else Color(0xFF477D91),
                    startAngle = i * 30f + 3f, sweepAngle = 17f, useCenter = false,
                    topLeft = top, size = ring, style = Stroke(r * 0.045f)
                )
            }
            for (i in 0 until 24) {
                val a = Math.toRadians((i * 15.0))
                val inner = r * 0.98f
                val outer = r * (if (i % 3 == 0) 1.08f else 1.035f)
                val p1 = Offset(c.x + kotlin.math.cos(a).toFloat() * inner, c.y + kotlin.math.sin(a).toFloat() * inner)
                val p2 = Offset(c.x + kotlin.math.cos(a).toFloat() * outer, c.y + kotlin.math.sin(a).toFloat() * outer)
                drawLine(if (i % 3 == 0) ReactorCyan else Color(0xFF66899A), p1, p2, strokeWidth = r * 0.025f)
            }
        }
        rotate(counterRotation, c) {
            drawArc(ReactorCyan, 205f, 72f, false, top, ring, style = Stroke(r * 0.075f))
            drawArc(Color(0xFF3B91A8), 25f, 72f, false, top, ring, style = Stroke(r * 0.06f))
            for (i in 0 until 6) {
                val a = Math.toRadians((i * 60.0))
                val p = Offset(c.x + kotlin.math.cos(a).toFloat() * r * 0.48f, c.y + kotlin.math.sin(a).toFloat() * r * 0.48f)
                drawCircle(Color(0xFF8CEEFF), r * 0.035f, p)
            }
        }
        drawCircle(Color(0xFF071522), r * 0.38f, c, style = Stroke(r * 0.055f))
        drawCircle(ReactorCyan.copy(alpha = 0.12f), r * 0.31f * pulse, c)
        drawCircle(ReactorCyan.copy(alpha = 0.85f), r * 0.19f * pulse, c)
        drawCircle(Color.White.copy(alpha = 0.92f), r * 0.065f * pulse, c)
    }
}
