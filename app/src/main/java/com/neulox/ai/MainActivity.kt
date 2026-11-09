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
    var tab by remember { mutableStateOf("Чат") }
    var apiKey by remember { mutableStateOf("") }
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
                    Text("ТВОЙ ИИ-ПОМОЩНИК", color = Neon, fontSize = 9.sp, letterSpacing = 2.sp)
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
                        Text("Настройки API", color = Neon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Вставь свой API-ключ совместимого провайдера. Он хранится только на этом устройстве.", color = Color.LightGray)
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("API key") }, singleLine = true)
                        Text("Модель: gpt-4o-mini", color = Color.LightGray, fontSize = 13.sp)
                    }
                }
                "Файлы" -> {
                    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Файлы", color = Neon, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Здесь будет создание и просмотр HTML, JSON, TXT и других файлов.", color = Color.LightGray)
                        Text("Первая версия приложения", color = Color.Gray, fontSize = 12.sp)
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
                                scope.launch {
                                    val answer = withContext(Dispatchers.IO) { requestAi(apiKey, prompt, tab == "Код") }
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

private fun requestAi(key: String, prompt: String, coding: Boolean): String {
    if (key.isBlank()) return "Открой «Настройки» и добавь API-ключ, чтобы включить ответы ИИ."
    return try {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
        }
        val system = if (coding) "Ты NEULOX CODE, опытный senior-разработчик. Отвечай по-русски, давай рабочий код и объясняй ошибки." else "Ты NEULOX AI, полезный ИИ-помощник. Отвечай по-русски ясно и точно."
        val body = JSONObject().put("model", "gpt-4o-mini")
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(response)
        if (json.has("error")) json.getJSONObject("error").optString("message", "Ошибка API")
        else json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "Пустой ответ")
    } catch (e: Exception) {
        "Не удалось связаться с API: ${e.message ?: "неизвестная ошибка"}"
    }
}
