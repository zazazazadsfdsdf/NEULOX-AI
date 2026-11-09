package com.neulox.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Neon = Color(0xFF35E889)
private val Bg = Color(0xFF090D0B)
private val Panel = Color(0xFF151C18)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeuloxApp() }
    }
}

@Composable
private fun NeuloxApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("neulox_settings", 0) }
    var tab by remember { mutableStateOf("Чат") }
    var apiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon,
            background = Bg,
            surface = Panel,
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).background(Neon, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("N", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 25.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("NEULOX AI", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text("БЕСПЛАТНЫЙ GEMINI AI", color = Neon, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Icon(Icons.Default.Circle, contentDescription = null, tint = Neon, modifier = Modifier.size(9.dp))
                Spacer(Modifier.width(6.dp))
                Text("ONLINE", color = Neon, fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Чат", "Код", "Файлы", "Настройки").forEach { item ->
                    val selected = tab == item
                    Surface(
                        onClick = { tab = item },
                        color = if (selected) Neon else Panel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(item, modifier = Modifier.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (selected) Color.Black else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            when (tab) {
                "Настройки" -> {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Настройки Gemini", color = Neon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Вставь бесплатный Gemini API key из Google AI Studio. Ключ сохраняется на этом устройстве.", color = Color.LightGray)
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                prefs.edit().putString("gemini_api_key", it).apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Gemini API key") },
                            singleLine = true
                        )
                        Text("Модель: gemini-2.5-flash-lite", color = Color.LightGray, fontSize = 13.sp)
                        Text("Бесплатный тариф имеет ограничения Google по запросам.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                "Файлы" -> {
                    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Файлы", color = Neon, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Создание и просмотр файлов появятся в следующем обновлении.", color = Color.LightGray)
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (messages.isEmpty()) {
                            Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(if (tab == "Код") "NEULOX CODE" else "Чем могу помочь?", color = Neon, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(if (tab == "Код") "Опиши задачу по программированию." else "Задай вопрос — начнём общение.", color = Color.LightGray)
                            }
                        } else {
                            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(messages) { msg ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.first) Arrangement.End else Arrangement.Start) {
                                        Surface(color = if (msg.first) Color(0xFF173D2B) else Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                                            Text(msg.second, Modifier.padding(14.dp), color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Neon)
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f),
                                placeholder = { Text("Напиши сообщение...") }, maxLines = 4)
                            Spacer(Modifier.width(8.dp))
                            IconButton(enabled = !busy && draft.isNotBlank(), onClick = {
                                val prompt = draft.trim()
                                draft = ""
                                messages.add(true to prompt)
                                busy = true
                                val coding = tab == "Код"
                                scope.launch {
                                    val answer = withContext(Dispatchers.IO) { requestGemini(apiKey, prompt, coding) }
                                    messages.add(false to answer)
                                    busy = false
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Neon)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun requestGemini(key: String, prompt: String, coding: Boolean): String {
    if (key.isBlank()) return "Открой «Настройки» и добавь бесплатный Gemini API-ключ из https://aistudio.google.com/app/apikey"
    var conn: HttpURLConnection? = null
    return try {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + java.net.URLEncoder.encode(key.trim(), "UTF-8")
        conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val system = if (coding) "Ты NEULOX CODE, опытный senior-разработчик. Отвечай по-русски, давай рабочий код и объясняй ошибки." else "Ты NEULOX AI, полезный ИИ-помощник. Отвечай по-русски ясно и точно."
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", JSONArray().put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 2048))
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (code !in 200..299) {
            val message = json.optJSONObject("error")?.optString("message") ?: "HTTP $code"
            "Ошибка Gemini ($code): $message"
        } else {
            val candidates = json.optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val answer = buildString {
                if (parts != null) for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    if (part != null) append(part.optString("text", ""))
                }
            }
            answer.ifBlank { "Gemini не вернул текстовый ответ. Попробуй отправить запрос ещё раз." }
        }
    } catch (e: Exception) {
        "Не удалось связаться с Gemini: ${e.message ?: "неизвестная ошибка"}"
    } finally {
        conn?.disconnect()
    }
}
