package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

internal data class StreamingTitle(val id: Int, val name: String, val overview: String, val poster: String)
internal data class StreamingResults(val titles: List<StreamingTitle>, val more: Boolean)

/** Real TMDB/JustWatch availability. This API cannot start playback on a third-party TV app. */
private object StreamingCatalog {
    private fun norm(v: String) = Normalizer.normalize(v.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("[^a-z0-9]"), "")

    private fun provider(name: String): String? {
        val v = norm(name)
        return when {
            "netflix" in v -> "netflix"
            "primevideo" in v -> "amazonprimevideo"
            "disney" in v -> "disneyplus"
            "globoplay" in v -> "globoplay"
            "hbomax" in v || v == "max" -> "hbomax"
            "appletv" in v -> "appletvplus"
            "paramount" in v -> "paramountplus"
            "crunchyroll" in v -> "crunchyroll"
            "plutotv" in v -> "plutotv"
            "mubi" in v -> "mubi"
            "telecine" in v -> "telecine"
            "looke" in v -> "looke"
            "clarovideo" in v -> "clarovideo"
            else -> null
        }
    }

    fun streaming(app: TvOfficialApp): Boolean =
        provider(app.name) != null ||
            listOf("youtube", "spotify", "plex", "twitch", "vimeo", "dazn")
                .any { it in norm(app.name) || it in norm(app.packageName) }

