package com.burhanuddin.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class ChatLine(val role: String, val text: String)
private const val CHAT_ENDPOINT = "https://jarvis-ai-independent.vercel.app/api/chat"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("jarvis_local", MODE_PRIVATE)
        val saved = runCatching { JSONArray(prefs.getString("messages", "[]")) }.getOrDefault(JSONArray())
        val initial = buildList {
            for (i in 0 until saved.length()) {
                val o = saved.optJSONObject(i) ?: continue
                add(ChatLine(o.optString("role"), o.optString("text")))
            }
        }
        setContent {
            var lines by remember { mutableStateOf(initial) }
            var draft by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("Offline-ready · Gemini connects when online") }
            val bg = Color(0xFF090812)
            val panel = Color(0xFF171426)
            fun persist(items: List<ChatLine>) {
                val arr = JSONArray()
                items.takeLast(100).forEach { arr.put(JSONObject().put("role", it.role).put("text", it.text)) }
                prefs.edit().putString("messages", arr.toString()).apply()
            }
            MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = Color(0xFF9B7CFF))) {
                Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◉", color = Color(0xFF9B7CFF), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("JARVIS AI", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text(status, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { lines = emptyList(); persist(lines) }) { Text("New chat") }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                        items(lines) { line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.role == "user") Arrangement.End else Arrangement.Start) {
                                Surface(color = if (line.role == "user") Color(0xFF30264F) else panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(vertical = 5.dp).widthIn(max = 310.dp)) {
                                    Text(line.text, color = Color.White, modifier = Modifier.padding(13.dp))
                                }
                            }
                        }
                        if (lines.isEmpty()) item {
                            Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                                Column(Modifier.padding(18.dp)) {
                                    Text("At your command.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Text("Your conversations are stored on this phone. Gemini replies need an internet connection.", color = Color.LightGray)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(24.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(value = draft, onValueChange = { draft = it }, enabled = !busy, modifier = Modifier.weight(1f).padding(10.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), decorationBox = { inner -> if (draft.isEmpty()) Text("Message JARVIS…", color = Color.Gray); inner() })
                        IconButton(enabled = !busy, onClick = {
                            val msg = draft.trim()
                            if (msg.isNotEmpty()) {
                                val history = lines + ChatLine("user", msg)
                                lines = history
                                draft = ""
                                persist(history)
                                busy = true
                                status = "Connecting to Gemini…"
                                lifecycleScope.launch {
                                    val answer = withContext(Dispatchers.IO) { requestGemini(history) }
                                    val updated = history + ChatLine("assistant", answer)
                                    lines = updated
                                    persist(updated)
                                    busy = false
                                    status = if (answer.startsWith("Connection error:")) "Offline · message saved" else "Gemini online"
                                }
                            }
                        }) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFBBA6FF)) }
                    }
                    Text("Offline: read history and use the interface. Online: Gemini responses.", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    private fun requestGemini(history: List<ChatLine>): String {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(CHAT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val messages = JSONArray()
            history.takeLast(24).forEach { messages.put(JSONObject().put("role", it.role).put("content", it.text)) }
            val body = JSONObject().put("messages", messages).put("style", "Balanced").put("mode", "Chat").put("modeInstruction", "")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(response)
            if (code !in 200..299) "Connection error: " + json.optString("error", "AI service returned HTTP $code")
            else json.optString("reply").ifBlank { "Connection error: the AI returned an empty response." }
        } catch (e: Exception) {
            "Connection error: " + (e.message ?: "Please check your internet connection.")
        } finally { connection?.disconnect() }
    }
}
