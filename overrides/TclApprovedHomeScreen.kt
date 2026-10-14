package com.hari.androidtvremote.ui.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import com.hari.androidtvremote.utils.Constant

/**
 * The approved 19:52 screenshot is the visual reference for this layout.
 * It deliberately reuses the same remote callbacks, playback component,
 * IPTV/Bluetooth companion support, and existing shortcuts preferences.
 *
 * Preview uses a DIFFERENT applicationId to avoid replacing an installed
 * 1.7 APK whose private signing key is not currently available.
 */
// The same Material theme powers settings and the remote; no dark override.
private val TclBg: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val TclSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer
private val TclSurfaceHigh: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
private val TclOutline: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val TclText: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground
private val TclMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val TclBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary

@Composable
fun TclApprovedHomeScreen(
    currentTab: HomeTab,
    activePadMode: RemotePadMode,
    defaultPadMode: RemotePadMode,
    sessionState: TvRemoteUiState,
    hapticsEnabled: Boolean,
    remoteShelfMode: RemoteShelfMode,
    remoteApps: List<RemoteShortcutApp>,
    onTabSelected: (HomeTab) -> Unit,
    onCyclePadMode: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenSettings: () -> Unit,
    onPower: () -> Unit,
    onQuickApp: (String) -> Unit,
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    onKeyDirection: (Remotemessage.RemoteKeyCode, Remotemessage.RemoteDirection) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onKeyboardText: (String) -> Unit,
    onKeyboardBackspace: (Int) -> Unit,
    onKeyboardEnter: () -> Unit,
    onSendText: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onOpenCastPlayer: (MediaItemUi) -> Unit,
    onClearStatus: () -> Unit,
    onUserRated: () -> Unit = {},
    onUserFeedbackClicked: () -> Unit = {},
    onDismissRatingPrompt: () -> Unit = {}
) {
    if (currentTab == HomeTab.Cast) {
        // Keep the previously implemented Chromecast screen and callbacks.
        HomeScreen(
            currentTab = currentTab,
            activePadMode = activePadMode,
            defaultPadMode = defaultPadMode,
            sessionState = sessionState,
            hapticsEnabled = hapticsEnabled,
            remoteShelfMode = remoteShelfMode,
            remoteApps = remoteApps,
            onTabSelected = onTabSelected,
            onCyclePadMode = onCyclePadMode,
            onOpenDiscovery = onOpenDiscovery,
            onOpenSettings = onOpenSettings,
            onPower = onPower,
            onQuickApp = onQuickApp,
            onRemoteKey = onRemoteKey,
            onVolumeChanged = onVolumeChanged,
            onVolumeUp = onVolumeUp,
            onVolumeDown = onVolumeDown,
            onKeyboardText = onKeyboardText,
            onKeyboardBackspace = onKeyboardBackspace,
            onKeyboardEnter = onKeyboardEnter,
            onToggleVoice = onToggleVoice,
            onOpenCastPlayer = onOpenCastPlayer,
            onClearStatus = onClearStatus,
            onUserRated = onUserRated,
            onUserFeedbackClicked = onUserFeedbackClicked,
            onDismissRatingPrompt = onDismissRatingPrompt
        )
        return
    }

    val context = LocalContext.current
    val view = LocalView.current
    val isConnected = sessionState.connectedDevice != null
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var headsetStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val headsetPrefs = remember(context) {
        context.getSharedPreferences("tcl_tv_installed_apps", android.content.Context.MODE_PRIVATE)
    }

    fun haptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
    fun remoteKey(key: Remotemessage.RemoteKeyCode) {
        haptic()
        if (isConnected) onRemoteKey(key) else onOpenDiscovery()
    }
    val micPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleVoice()
    }
    fun voiceAction() {
        haptic()
        if (!isConnected) {
            onOpenDiscovery()
        } else if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onToggleVoice()
        } else {
            micPermissionRequest.launch(Manifest.permission.RECORD_AUDIO)
        }
    }


    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(Brush.verticalGradient(listOf(TclSurface, TclBg, TclBg)))
    ) {
        val compactHeight = maxHeight < 760.dp
        val tightHeight = maxHeight < 650.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (maxWidth < 360.dp) 8.dp else 12.dp,
                    vertical = if (tightHeight) 4.dp else 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (tightHeight) 6.dp else if (compactHeight) 8.dp else 10.dp
            )
        ) {
                    TclApprovedHeader(
                        tvName = sessionState.connectedDevice?.name ?: "Escolher televisão",
                        isConnected = isConnected,
                        onChooseTv = onOpenDiscovery,
                        onPower = { if (isConnected) onPower() else onOpenDiscovery() },
                        onCast = { onTabSelected(HomeTab.Cast) },
                        onSettings = onOpenSettings
                    )

                    TclInstalledAppsBar(
                        tvHost = sessionState.connectedDevice?.ipAddress,
                        tvIdentity = sessionState.connectedDevice?.id,
                        tvName = sessionState.connectedDevice?.name,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(listOf(TclSurfaceHigh, TclSurface))
                            )
                            .border(1.dp, TclOutline.copy(alpha = .75f), RoundedCornerShape(20.dp))
                            .padding(if (compactHeight) 7.dp else 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Keep all actions: 10 s, play/pause/stop, speeds,
                        // restart episode, episode guide and headphones.
                        PlaybackTransportBar(
                            onRemoteKey = { remoteKey(it) },
                            onHeadphones = {
                                val tv = sessionState.connectedDevice
                                val host = tv?.ipAddress
                                if (host.isNullOrBlank()) {
                                    onOpenDiscovery()
                                } else {
                                    val code = TclTvAppApi.savedCode(
                                        headsetPrefs, host, tv.id, tv.name
                                    )
                                    if (code.isNullOrBlank()) {
                                        headsetStatus = "Vincule primeiro o catálogo de aplicativos " +
                                            "com o código da TCL. O mesmo vínculo será usado para os fones."
                                    } else {
                                        scope.launch {
                                            headsetStatus = "Enviando pedido de reconexão..."
                                            headsetStatus = runCatching {
                                                TclTvAppApi.headphones(host, code)
                                            }.getOrElse { error ->
                                                error.message ?: "Não foi possível consultar a TCL."
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // The old 178dp cap produced a tiny disc despite free
                        // space below it, and clipped the fourth numeric row.
                        // Same footprint in D-pad, pointer and 4-row numeric mode.
                        val gap = if (maxWidth < 350.dp) 6.dp else 8.dp
                        val rockerWidth = if (maxWidth < 350.dp) 53.dp else 57.dp
                        val padWidth = (maxWidth - rockerWidth * 2 - gap * 2)
                            .coerceAtLeast(0.dp).coerceAtMost(280.dp)
                        val padHeight = padWidth
                        Row(
                            modifier = Modifier.fillMaxWidth().height(padHeight),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TclSideRocker(
                                modifier = Modifier.width(rockerWidth).fillMaxHeight(),
                                label = "Volume",
                                headerIcon = Icons.Filled.VolumeUp,
                                fluidLevel = sessionState.volumeFraction,
                                isMuted = sessionState.isMuted,
                                onHeader = {
                                    remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_MUTE)
                                },
                                onIncrease = {
                                    haptic()
                                    if (isConnected) onVolumeUp() else onOpenDiscovery()
                                },
                                onDecrease = {
                                    haptic()
                                    if (isConnected) onVolumeDown() else onOpenDiscovery()
                                }
                            )
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                TclResponsivePad(
                                    mode = activePadMode,
                                    width = padWidth,
                                    height = padHeight,
                                    isConnected = isConnected,
                                    hapticsEnabled = hapticsEnabled,
                                    onShort = { remoteKey(it) },
                                    onHoldStart = {
                                        onKeyDirection(it, Remotemessage.RemoteDirection.START_LONG)
                                    },
                                    onHoldEnd = {
                                        onKeyDirection(it, Remotemessage.RemoteDirection.END_LONG)
                                    }
                                )
                            }
                            TclSideRocker(
                                modifier = Modifier.width(rockerWidth).fillMaxHeight(),
                                label = "Canal",
                                headerIcon = Icons.Filled.Tv,
                                onHeader = {
                                    remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_TV)
                                },
                                onIncrease = {
                                    remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_UP)
                                },
                                onDecrease = {
                                    remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_DOWN)
                                }
                            )
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val buttonDiameter = ((maxWidth - 8.dp * 4) / 5)
                            .coerceAtMost(if (compactHeight) 49.dp else 62.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TclQuickAction(
                                "Teclado", Icons.Filled.Keyboard, buttonDiameter
                            ) {
                                haptic()
                                if (isConnected) showKeyboard = true else onOpenDiscovery()
                            }
                            TclQuickAction("Início", Icons.Filled.Home, buttonDiameter) {
                                remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_HOME)
                            }
                            TclQuickAction(
                                when (activePadMode) {
                                    RemotePadMode.DPad -> "Ponteiro"
                                    RemotePadMode.Touchpad -> "Números"
                                    RemotePadMode.NumberPad -> "Direções"
                                },
                                when (activePadMode) {
                                    RemotePadMode.DPad -> Icons.Filled.TouchApp
                                    RemotePadMode.Touchpad -> Icons.Filled.Dialpad
                                    RemotePadMode.NumberPad -> Icons.Filled.Check
                                },
                                buttonDiameter
                            ) {
                                haptic()
                                onCyclePadMode()
                            }
                            TclQuickAction("Microfone", Icons.Filled.Mic, buttonDiameter) {
                                voiceAction()
                            }
                            TclQuickAction(
                                "Voltar", Icons.Filled.KeyboardBackspace, buttonDiameter
                            ) {
                                remoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
        }
    }

    if (headsetStatus != null) {
        AlertDialog(
            onDismissRequest = { headsetStatus = null },
            title = { Text("Fones Bluetooth") },
            text = { Text(headsetStatus.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { headsetStatus = null }) { Text("Entendi") }
            }
        )
    }

    if (showKeyboard) {
        TclTextInputDialog(
            onDismiss = { showKeyboard = false },
            onAppendText = onKeyboardText,
            onBackspace = onKeyboardBackspace,
            onSendText = onSendText
        )
    }
}

@Composable
private fun TclApprovedHeader(
    tvName: String,
    isConnected: Boolean,
    onChooseTv: () -> Unit,
    onPower: () -> Unit,
    onCast: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(Brush.horizontalGradient(
                listOf(TclSurfaceHigh, TclSurface)
            ))
            .border(1.dp, TclOutline.copy(alpha = .8f), RoundedCornerShape(21.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(43.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(
                    listOf(TclSurfaceHigh, TclSurface, TclSurfaceHigh)
                )).clickable(onClick = onChooseTv),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Tv, contentDescription = "Escolher TV", tint = TclBlue,
                modifier = Modifier.size(31.dp))
        }
        Column(modifier = Modifier.weight(1f).clickable(onClick = onChooseTv)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(tvName, color = TclText, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (isConnected) Color(0xFF17DE68) else Color(0xFFFFA427)))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null,
                    tint = TclMuted, modifier = Modifier.size(16.dp))
            }
            Text(if (isConnected) "TCL  •  Android TV" else "Conecte sua TCL",
                color = TclMuted, maxLines = 1, fontSize = 11.sp)
        }
        TclHeaderAction(
            icon = Icons.Filled.PowerSettingsNew,
            description = "Ligar ou desligar",
            tint = Color(0xFFFF4775), onClick = onPower
        )
        TclHeaderAction(
            icon = Icons.Filled.Cast, description = "Transmitir",
            tint = TclBlue, onClick = onCast
        )
        TclHeaderAction(
            icon = Icons.Filled.Settings, description = "Configurações",
            tint = TclText, onClick = onSettings
        )
    }
}

