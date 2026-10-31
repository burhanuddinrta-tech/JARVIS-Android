package com.burhanuddin.jarvis

import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
                            Surface(color = Color(0xFF103044), shape = RoundedCornerShape(30.dp)) {
                                Text("● $status", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = ReactorCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Text("SECURE CONNECTION  •  INTERNET REQUIRED  •  OFFLINE AI NOT INSTALLED", Modifier.fillMaxWidth(), color = Color(0xFF58788C), fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
            }
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
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.43f
        drawCircle(Color(0xFF123C52), radius = radius * 1.12f, center = c, style = Stroke(width = radius * 0.10f))
        drawCircle(ReactorCyan.copy(alpha = 0.22f), radius = radius * 0.94f, center = c, style = Stroke(width = radius * 0.035f))
        drawCircle(ReactorCyan, radius = radius * 0.78f, center = c, style = Stroke(width = radius * 0.055f))
        drawCircle(Color(0xFF1A7892), radius = radius * 0.58f, center = c, style = Stroke(width = radius * 0.09f))
        drawCircle(ReactorCyan.copy(alpha = 0.18f), radius = radius * 0.40f, center = c)
        drawCircle(ReactorCyan, radius = radius * 0.19f, center = c)
        drawCircle(Color.White.copy(alpha = 0.85f), radius = radius * 0.075f, center = c)
        drawArc(ReactorCyan, -65f, 95f, false, topLeft = Offset(c.x - radius, c.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(width = radius * 0.09f))
        drawArc(Color(0xFF2A91AA), 115f, 95f, false, topLeft = Offset(c.x - radius, c.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(width = radius * 0.07f))
    }
}