    private fun get(token: String, route: String): JSONObject {
        require(token.isNotBlank()) { "Informe seu token de leitura TMDB." }
        val connection = URL("https://api.themoviedb.org/3/$route")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6000
            connection.readTimeout = 9000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            if (status !in 200..299) {
                error(if (status == 401 || status == 403) "Token TMDB não autorizado."
                    else "TMDB indisponível (HTTP $status).")
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun discover(token: String, app: String, media: String, page: Int): StreamingResults =
        withContext(Dispatchers.IO) {
            val alias = provider(app) ?: return@withContext StreamingResults(emptyList(), false)
            val providers = get(token, "watch/providers/$media?language=pt-BR&watch_region=BR")
                .optJSONArray("results") ?: JSONArray()
            var id = 0
            for (i in 0 until providers.length()) {
                val entry = providers.optJSONObject(i) ?: continue
                if (norm(entry.optString("provider_name")) == alias) {
                    id = entry.optInt("provider_id"); break
                }
            }
            if (id == 0) return@withContext StreamingResults(emptyList(), false)
            val result = get(token, "discover/$media?language=pt-BR&watch_region=BR" +
                "&with_watch_providers=$id&with_watch_monetization_types=flatrate" +
                "&include_adult=false&sort_by=popularity.desc&page=$page")
            val items = result.optJSONArray("results") ?: JSONArray()
            val titles = buildList {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val movieId = item.optInt("id")
                    val name = item.optString(if (media == "tv") "name" else "title")
                    if (movieId > 0 && name.isNotBlank()) {
                        add(StreamingTitle(movieId, name, item.optString("overview"),
                            item.optString("poster_path").takeIf { it.startsWith("/") } ?: ""))
                    }
                }
            }
            StreamingResults(titles, page < result.optInt("total_pages", page))
        }
}

@Composable
internal fun TclStreamingScreens(
    apps: List<TvOfficialApp>, host: String, pairing: String, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("anytv_streaming_catalog", Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(prefs.getString("tmdb_token", "").orEmpty()) }
    var tokenDraft by remember { mutableStateOf(token) }
    var editToken by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<TvOfficialApp?>(null) }
    var all by remember { mutableStateOf(false) }
    var media by remember { mutableStateOf("movie") }
    var page by remember { mutableStateOf(1) }
    var titles by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var more by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<StreamingTitle?>(null) }
    val displayed = remember(apps, all) { apps.filter { all || StreamingCatalog.streaming(it) } }

    LaunchedEffect(selected?.packageName, media, page, token) {
        val app = selected ?: return@LaunchedEffect
        if (page == 1) { titles = emptyList(); more = false }
        if (token.isBlank()) return@LaunchedEffect
        loading = true
        message = null
        try {
            val response = StreamingCatalog.discover(token, app.name, media, page)
            titles = if (page == 1) response.titles else (titles + response.titles).distinctBy { it.id }
            more = response.more
            if (page == 1 && response.titles.isEmpty())
                message = "Nenhum catálogo de assinatura compatível encontrado para este serviço."
        } catch (error: Exception) {
            message = error.message ?: "Falha ao consultar catálogo."
        } finally {
            loading = false
        }
    }

    fun openOnTv(app: TvOfficialApp) {
        if (host.isBlank() || pairing.isBlank()) {
            message = "Conecte o AnyTV Connect e vincule a TV antes de abrir aplicativos."
        } else {
            scope.launch {
                message = null
                runCatching { TclTvAppApi.open(host, pairing, app.packageName) }
                    .onSuccess { message = if (it) "Abertura solicitada na TCL." else "A TV não abriu o aplicativo." }
                    .onFailure { message = it.message ?: "Não foi possível abrir na TV." }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Streaming", fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
                if (selected == null) {
                    Text("Escolha um aplicativo realmente instalado na TCL.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Serviços", fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { all = !all }) {
                            Text(if (all) "Só streaming" else "Ver todos")
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayed, key = { it.packageName }) { app ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                    selected = app; media = "movie"; page = 1; message = null
                                }.padding(7.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OfficialTvIcon(app, 48.dp)
                                Text(app.name, modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("›", fontSize = 22.sp)
                            }
                        }
                        if (displayed.isEmpty()) item {
                            Text("Nenhum serviço identificado. Atualize os aplicativos da TCL.")
                        }
                    }
                } else {
                    val app = selected!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selected = null; details = null; message = null }) {
                            Text("‹ Voltar")
                        }
                        OfficialTvIcon(app, 38.dp)
                        Text(app.name, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = { openOnTv(app) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir " + app.name + " na TCL", maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Catálogo brasileiro", modifier = Modifier.weight(1f))
                        TextButton(onClick = { editToken = !editToken }) {
                            Text(if (editToken) "Ocultar" else "Configurar")
                        }
                    }
                    if (editToken || token.isBlank()) {
                        Text("Para consultar filmes e séries reais, configure um token de leitura " +
                            "TMDB. Aplicativos sem provedor TMDB continuam disponíveis para abertura na TV.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = tokenDraft,
                            onValueChange = { tokenDraft = it.trim() },
                            label = { Text("Token TMDB") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.themoviedb.org/settings/api")))
                            }) { Text("Obter token") }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = {
                                token = tokenDraft
                                prefs.edit().putString("tmdb_token", tokenDraft).apply()
                                editToken = false; page = 1
                            }) { Text("Salvar") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { media = "movie"; page = 1 },
                            enabled = media != "movie") { Text("Filmes") }
                        OutlinedButton(onClick = { media = "tv"; page = 1 },
                            enabled = media != "tv") { Text("Séries") }
                    }
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    if (message != null) Text(message.orEmpty(), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(titles, key = { it.id }) { title ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clickable { details = title }.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                if (title.poster.isNotEmpty()) {
                                    AsyncImage(
                                        model = "https://image.tmdb.org/t/p/w185" + title.poster,
                                        contentDescription = "Cartaz: " + title.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(63.dp, 95.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title.name, fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(title.overview.ifBlank { "Sinopse indisponível." },
                                        fontSize = 12.sp, maxLines = 3,
                                        overflow = TextOverflow.Ellipsis)
                                    Text("Ver detalhes ›", fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (more) item {
                            OutlinedButton(onClick = { if (!loading) page++ },
                                enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                                Text("Carregar mais")
                            }
                        }
                    }
                }
                Text("Disponibilidade no Brasil: TMDB / JustWatch. " +
                    "Este produto usa a API TMDB, mas não é endossado ou certificado pelo TMDB.",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val title = details
    if (title != null) {
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(title.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title.overview.ifBlank { "Sinopse indisponível." })
                    Text("O catálogo informa disponibilidade, não garante um link de reprodução " +
                        "direto nos aplicativos da TV.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    details = null
                    selected?.let { openOnTv(it) }
                }) { Text("Abrir app na TCL") }
            },
            dismissButton = {
                TextButton(onClick = { details = null }) { Text("Voltar") }
            }
        )
    }
}
