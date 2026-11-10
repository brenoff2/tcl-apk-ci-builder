package com.hari.androidtvremote.ui.app

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Real LG launcher IDs; never pass Android TV package names to webOS. */
data class LgWebOsApp(val id: String, val name: String, val icon: String = "")

@Composable
private fun LgAppIcon(app: LgWebOsApp, size: Dp = 52.dp, fallbackIcon: String = "") {
    val sources = listOf(app.icon.trim(), fallbackIcon).filter { source ->
        source.startsWith("http://") || source.startsWith("https://")
    }.distinct()
    var image by remember(sources) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sources) {
        image = null
        image = withContext(Dispatchers.IO) {
            sources.firstNotNullOfOrNull { source ->
                runCatching {
                    val connection = URL(source).openConnection() as HttpURLConnection
                    connection.connectTimeout = 2500
                    connection.readTimeout = 2500
                    try {
                        val type = connection.contentType.orEmpty().lowercase()
                        val length = connection.contentLengthLong
                        if (!type.startsWith("image/") || length > 512_000) null
                        else connection.inputStream.use { input ->
                            val bytes = input.readNBytes(512_001)
                            if (bytes.size > 512_000) null
                            else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }
                    } finally { connection.disconnect() }
                }.getOrNull()
            }
        }
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(15.dp))
        .background(Color(0xFF1C1E25)), contentAlignment = Alignment.Center) {
        if (image != null) Image(bitmap = image!!, contentDescription = app.name,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Text(app.name.take(2).uppercase(), color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Same home arrangement and app-picker behavior as TCL; webOS transport stays LG-only. */
@Composable
fun LgWebOsAppsBar(
    deviceId: String,
    apps: List<LgWebOsApp>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLaunch: (String) -> Unit,
    onOpenStreaming: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("anytv_lg_webos_apps", Context.MODE_PRIVATE)
    }
    val prefKey = "favorites:$deviceId"
    val savedFavorites = remember(deviceId) { prefs.getStringSet(prefKey, null)?.toSet() }
    var favorites by remember(deviceId) { mutableStateOf(savedFavorites.orEmpty()) }
    var hasUserFavorites by remember(deviceId) { mutableStateOf(savedFavorites != null) }
    var showManager by remember(deviceId) { mutableStateOf(false) }
    var showLaunchPicker by remember(deviceId) { mutableStateOf(false) }
    var selectedBannerTitle by remember(deviceId) { mutableStateOf<StreamingTitle?>(null) }
    var titleOffers by remember(deviceId) { mutableStateOf<List<StreamingOffer>?>(null) }
    var titleOffersError by remember(deviceId) { mutableStateOf(false) }
    LaunchedEffect(deviceId, selectedBannerTitle?.id, selectedBannerTitle?.media) {
        val title = selectedBannerTitle
        titleOffers = null
        titleOffersError = false
        if (title != null) {
            runCatching { anyTvOffersForTitle(title) }
                .onSuccess { titleOffers = it }
                .onFailure { titleOffersError = true }
        }
    }

    val allApps = apps.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    // Use provider logos only for exact service matches; launcher IDs always come from LG.
    var providerIcons by remember(deviceId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(deviceId, allApps.map { it.name }) {
        providerIcons = emptyMap()
        providerIcons = runCatching {
            anyTvBrazilProviders().filter { it.logo.isNotBlank() }.associate { provider ->
                anyTvServiceKey(provider.name) to
                    ("https://image.tmdb.org/t/p/w92" + provider.logo)
            }
        }.getOrDefault(emptyMap())
    }
    fun appLogo(app: LgWebOsApp): String = providerIcons[anyTvServiceKey(app.name)].orEmpty()
    val selectedApps = if (!hasUserFavorites) allApps.take(6)
        else allApps.filter { it.id in favorites }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (selectedApps.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedApps.forEach { app ->
                    Column(
                        modifier = Modifier.width(56.dp).clickable { onLaunch(app.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        LgAppIcon(app, 52.dp, appLogo(app))
                        Text(app.name, fontSize = 10.sp, maxLines = 1,
                            color = Color.White, textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF1C1E25)).clickable { showManager = true },
                    contentAlignment = Alignment.Center) {
                    Text("Editar", color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            OutlinedButton(
                onClick = { if (!loading && allApps.isEmpty()) onRefresh() else showManager = true },
                enabled = deviceId.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Identificando aplicativos instalados..."
                    else if (allApps.isEmpty()) "Atualizar aplicativos da LG"
                    else "Escolher aplicativos da LG")
            }
        }
        // The home banner is navigation to the Streaming catalogue, never a TV launch.
        AnyTvHomeFilmBanner(onOpenStreaming = onOpenStreaming)
        if (error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(error, color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh, enabled = !loading) { Text("Tentar") }
            }
        }
    }

    if (showManager) {
        var draft by remember(deviceId, showManager) { mutableStateOf(favorites) }
        val displayed = allApps.sortedWith(compareBy<LgWebOsApp> {
            if (it.id in draft) 0 else 1
        }.thenBy { it.name.lowercase() })
        Dialog(onDismissRequest = { showManager = false }) {
            Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Editar aplicativos da TV", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)
                    Text("Somente aplicativos anunciados pela LG aparecem no controle remoto.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                        items(displayed, key = { it.id }) { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LgAppIcon(app, 36.dp, appLogo(app))
                                Text(app.name, fontSize = 12.sp,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp))
                                Checkbox(checked = app.id in draft,
                                    onCheckedChange = { checked ->
                                        draft = if (checked) draft + app.id else draft - app.id
                                    })
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onRefresh, enabled = !loading) { Text("Atualizar") }
                        TextButton(onClick = { draft = emptySet() }) { Text("Limpar") }
                        TextButton(onClick = { showManager = false }) { Text("Cancelar") }
                        Spacer(Modifier.width(6.dp))
                        Button(onClick = {
                            favorites = draft.filter { id -> allApps.any { it.id == id } }.toSet()
                            hasUserFavorites = true
                            prefs.edit().putStringSet(prefKey, favorites.toSet()).commit()
                            showManager = false
                        }) { Text("Salvar") }
                    }
                }
            }
        }
    }

    selectedBannerTitle?.let { title ->
        val installedForTitle = (titleOffers ?: emptyList()).mapNotNull { offer ->
            allApps.firstOrNull { anyTvServiceKey(it.name) == anyTvServiceKey(offer.name) }
                ?.let { offer to it }
        }.distinctBy { (_, app) -> app.id }
        Dialog(onDismissRequest = { selectedBannerTitle = null }) {
            Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (title.overview.isNotBlank())
                        Text(title.overview, maxLines = 5,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
                    Text("Onde assistir na TV", fontWeight = FontWeight.SemiBold)
                    when {
                        titleOffers == null && !titleOffersError ->
                            Text("Verificando disponibilidade no Brasil...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        titleOffersError ->
                            Text("Não foi possível consultar onde assistir. Tente novamente.",
                                color = MaterialTheme.colorScheme.error)
                        installedForTitle.isEmpty() ->
                            Text("Não foi identificado um aplicativo instalado na LG para este título.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> {
                            LazyColumn(modifier = Modifier.heightIn(max = 310.dp)) {
                                items(installedForTitle, key = { (_, app) -> app.id }) { (_, app) ->
                                    Row(modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            selectedBannerTitle = null
                                            onLaunch(app.id)
                                        }.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        LgAppIcon(app, 36.dp, appLogo(app))
                                        Column(modifier = Modifier.padding(start = 9.dp)) {
                                            Text(app.name, fontSize = 13.sp)
                                            Text("Abrir aplicativo na LG",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text("O aplicativo será aberto na TV. A reprodução automática do título depende de um link de conteúdo compatível com o streaming.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { selectedBannerTitle = null }) { Text("Fechar") }
                }
            }
        }
    }

    if (showLaunchPicker) {
        Dialog(onDismissRequest = { showLaunchPicker = false }) {
            Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Abrir aplicativo na LG", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)
                    Text("A LG pode abrir o aplicativo; a navegação até um título depende da TV.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(if (selectedApps.isNotEmpty()) selectedApps else allApps,
                            key = { it.id }) { app ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clickable { showLaunchPicker = false; onLaunch(app.id) }
                                .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                LgAppIcon(app, 36.dp, appLogo(app))
                                Text(app.name, modifier = Modifier.padding(start = 9.dp),
                                    fontSize = 13.sp)
                            }
                        }
                    }
                    TextButton(onClick = { showLaunchPicker = false }) { Text("Fechar") }
                }
            }
        }
    }
}
