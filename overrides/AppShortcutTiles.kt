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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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

/**
 * Square, offline logos for built-in streaming shortcuts.
 * A long press on the IPTV tile edits the user-selected link to their own player.
 * No channel lists or IPTV streams are supplied.
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
    var iptvLink by remember { mutableStateOf(prefs.getString("link", "") ?: "") }
    var editingIptv by remember { mutableStateOf(false) }
    var iptvInput by rememberSaveable { mutableStateOf(iptvLink) }
    var iptvError by remember { mutableStateOf("") }

    fun openEditor() {
        iptvInput = iptvLink
        iptvError = ""
        editingIptv = true
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickApps.forEach { app ->
            val tileSize = shortcutSize * 0.88f
            val logoId = when (app.id) {
                "netflix" -> R.drawable.tcl_brand_netflix
                "prime_video" -> R.drawable.tcl_brand_prime_video
                "hbo_max" -> R.drawable.tcl_brand_max
                "disney_plus" -> R.drawable.tcl_brand_disney_plus
                "apple_tv" -> R.drawable.tcl_brand_apple_tv
                "youtube" -> R.drawable.tcl_brand_youtube
                else -> null
            }
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF15151B))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
                    .combinedClickable(
                        onClick = {
                            if (app.id == "iptv") {
                                if (iptvLink.isBlank()) openEditor()
                                else onQuickApp(iptvLink)
                            } else {
                                onQuickApp(app.intentUri ?: app.launchName)
                            }
                        },
                        onLongClick = { if (app.id == "iptv") openEditor() }
                    )
                    .semantics {
                        contentDescription = if (app.id == "iptv")
                            "IPTV; toque para abrir, segure para configurar"
                        else app.label
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    logoId != null -> Image(
                        painter = painterResource(logoId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        contentScale = ContentScale.Fit
                    )
                    app.id == "iptv" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(tileSize * 0.46f)
                        )
                        Text("IPTV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    else -> Text(
                        app.mark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (editingIptv) {
        AlertDialog(
            onDismissRequest = { editingIptv = false },
            title = { Text("Configurar IPTV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "Cole o link de abertura do aplicativo IPTV que você usa na televisão. " +
                            "Toque no ícone para abrir e pressione por mais tempo para alterar."
                    )
                    OutlinedTextField(
                        value = iptvInput,
                        onValueChange = { iptvInput = it; iptvError = "" },
                        label = { Text("Link / Intent do seu IPTV") },
                        placeholder = { Text("intent://... ou https://...") },
                        singleLine = false,
                        minLines = 2
                    )
                    if (iptvError.isNotBlank()) {
                        Text(iptvError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val valid = CustomShortcut.validateIntentUri(iptvInput)
                    if (valid != null) {
                        iptvError = valid
                    } else {
                        val saved = iptvInput.trim()
                        prefs.edit().putString("link", saved).apply()
                        iptvLink = saved
                        editingIptv = false
                        onQuickApp(saved)
                    }
                }) { Text("Salvar e abrir") }
            },
            dismissButton = {
                TextButton(onClick = { editingIptv = false }) { Text("Cancelar") }
            }
        )
    }
}
