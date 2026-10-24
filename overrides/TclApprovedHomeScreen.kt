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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
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
import androidx.core.view.WindowCompat
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


    // Draw the theme background under system insets, instead of leaving white bars.
    val systemView = LocalView.current
    val systemBg = MaterialTheme.colorScheme.background
    SideEffect {
        (systemView.context as? android.app.Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = systemBg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = systemBg.toArgb()
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, systemView)
                .isAppearanceLightStatusBars = systemBg.luminance() > 0.5f
            WindowCompat.getInsetsController(window, systemView)
                .isAppearanceLightNavigationBars = systemBg.luminance() > 0.5f
        }
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(TclSurface, TclBg, TclBg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
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
                        val gap = if (maxWidth < 340.dp) 5.dp else 8.dp
                        val rockerWidth = if (maxWidth < 340.dp) 46.dp else 57.dp
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
            },
            dismissButton = {
                if (headsetStatus?.contains("bloqueou a reconexão") == true) {
                    TextButton(onClick = {
                        val tv = sessionState.connectedDevice
                        val host = tv?.ipAddress
                        val pairing = if (host.isNullOrBlank()) null else
                            TclTvAppApi.savedCode(headsetPrefs, host, tv.id, tv.name)
                        if (!host.isNullOrBlank() && !pairing.isNullOrBlank()) {
                            scope.launch {
                                val opened = TclTvAppApi.openBluetoothSettings(host, pairing)
                                headsetStatus = if (opened) {
                                    "Configurações Bluetooth solicitadas. Na TV, selecione seus fones."
                                } else {
                                    "A TV não permitiu abrir as configurações remotamente. " +
                                    "Abra Controle TCL TV na televisão e selecione Configurações Bluetooth da TV."
                                }
                            }
                        }
                    }) { Text("Abrir Bluetooth na TV") }
                }
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
    // Connected TV manufacturer is inferred from the discovered name, never edited.
    val deviceBrand = remember(tvName) {
        listOf("TCL", "Sony", "Philips", "Hisense", "Xiaomi", "Google TV",
            "Android TV", "Samsung", "LG").firstOrNull {
            tvName.contains(it, ignoreCase = true)
        } ?: "Android TV"
    }
    // A slim toolbar, not a second oversized connected-status card.
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = if (isConnected) Modifier.weight(1f)
                else Modifier.weight(1f).clickable(onClick = onChooseTv),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape)
                .background(if (isConnected) Color(0xFF17DE68) else Color(0xFFFFA427)))
            Text(tvName, color = TclText, fontWeight = FontWeight.Medium,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            if (!isConnected) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Selecionar televisão manualmente",
                    tint = TclMuted, modifier = Modifier.size(13.dp))
            }
        }
        Text(deviceBrand, color = TclText, fontSize = 10.sp, maxLines = 1,
            fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 3.dp))
        TclHeaderAction(Icons.Filled.PowerSettingsNew, "Ligar ou desligar",
            Color(0xFFFF4775), onPower)
        TclHeaderAction(Icons.Filled.Cast, "Transmitir", TclBlue, onCast)
        TclHeaderAction(Icons.Filled.Settings, "Configurações", TclText, onSettings)
    }
}

@Composable
internal fun TclHeaderAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(32.dp)
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
    val outline = RoundedCornerShape(25.dp)
    Box(
        modifier = modifier
            .clip(outline)
            .background(Brush.verticalGradient(listOf(TclSurfaceHigh, TclSurface)))
            .border(1.dp, TclOutline, outline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                headerIcon, contentDescription = if (label == "Volume") "Silenciar" else "TV",
                tint = TclText,
                modifier = Modifier.size(17.dp).clickable(onClick = onHeader)
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar " + label,
                    tint = TclText, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuir " + label,
                    tint = TclText, modifier = Modifier.size(24.dp))
            }
            Text(label, fontSize = 9.sp, color = TclText, maxLines = 1)
        }
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
    var note by remember { mutableStateOf<String?>(null) }

    fun sendDraft() {
        if (draft.isBlank()) {
            note = "Digite algum texto antes de enviar."
            return
        }
        // TV Remote v2: send one complete batch, not per-character deltas.
        onSendText(draft)
        note = "Comando transmitido. Se não aparecer na TV, selecione um campo " +
            "de texto e tente com o teclado virtual da TV oculto."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor de texto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Selecione um campo de pesquisa ou texto na TV. O protocolo " +
                        "pode ignorar o texto quando não há campo ativo. " +
                        "Alguns aplicativos exigem ocultar o teclado virtual da TV.",
                    color = TclMuted
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it; note = null },
                    label = { Text("Digite ou cole aqui") },
                    minLines = 2,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { sendDraft() }),
                    modifier = Modifier.fillMaxWidth()
                )
                if (note != null) Text(note.orEmpty(), color = TclBlue)
            }
        },
        confirmButton = {
            TextButton(onClick = { sendDraft() }) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
