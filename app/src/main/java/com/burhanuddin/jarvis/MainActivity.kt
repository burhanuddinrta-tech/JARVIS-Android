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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class ChatLine(val role: String, val text: String)

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
            var online by remember { mutableStateOf(false) }
            val bg = Color(0xFF090812)
            val panel = Color(0xFF171426)
            MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = Color(0xFF9B7CFF))) {
                Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◉", color = Color(0xFF9B7CFF), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("JARVIS", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("Personal AI assistant", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(if (online) "ONLINE" else "OFFLINE MODE", color = if (online) Color(0xFF65D69A) else Color.LightGray)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Your conversations are saved on this phone.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false, contentPadding = PaddingValues(vertical = 12.dp)) {
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
                                    Text("Hello. I'm JARVIS.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Text("Your chat interface and saved conversations are available offline. Connect to the internet for AI responses.", color = Color.LightGray)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(24.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f).padding(10.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), decorationBox = { inner -> if (draft.isEmpty()) Text("Message JARVIS…", color = Color.Gray); inner() })
                        IconButton(onClick = {
                            val msg = draft.trim()
                            if (msg.isNotEmpty()) {
                                lines = lines + ChatLine("user", msg) + ChatLine("assistant", "Your message is saved locally. Online Gemini connection is being integrated into this native build.")
                                draft = ""
                                val arr = JSONArray()
                                lines.takeLast(100).forEach { arr.put(JSONObject().put("role", it.role).put("text", it.text)) }
                                prefs.edit().putString("messages", arr.toString()).apply()
                            }
                        }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFBBA6FF)) }
                    }
                }
            }
        }
    }
}
