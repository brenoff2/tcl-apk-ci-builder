package com.hari.androidtvremote.ui.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private val TclBg = Color(0xFF050B13)
private val TclSurface = Color(0xFF101D2C)
private val TclSurfaceHigh = Color(0xFF1A2D43)
private val TclOutline = Color(0xFF254562)
private val TclText = Color(0xFFEAF2FF)
private val TclMuted = Color(0xFFA8BAD6)
private val TclBlue = Color(0xFF298FFF)

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
    onManageApps: () -> Unit,
    onPower: () -> Unit,
    onQuickApp: (String) -> Unit,
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onKeyboardText: (String) -> Unit,
    onKeyboardBackspace: (Int) -> Unit,
    onKeyboardEnter: () -> Unit,
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

    // Preserve a user-created order, but show the seven approved apps first
    // when no preference was saved. Extra previously known apps stay accessible.
    val appsToRender = remember(context, remoteApps) {
        val prefs = context.getSharedPreferences(Constant.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val explicitOrder = prefs.getString(Constant.PREF_REMOTE_APP_ORDER, null)
        if (!explicitOrder.isNullOrBlank()) remoteApps
        else {
            val first = listOf(
                "netflix", "prime_video", "hbo_max", "disney_plus",
                "apple_tv", "youtube", "iptv"
            )
            val appsById = remoteApps.associateBy { it.id }
            first.mapNotNull(appsById::get) + remoteApps.filter { it.id !in first }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TclBlue,
            onPrimary = Color.White,
            surface = TclBg,
            surfaceContainer = TclSurface,
            surfaceContainerHigh = TclSurfaceHigh,
            secondaryContainer = TclSurfaceHigh,
            onSecondaryContainer = TclText,
            outlineVariant = TclOutline,
            onSurface = TclText,
            onSurfaceVariant = TclMuted
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1726), TclBg, Color(0xFF02060B))
                )
            )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    TclBottomTabs(
                        currentTab = currentTab,
                        onRemote = { onTabSelected(HomeTab.Remote) },
                        onCast = { onTabSelected(HomeTab.Cast) }
                    )
                }
            ) { scaffoldPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TclApprovedHeader(
                        tvName = sessionState.connectedDevice?.name ?: "Escolher televisão",
                        isConnected = isConnected,
                        onChooseTv = onOpenDiscovery,
                        onPower = { if (isConnected) onPower() else onOpenDiscovery() },
                        onCast = { onTabSelected(HomeTab.Cast) },
                        onSettings = onOpenSettings
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aplicativos", fontWeight = FontWeight.SemiBold,
                                color = TclText, fontSize = 17.sp)
                            Text(
                                "Ver todos  ›",
                                modifier = Modifier.clickable(onClick = onManageApps)
                                    .padding(6.dp)
                                    .semantics { contentDescription = "Ver e ordenar aplicativos" },
                                color = TclMuted, fontSize = 13.sp
                            )
                        }
                        AppShortcutStripSquare(
                            quickApps = appsToRender,
                            onQuickApp = { id ->
                                if (isConnected) onQuickApp(id) else onOpenDiscovery()
                            },
                            onRemoteKey = { remoteKey(it) },
                            shortcutSize = 79.dp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(listOf(TclSurface, Color(0xFF0B1725)))
                            )
                            .border(1.dp, TclOutline.copy(alpha = .75f), RoundedCornerShape(20.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Cast, contentDescription = null,
                                tint = TclBlue, modifier = Modifier.size(18.dp))
                            Text("Reprodução", color = TclMuted, fontSize = 14.sp)
                        }
                        // Keep all actions: 10 s, play/pause/stop, speeds,
                        // restart episode, episode guide and headphones.
                        PlaybackTransportBar(onRemoteKey = { remoteKey(it) })
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val gap = if (maxWidth < 350.dp) 7.dp else 10.dp
                        val rockerWidth = if (maxWidth < 350.dp) 51.dp else 59.dp
                        val padSize = (maxWidth - rockerWidth * 2 - gap * 2)
                            .coerceAtLeast(0.dp).coerceAtMost(270.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(padSize),
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
                            Box(modifier = Modifier.weight(1f)) {
                                // Native Android TV D-pad/touchpad/number-pad,
                                // preserving hit zones, haptics and key mapping.
                                RemotePadStage(
                                    activePadMode = activePadMode,
                                    stageHeight = padSize,
                                    isConnected = isConnected,
                                    hapticsEnabled = hapticsEnabled,
                                    onAction = {
                                        if (isConnected) onRemoteKey(it) else onOpenDiscovery()
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
                        val buttonDiameter = ((maxWidth - 4 * 8.dp) / 5)
                            .coerceAtMost(70.dp)
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
        }
    }

    if (showKeyboard) {
        KeyboardDialog(
            onDismiss = { showKeyboard = false },
            onInsertText = onKeyboardText,
            onBackspace = { onKeyboardBackspace(1) },
            onEnter = onKeyboardEnter
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
                listOf(Color(0xFF101B2A), Color(0xFF08111E))
            ))
            .border(1.dp, TclOutline.copy(alpha = .8f), RoundedCornerShape(21.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier.size(43.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(
                    listOf(Color(0xFF293F7A), Color(0xFF0E1D36), Color(0xFF14213B))
                )).clickable(onClick = onChooseTv),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Tv, contentDescription = "Escolher TV", tint = Color(0xFF9EC4FF),
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
            tint = Color(0xFF85CAFF), onClick = onCast
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
                listOf(Color(0xFF22354B), Color(0xFF0D1524))
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
                listOf(Color(0xFF182A3F), Color(0xFF07111E))
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
                    listOf(Color(0xFF1A3047), Color(0xFF081522))
                ))
                .border(1.dp, TclOutline, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = title },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TclText,
                modifier = Modifier.size(27.dp))
        }
        Text(title, color = TclMuted, fontSize = 11.sp, maxLines = 1,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun TclBottomTabs(
    currentTab: HomeTab,
    onRemote: () -> Unit,
    onCast: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(Brush.verticalGradient(
                listOf(Color(0xFF14263B), Color(0xFF071321))
            ))
            .border(1.dp, TclOutline, RoundedCornerShape(38.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TclBottomTab("Remote", Icons.Filled.Tv,
            selected = currentTab == HomeTab.Remote,
            modifier = Modifier.weight(1f), onClick = onRemote)
        Box(Modifier.width(1.dp).height(33.dp)
            .background(TclOutline))
        TclBottomTab("Cast", Icons.Filled.Cast,
            selected = currentTab == HomeTab.Cast,
            modifier = Modifier.weight(1f), onClick = onCast)
    }
}

@Composable
private fun TclBottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label,
            tint = if (selected) TclBlue else TclMuted,
            modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(1.dp))
        Text(label, fontSize = 12.sp, color = if (selected) TclBlue else TclMuted)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.width(66.dp).height(3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) TclBlue else Color.Transparent)
        )
    }
}
