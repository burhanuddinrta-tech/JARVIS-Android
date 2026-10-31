package com.burhanuddin.jarvis

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val JARVIS_API = "https://openmind-ai-ursbf6.v2.appdeploy.ai/api/chat"

data class ChatMessage(val role: String, val content: String)

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
            var status by remember { mutableStateOf("ONLINE MODE") }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38D9F5),
                    background = Color(0xFF07111F),
                    surface = Color(0xFF101E30),
                    onPrimary = Color(0xFF001018),
                    onBackground = Color(0xFFEAF7FF),
                    onSurface = Color(0xFFEAF7FF)
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF07111F)) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("J.A.R.V.I.S.", color = Color(0xFF55E5FF), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Text("PERSONAL AI ASSISTANT", color = Color(0xFF8DAFC2), fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            Text(status, color = Color(0xFF55E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (messages.isEmpty()) {
                                item {
                                    Surface(color = Color(0xFF10263A), shape = RoundedCornerShape(18.dp)) {
                                        Column(Modifier.padding(18.dp)) {
                                            Text("Good day. JARVIS is ready.", color = Color(0xFF55E5FF), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "This first native build uses your existing online JARVIS service. An offline AI model is not installed in this version.",
                                                color = Color(0xFFD5E5EF), fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            items(messages) { message ->
                                val isUser = message.role == "user"
                                Column(
                                    Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        color = if (isUser) Color(0xFF123C52) else Color(0xFF142438),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(Modifier.padding(13.dp)) {
                                            Text(if (isUser) "YOU" else "JARVIS", color = Color(0xFF55DFF5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(5.dp))
                                            Text(message.content, color = Color(0xFFEAF7FF), fontSize = 15.sp)
                                            if (!isUser) {
                                                Spacer(Modifier.height(8.dp))
                                                Button(onClick = { speak(message.content) }) { Text("Read aloud") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (busy) {
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF55E5FF), strokeWidth = 2.dp)
                                Text("  JARVIS is thinking...", color = Color(0xFF9BC9D8), fontSize = 13.sp)
                            }
                        }

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Message JARVIS...") },
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 4
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val question = input.trim()
                                if (question.isNotEmpty() && !busy) {
                                    input = ""
                                    messages.add(ChatMessage("user", question))
                                    busy = true
                                    status = "CONNECTING"
                                    lifecycleScope.launch {
                                        val answer = withContext(Dispatchers.IO) { requestJarvis(messages.toList()) }
                                        messages.add(ChatMessage("assistant", answer))
                                        busy = false
                                        status = if (answer.startsWith("Connection issue:")) "CHECK CONNECTION" else "ONLINE MODE"
                                    }
                                }
                            },
                            enabled = input.isNotBlank() && !busy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38D9F5), contentColor = Color(0xFF001018))
                        ) {
                            Text("SEND", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Online AI requires an internet connection. Offline AI is not included yet.", color = Color(0xFF7894A6), fontSize = 10.sp)
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
                    history.takeLast(12).forEach { message ->
                        put(JSONObject().put("role", message.role).put("content", message.content))
                    }
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
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(response)
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