@Composable
private fun TclHeaderAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(37.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(
                listOf(TclSurfaceHigh, TclSurface)
            ))
            .border(1.dp, TclOutline.copy(alpha = .9f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun TclSideRocker(
    modifier: Modifier,
    label: String,
    headerIcon: ImageVector,
    onHeader: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(35.dp))
            .background(Brush.verticalGradient(
                listOf(TclSurfaceHigh, TclSurface)
            ))
            .border(1.dp, TclOutline, RoundedCornerShape(35.dp))
            .padding(vertical = 7.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            headerIcon, contentDescription = if (label == "Volume") "Silenciar" else "TV",
            tint = TclText, modifier = Modifier.size(24.dp)
                .clickable(onClick = onHeader).padding(1.dp)
        )
        Column(
            modifier = Modifier.weight(1f).padding(top = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Aumentar " + label,
                tint = TclText, modifier = Modifier.size(36.dp)
                    .clickable(onClick = onIncrease).padding(4.dp))
            Box(Modifier.width(3.dp).height(37.dp)
                .clip(RoundedCornerShape(2.dp)).background(TclBlue))
            Icon(Icons.Filled.Remove, contentDescription = "Diminuir " + label,
                tint = TclText, modifier = Modifier.size(34.dp)
                    .clickable(onClick = onDecrease).padding(4.dp))
        }
        Text(label, fontSize = 10.sp, color = TclMuted, maxLines = 1)
    }
}

