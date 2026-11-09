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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Real launcher entries returned by the connected LG, never a built-in provider catalog. */
data class LgWebOsApp(val id: String, val name: String, val icon: String = "")

@Composable
private fun LgAppIcon(app: LgWebOsApp) {
    var image by remember(app.icon) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(app.icon) {
        image = null
        val source = app.icon.trim()
        if (source.isEmpty() || !(source.startsWith("http://") || source.startsWith("https://"))) return@LaunchedEffect
        image = withContext(Dispatchers.IO) {
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
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
        .background(Color(0xFF242630)), contentAlignment = Alignment.Center) {
        if (image != null) Image(bitmap = image!!, contentDescription = app.name,
            modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
        else Text(app.name.take(2).uppercase(), color = Color.White, fontSize = 12.sp)
    }
}

/** LG only. TCL continues to use TclInstalledAppsBar and its Android companion. */
@Composable
fun LgWebOsAppsBar(
    deviceId: String,
    apps: List<LgWebOsApp>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLaunch: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("anytv_lg_webos_apps", Context.MODE_PRIVATE)
    }
    val prefKey = "favorites:$deviceId"
    var favorites by remember(deviceId) {
        mutableStateOf(prefs.getStringSet(prefKey, emptySet())?.toSet() ?: emptySet())
    }
    LaunchedEffect(deviceId) { if (deviceId.isNotBlank()) onRefresh() }
    val visible = apps.distinctBy { it.id }.sortedWith(
        compareBy<LgWebOsApp> { if (it.id in favorites) 0 else 1 }
            .thenBy { it.name.lowercase() }
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF15171E)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("Aplicativos da LG", color = Color.White, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onRefresh, enabled = deviceId.isNotBlank() && !loading) {
                Text("Atualizar")
            }
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Consultando a LG...", color = Color.LightGray, fontSize = 12.sp)
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (!loading && error == null && visible.isEmpty()) {
            Text("Nenhum aplicativo retornado pela LG. Use Atualizar para consultar novamente.",
                color = Color.LightGray, fontSize = 12.sp)
        }
        if (visible.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { app ->
                    Column(modifier = Modifier.size(width = 94.dp, height = 105.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.clickable { onLaunch(app.id) }) { LgAppIcon(app) }
                        Text(app.name, color = Color.White, fontSize = 11.sp,
                            maxLines = 2)
                        TextButton(onClick = {
                            favorites = if (app.id in favorites) favorites - app.id
                                else favorites + app.id
                            prefs.edit().putStringSet(prefKey, favorites.toSet()).apply()
                        }) {
                            Text(if (app.id in favorites) "★" else "☆", fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }
}
