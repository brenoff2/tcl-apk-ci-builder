package com.hari.androidtvremote.ui.app

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.hari.androidtvremote.androidLib.remote.Remotemessage

private val alternateAppLinks = mapOf(
    "netflix" to "netflix://",
    "prime_video" to "https://app.primevideo.com",
    "hbo_max" to "https://play.max.com",
    "disney_plus" to "https://www.disneyplus.com",
    "apple_tv" to "https://tv.apple.com",
    "youtube" to "https://www.youtube.com"
)

/**
 * 2/3-size tiles, logos fill the tile with no internal transparent margins.
 * The TV companion enumerates installed TV apps and selects IPTV on the TV.
 * The phone cannot enumerate TV packages via Android TV Remote v2 alone.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppShortcutStripSquare(
    quickApps: List<RemoteShortcutApp>,
    onQuickApp: (String) -> Unit,
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    shortcutSize: Dp
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("tcl_tv_companion", Context.MODE_PRIVATE)
    }
    var showSetup by remember { mutableStateOf(false) }
    var editStreaming by remember { mutableStateOf<RemoteShortcutApp?>(null) }
    var requestedChange by remember { mutableStateOf(false) }

    fun iptvAction(change: Boolean) {
        if (prefs.getBoolean("confirmed", false)) {
            onRemoteKey(if (change)
                Remotemessage.RemoteKeyCode.KEYCODE_PROG_YELLOW
                else Remotemessage.RemoteKeyCode.KEYCODE_PROG_BLUE)
        } else {
            requestedChange = change
            showSetup = true
        }
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickApps.forEach { app ->
            val iconId = when (app.id) {
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
                    .size(shortcutSize * 0.66f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF141924))
                    .combinedClickable(
                        onClick = {
                            if (app.id == "iptv") iptvAction(false)
                            else onQuickApp(app.intentUri ?: app.launchName)
                        },
                        onLongClick = {
                            if (app.id == "iptv") iptvAction(true)
                            else if (app.id in alternateAppLinks) editStreaming = app
                        }
                    )
                    .semantics {
                        contentDescription = if (app.id == "iptv")
                            "IPTV: toque para abrir, segure para escolher outro app instalado na TV"
                        else app.label
                    },
                contentAlignment = Alignment.Center
            ) {
                if (iconId != null) {
                    Image(
                        painter = painterResource(iconId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = app.mark,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSetup) {
        TvCompanionSetupDialog(
            description = "Para escolher os aplicativos realmente instalados na TCL, " +
                "instale o complemento Controle TCL TV na televisão e ative o serviço " +
                "em Configurações → Acessibilidade. A lista aparece na própria TV.",
            onReady = {
                prefs.edit().putBoolean("confirmed", true).apply()
                showSetup = false
                onRemoteKey(if (requestedChange)
                    Remotemessage.RemoteKeyCode.KEYCODE_PROG_YELLOW
                    else Remotemessage.RemoteKeyCode.KEYCODE_PROG_BLUE)
            },
            onDismiss = { showSetup = false },
            onFallback = {
                showSetup = false
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_HOME)
            }
        )
    }

    val app = editStreaming
    if (app != null) {
        AlertDialog(
            onDismissRequest = { editStreaming = null },
            title = { Text("Abrir " + app.label) },
            text = {
                Text("Se o atalho não abrir na TCL, teste o link nativo do serviço. " +
                     "Alguns fabricantes não aceitam abertura pelo identificador Android.")
            },
            confirmButton = {
                TextButton(onClick = {
                    editStreaming = null
                    onQuickApp(app.launchName)
                }) { Text("Abrir app") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editStreaming = null
                    onQuickApp(alternateAppLinks.getValue(app.id))
                }) { Text("Link alternativo") }
            }
        )
    }
}

@Composable
fun TvCompanionSetupDialog(
    description: String,
    onReady: () -> Unit,
    onDismiss: () -> Unit,
    onFallback: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complemento Controle TCL TV") },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onReady) { Text("Já configurei na TV") }
        },
        dismissButton = {
            if (onFallback != null) {
                TextButton(onClick = onFallback) { Text("Ver apps na TV") }
            } else {
                TextButton(onClick = onDismiss) { Text("Agora não") }
            }
        }
    )
}