@Composable
private fun TclQuickAction(
    title: String,
    icon: ImageVector,
    diameter: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(diameter)
                .clip(CircleShape)
                .background(Brush.verticalGradient(
                    listOf(TclSurfaceHigh, TclSurface)
                ))
                .border(1.dp, TclOutline, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = title },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TclText,
                modifier = Modifier.size(if (diameter < 54.dp) 22.dp else 27.dp))
        }
        Text(title, color = TclMuted, fontSize = 11.sp, maxLines = 1,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun TclTextInputDialog(
    onDismiss: () -> Unit,
    onAppendText: (String) -> Unit,
    onBackspace: (Int) -> Unit,
    onSendText: (String) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var liveTyping by rememberSaveable { mutableStateOf(true) }
    var note by remember { mutableStateOf<String?>(null) }

    fun changeText(value: String) {
        val previous = draft
        if (value == previous) return
        draft = value
        note = null
        if (!liveTyping) return

        // Deliver only the changed portion to the Android TV Remote v2 IME.
        // Resending the entire buffer at every keystroke can duplicate text.
        when {
            value.startsWith(previous) -> onAppendText(value.substring(previous.length))
            previous.startsWith(value) -> onBackspace(previous.length - value.length)
            else -> {
                // Composing text, selection replacement or pasted correction:
                // clear the previously mirrored buffer and insert the new one.
                if (previous.isNotEmpty()) onBackspace(previous.length)
                if (value.isNotEmpty()) onAppendText(value)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teclado da TCL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Abra e selecione primeiro um campo de pesquisa ou texto na TV. " +
                    "O teclado do celular não controla menus que não aceitam digitação.",
                    color = TclMuted
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Digitar na TV ao vivo", color = TclText)
                    Switch(
                        checked = liveTyping,
                        onCheckedChange = { next ->
                            if (next && !liveTyping && draft.isNotEmpty()) {
                                onSendText(draft)
                                note = "Texto enviado ao campo que estiver ativo na TV."
                            }
                            liveTyping = next
                        }
                    )
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = ::changeText,
                    label = { Text("Digite ou cole aqui") },
                    minLines = 2,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!liveTyping && draft.isNotEmpty()) {
                            onSendText(draft)
                            note = "Texto enviado ao campo que estiver ativo na TV."
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
                if (note != null) Text(note.orEmpty(), color = TclBlue)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!liveTyping && draft.isNotEmpty()) {
                        onSendText(draft)
                        note = "Texto enviado ao campo que estiver ativo na TV."
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (liveTyping) "Concluir" else "Enviar texto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
