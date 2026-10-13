package com.hari.androidtvremote.ui.app

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hari.androidtvremote.R

private data class TvIptvPlayer(val name: String, val packageName: String)

private val nativeAppLinks = mapOf(
    "netflix" to "netflix://",
    "prime_video" to "https://app.primevideo.com",
    "hbo_max" to "https://play.max.com",
    "disney_plus" to "https://www.disneyplus.com",
    "apple_tv" to "https://tv.apple.com",
    "youtube" to "vnd.youtube.launch://"
)

private val iptvPlayerOptions = listOf(
    TvIptvPlayer("TiviMate", "ar.tvplayer.tv"),
    TvIptvPlayer("XCIPTV Player", "com.nathnetwork.xciptv"),
    TvIptvPlayer("Televizo", "com.ottplay.ottplay"),
    TvIptvPlayer("Kodi", "org.xbmc.kodi"),
    TvIptvPlayer("VLC", "org.videolan.vlc")
)

/**
 * The remote v2 protocol cannot query which packages are installed on the TV.
 * This picker lets the user select a likely IPTV player by known app ID;
 * it never asks for an IPTV server URL or supplies streams.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppShortcutStripSquare(
    quickApps: List<RemoteShortcutApp>,
    onQuickApp: (String) -> Unit,
    shortcutSize: Dp
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("tcl_iptv_shortcut", Context.MODE_PRIVATE)
    }
    var selectedPackage by remember {
        mutableStateOf(prefs.getString("tv_player_package", "") ?: "")
    }
    var showingPicker by remember { mutableStateOf(false) }
    var editingStreaming by remember { mutableStateOf<RemoteShortcutApp?>(null) }
    var customPackage by rememberSaveable { mutableStateOf("") }
    var pickerError by remember { mutableStateOf("") }

    fun openPicker() {
        customPackage = selectedPackage.takeIf {
            it.isNotBlank() && iptvPlayerOptions.none { p -> p.packageName == it }
        } ?: ""
        pickerError = ""
        showingPicker = true
    }

    fun choose(packageName: String) {
        selectedPackage = packageName
        prefs.edit().putString("tv_player_package", packageName).apply()
        showingPicker = false
        onQuickApp("market://launch?id=" + packageName)
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickApps.forEach { app ->
            val logoId = when (app.id) {
                "netflix" -> R.drawable.tcl_brand_netflix
                "prime_video" -> R.drawable.tcl_brand_prime_video
                "hbo_max" -> R.drawable.tcl_brand_max
                "disney_plus" -> R.drawable.tcl_brand_disney_plus
                "apple_tv" -> R.drawable.tcl_brand_apple_tv
                "youtube" -> R.drawable.tcl_brand_youtube
                "iptv" -> R.drawable.tcl_brand_iptv
                else -> null
            }
            Box(
                modifier = Modifier
                    .size(shortcutSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF121624))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {
                            if (app.id == "iptv") {
                                if (selectedPackage.isBlank()) openPicker()
                                else onQuickApp("market://launch?id=" + selectedPackage)
                            } else {
                                val alternate = nativeAppLinks[app.id]
                                if (alternate != null && prefs.getBoolean("use_native_" + app.id, false)) {
                                    onQuickApp(alternate)
                                } else {
                                    onQuickApp(app.intentUri ?: app.launchName)
                                }
                            }
                        },
                        onLongClick = {
                            if (app.id == "iptv") openPicker()
                            else if (app.id in nativeAppLinks) editingStreaming = app
                        }
                    )
                    .semantics {
                        contentDescription = if (app.id == "iptv")
                            "IPTV, toque para abrir ou segure para escolher outro aplicativo"
                        else app.label
                    },
                contentAlignment = Alignment.Center
            ) {
                if (logoId != null) {
                    Image(
                        painter = painterResource(logoId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = app.mark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    val selectedStreaming = editingStreaming
    if (selectedStreaming != null) {
        AlertDialog(
            onDismissRequest = { editingStreaming = null },
            title = { Text("Abrir " + selectedStreaming.label) },
            text = {
                Text(
                    "Selecione como a TCL deve iniciar este serviço. " +
                    "O método por aplicativo evita abrir um site; se não funcionar na sua TV, " +
                    "teste o link nativo. A preferência fica salva."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("use_native_" + selectedStreaming.id, false).apply()
                    editingStreaming = null
                    onQuickApp(selectedStreaming.launchName)
                }) { Text("Abrir aplicativo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("use_native_" + selectedStreaming.id, true).apply()
                    editingStreaming = null
                    onQuickApp(nativeAppLinks.getValue(selectedStreaming.id))
                }) { Text("Testar link nativo") }
            }
        )
    }

    if (showingPicker) {
        AlertDialog(
            onDismissRequest = { showingPicker = false },
            title = { Text("Escolher aplicativo IPTV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Selecione o player que você já tem instalado na TV. " +
                        "A conexão remota não permite listar os aplicativos instalados.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    iptvPlayerOptions.forEach { player ->
                        TextButton(onClick = { choose(player.packageName) }) {
                            Text(
                                text = (if (player.packageName == selectedPackage) "✓  " else "") +
                                    player.name
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customPackage,
                        onValueChange = { customPackage = it; pickerError = "" },
                        label = { Text("Outro app: ID do pacote (opcional)") },
                        placeholder = { Text("com.exemplo.player") },
                        singleLine = true
                    )
                    if (pickerError.isNotEmpty()) {
                        Text(pickerError, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        selectedPackage = ""
                        prefs.edit().remove("tv_player_package").apply()
                        showingPicker = false
                    }) { Text("Restaurar escolha / redefinir atalho") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val candidate = customPackage.trim()
                    if (candidate.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+"))) {
                        choose(candidate)
                    } else {
                        pickerError = "Selecione um player acima ou informe um ID Android válido."
                    }
                }) { Text("Usar outro app") }
            },
            dismissButton = {
                TextButton(onClick = { showingPicker = false }) { Text("Cancelar") }
            }
        )
    }
}
