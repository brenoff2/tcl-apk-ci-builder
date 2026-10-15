package com.hari.androidtvremote.ui.app

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TvOfficialApp(val packageName: String, val name: String, val icon: String)

/** TV-local pairing; icons and app names are read from Android TV PackageManager. */
internal object TclTvAppApi {
    private fun request(host: String, code: String, path: String, body: String? = null): String {
        require(host.matches(Regex("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) {
            "Endereço IP da TV indisponível"
        }
        val conn = URL("http://$host:18765$path").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 2600
            conn.readTimeout = 8500
            conn.requestMethod = if (body == null) "GET" else "POST"
            conn.setRequestProperty("X-TCL-Pair", code)
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            if (status == 401) throw IllegalStateException("Código incorreto. Confira os 8 dígitos na TCL.")
            if (status == 404 && path == "/headphones") {
                throw IllegalStateException("Para reconectar os fones, instale Controle TCL TV 1.3 na televisão.")
            }
            if (status !in 200..299) throw IllegalStateException("A TCL não conseguiu executar a ação ($status).")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (error: java.net.ConnectException) {
            throw IllegalStateException("Abra Controle TCL TV na televisão e ative a conexão.", error)
        } catch (error: java.net.SocketTimeoutException) {
            throw IllegalStateException("A TCL não respondeu. Verifique se os dois aparelhos usam a mesma rede.", error)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun apps(host: String, code: String): List<TvOfficialApp> =
        withContext(Dispatchers.IO) {
            val data = JSONArray(request(host, code, "/apps"))
            buildList {
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val pkg = item.optString("package")
                    if (pkg.isNotBlank()) add(
                        TvOfficialApp(pkg, item.optString("name", pkg), item.optString("icon"))
                    )
                }
            }.sortedBy { it.name.lowercase() }
        }

    suspend fun open(host: String, code: String, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            JSONObject(request(host, code, "/launch",
                JSONObject().put("package", packageName).toString())).optBoolean("launched")
        }

    suspend fun searchTitle(
        host: String, code: String, packageName: String, title: String
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("package", packageName)
            .put("title", title.take(180)).toString()
        JSONObject(request(host, code, "/title-search", body))
            .optString("mode", "unavailable")
    }

    suspend fun openBluetoothSettings(host: String, code: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                JSONObject(request(host, code, "/headphones/settings", "{}"))
                    .optBoolean("opened", false)
            }.getOrDefault(false)
        }

    suspend fun headphones(host: String, code: String): String =
        withContext(Dispatchers.IO) {
            var status = JSONObject(request(host, code, "/headphones", "{}"))
                .optString("status", "")
            // TV 1.4 responds "requested" immediately, then asynchronously
            // checks A2DP state. Never claim connected until it is confirmed.
            if (status == "requested") {
                for (attempt in 0 until 12) {
                    kotlinx.coroutines.delay(800L)
                    status = JSONObject(request(host, code, "/headphones/status"))
                        .optString("status", "not_supported")
                    if (status != "reconnecting" && status != "requested") break
                }
            }
            when (status) {
                "connected" -> "Fones reconectados. A TCL confirmou a conexão Bluetooth."
                "reconnecting", "requested" -> "A TCL ainda está tentando reconectar os fones."
                "not_configured" -> "Na TV, abra Controle TCL TV e selecione seus fones pareados."
                "permission_required" -> "Na TV, abra Controle TCL TV e autorize a conexão Bluetooth."
                "api_restricted" -> "O Android da TV bloqueou a reconexão automática. " +
                    "Você pode abrir as configurações Bluetooth da TV pelo botão abaixo."
                "not_paired" -> "Esses fones não estão mais pareados com a TCL."
                "not_connected", "timeout", "profile_unavailable" ->
                    "A TV não conseguiu confirmar a reconexão dos fones."
                "bluetooth_off" -> "O Bluetooth da TCL está desligado."
                "not_supported" -> "O recurso não é permitido pelo Android desta TCL."
                else -> "Resposta inesperada da TCL: $status"
            }
        }

    fun savedCode(prefs: android.content.SharedPreferences,
                  host: String?, deviceId: String?, deviceName: String?): String? {
        val name = deviceName?.trim()?.lowercase()
        return listOfNotNull(
            host?.takeIf { it.isNotBlank() }?.let { prefs.getString("pair:$it", null) },
            deviceId?.takeIf { it.isNotBlank() }?.let { prefs.getString("pair:$it", null) },
            name?.takeIf { it.isNotBlank() }?.let { prefs.getString("pair:name:$it", null) }
        ).firstOrNull { it.matches(Regex("[0-9]{8}")) }
    }
}

private fun savedFavoriteIds(raw: String?): List<String> =
    if (raw.isNullOrBlank()) emptyList() else try {
        val json = JSONArray(raw)
        (0 until json.length()).map { json.optString(it) }
            .filter { it.isNotBlank() }.distinct()
    } catch (_: Exception) { emptyList() }

@Composable
internal fun OfficialTvIcon(app: TvOfficialApp, size: Dp) {
    val bitmap: ImageBitmap? = remember(app.icon) {
        try {
            val decoded = Base64.decode(app.icon, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
        } catch (_: Exception) { null }
    }
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = app.name,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Text(app.name.take(2).uppercase(), color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun TclInstalledAppsBar(
    tvHost: String?, tvIdentity: String?, tvName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("tcl_tv_installed_apps", Context.MODE_PRIVATE)
    }
    val host = tvHost.orEmpty()
    // Discovery IDs can change after reconnect. Keep aliases by TV IP and
    // friendly name, and migrate v1.9 preferences that stored only the ID.
    val deviceKey = tvIdentity?.takeIf { it.isNotBlank() } ?: host
    val nameKey = tvName?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val savedPair = remember(deviceKey, host, nameKey) {
        prefs.getString("pair:$host", null)
            ?: prefs.getString("pair:$deviceKey", null)
            ?: nameKey?.let { prefs.getString("pair:name:$it", null) }
            ?: ""
    }
    var pairing by remember(deviceKey, host, nameKey) { mutableStateOf(savedPair) }
    var codeEntry by remember(deviceKey, host, nameKey) { mutableStateOf(pairing) }
    var allApps by remember(deviceKey, host, nameKey) { mutableStateOf<List<TvOfficialApp>>(emptyList()) }
    val savedFavoriteRaw = prefs.getString("favorites:$host", null)
        ?: prefs.getString("favorites:$deviceKey", null)
        ?: nameKey?.let { prefs.getString("favorites:name:$it", null) }
    var favorites by remember(deviceKey, host) {
        mutableStateOf(savedFavoriteIds(savedFavoriteRaw))
    }
    var hasUserFavorites by remember(deviceKey, host) {
        mutableStateOf(savedFavoriteRaw != null)
    }
    var showPair by remember(deviceKey, host) { mutableStateOf(false) }
    var showManager by remember(deviceKey, host) { mutableStateOf(false) }
    var showStreaming by remember(deviceKey, host) { mutableStateOf(false) }
    var busy by remember(deviceKey, host) { mutableStateOf(false) }
    var errorText by remember(deviceKey, host) { mutableStateOf<String?>(null) }
    var refresh by remember(deviceKey, host) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    fun storePair(code: String) {
        // commit synchronously so immediately closing the app cannot lose it.
        val editor = prefs.edit().putString("pair:$deviceKey", code)
        if (host.isNotBlank()) editor.putString("pair:$host", code)
        if (nameKey != null) editor.putString("pair:name:$nameKey", code)
        editor.commit()
    }
    fun storeFavorites(ids: List<String>) {
        val serialized = JSONArray(ids).toString()
        val editor = prefs.edit().putString("favorites:$deviceKey", serialized)
        if (host.isNotBlank()) editor.putString("favorites:$host", serialized)
        if (nameKey != null) editor.putString("favorites:name:$nameKey", serialized)
        editor.commit()
    }

    // v1.9 stored pair:<ephemeral_device_id> only. Search legacy keys and
    // verify candidates against the actual TV before adopting a migrated code.
    LaunchedEffect(host, deviceKey, nameKey) {
        if (host.isNotBlank() && pairing.isBlank()) {
            val candidates = prefs.all.entries
                .filter { it.key.startsWith("pair:") }
                .mapNotNull { it.value as? String }
                .filter { it.matches(Regex("[0-9]{8}")) }
                .distinct().take(5)
            if (candidates.isNotEmpty()) {
                busy = true
                for (code in candidates) {
                    val apps = runCatching { TclTvAppApi.apps(host, code) }.getOrNull()
                    if (apps != null) {
                        storePair(code)
                        pairing = code
                        codeEntry = code
                        allApps = apps
                        errorText = null
                        break
                    }
                }
                busy = false
            }
        }
    }

    LaunchedEffect(host, pairing, refresh) {
        if (host.isNotBlank() && pairing.isNotBlank()) {
            busy = true
            runCatching { TclTvAppApi.apps(host, pairing) }
                .onSuccess { list ->
                    allApps = list
                    storePair(pairing)
                    errorText = null
                }.onFailure {
                    errorText = it.message ?: "Não foi possível consultar aplicativos da TCL."
                    if (it.message?.contains("Código incorreto") == true) {
                        codeEntry = pairing
                        showPair = true
                    }
                }
            busy = false
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Aplicativos", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            TextButton(onClick = {
                if (host.isBlank()) errorText = "Conecte a TCL primeiro."
                else if (pairing.isBlank()) showPair = true
                else showStreaming = true
            }) { Text("Streaming") }
            TextButton(onClick = {
                if (host.isBlank()) errorText = "Conecte a TCL primeiro."
                else if (pairing.isBlank()) showPair = true
                else if (allApps.isEmpty()) refresh++
                else showManager = true
            }) { Text("Ver todos  ›") }
        }
        val byPackage = remember(allApps) { allApps.associateBy { it.packageName } }
        // First connection: show only packages actually reported by this TV.
        // The user chooses favorites; never invent Netflix/Prime/etc placeholders.
        val selectedApps = if (!hasUserFavorites) allApps.take(7)
            else favorites.mapNotNull { byPackage[it] }
        if (selectedApps.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                selectedApps.forEach { app ->
                    Column(
                        modifier = Modifier.width(62.dp).clickable {
                            scope.launch {
                                errorText = null
                                runCatching { TclTvAppApi.open(host, pairing, app.packageName) }
                                    .onFailure {
                                        errorText = it.message ?: ("Não foi possível abrir " + app.name)
                                    }.onSuccess { opened ->
                                        if (!opened) errorText = "A TCL não iniciou " + app.name
                                    }
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        OfficialTvIcon(app = app, size = 56.dp)
                        Text(app.name, fontSize = 10.sp, maxLines = 1,
                            textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showManager = true },
                    contentAlignment = Alignment.Center
                ) { Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 23.sp) }
            }
        } else {
            OutlinedButton(
                onClick = {
                    if (host.isBlank()) errorText = "Conecte a TCL primeiro."
                    else if (pairing.isBlank()) showPair = true
                    else if (allApps.isEmpty()) refresh++
                    else showManager = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(if (host.isBlank()) "Conecte à TCL para identificar os aplicativos"
                     else if (pairing.isBlank()) "Vincular leitura de aplicativos da TV"
                     else if (busy) "Identificando aplicativos instalados..." else "Atualizar aplicativos da TV")
            }
        }
        if (errorText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(errorText.orEmpty(), color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (pairing.isNotBlank()) refresh++ else showPair = true
                }) { Text("Tentar") }
            }
        }
    }

    if (showStreaming) {
        TclStreamingScreens(apps = allApps, host = host, pairing = pairing,
            onDismiss = { showStreaming = false })
    }

    if (showPair) {
        AlertDialog(
            onDismissRequest = { showPair = false },
            title = { Text("Aplicativos instalados na TCL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Na TV, abra Controle TCL TV e encontre o código de 8 dígitos. " +
                            "Ative a conexão pelo próprio complemento na TV e conecte ambos à mesma rede.",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = codeEntry,
                        onValueChange = { text -> codeEntry = text.filter { it.isDigit() }.take(8) },
                        label = { Text("Código que aparece na TV") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = codeEntry.length == 8 && host.isNotBlank() && !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            runCatching { TclTvAppApi.apps(host, codeEntry) }
                                .onSuccess { list ->
                                    storePair(codeEntry)
                                    pairing = codeEntry
                                    allApps = list
                                    errorText = null
                                    showPair = false
                                    showManager = true
                                }.onFailure {
                                    errorText = it.message ?: "Código ou conexão inválidos."
                                }
                            busy = false
                        }
                    }
                ) { Text("Vincular") }
            },
            dismissButton = {
                TextButton(onClick = { showPair = false }) { Text("Cancelar") }
            }
        )
    }

    if (showManager) {
        var draft by remember(deviceKey, host, showManager) { mutableStateOf(favorites) }
        val present = allApps.associateBy { it.packageName }
        val displayed = allApps.sortedWith(
            compareBy<TvOfficialApp> { if (it.packageName in draft) 0 else 1 }
                .thenBy { item ->
                    val index = draft.indexOf(item.packageName)
                    if (index >= 0) index else Int.MAX_VALUE
                }.thenBy { it.name.lowercase() }
        )
        Dialog(onDismissRequest = { showManager = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Seus aplicativos", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Ícones oficiais da TV. Marque os favoritos e use ↑ ↓ para ordenar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(displayed, key = { it.packageName }) { item ->
                            val selected = item.packageName in draft
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OfficialTvIcon(app = item, size = 39.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(item.name, modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp)
                                if (selected) {
                                    TextButton(
                                        onClick = {
                                            val pos = draft.indexOf(item.packageName)
                                            if (pos > 0) {
                                                val list = draft.toMutableList()
                                                java.util.Collections.swap(list, pos, pos - 1)
                                                draft = list
                                            }
                                        },
                                        enabled = draft.indexOf(item.packageName) > 0
                                    ) { Text("↑") }
                                    TextButton(
                                        onClick = {
                                            val pos = draft.indexOf(item.packageName)
                                            if (pos >= 0 && pos < draft.lastIndex) {
                                                val list = draft.toMutableList()
                                                java.util.Collections.swap(list, pos, pos + 1)
                                                draft = list
                                            }
                                        },
                                        enabled = draft.indexOf(item.packageName) < draft.lastIndex
                                    ) { Text("↓") }
                                }
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        draft = if (checked) draft + item.packageName
                                        else draft.filterNot { it == item.packageName }
                                    }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { draft = emptyList() }) { Text("Limpar") }
                        TextButton(onClick = { showManager = false }) { Text("Cancelar") }
                        Spacer(Modifier.width(6.dp))
                        Button(onClick = {
                            val valid = draft.filter { it in present }
                            favorites = valid
                            hasUserFavorites = true
                            storeFavorites(valid)
                            showManager = false
                        }) { Text("Salvar") }
                    }
                }
            }
        }
    }
}
