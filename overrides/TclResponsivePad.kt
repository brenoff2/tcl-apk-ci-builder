package com.hari.androidtvremote.ui.app

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Full-width, actual-press D-pad and 4-row numeric grid.
 *
 * The regular Android TV remote protocol explicitly supports START_LONG and
 * END_LONG. Send SHORT only for a tap; bracket a held key by the pair, and
 * ALWAYS release on pointer cancellation. This avoids guessing a repeat
 * interval and lets Android TV decide whether to open context menus, etc.
 */
@Composable
fun TclResponsivePad(
    mode: RemotePadMode,
    width: Dp,
    height: Dp,
    isConnected: Boolean,
    hapticsEnabled: Boolean,
    onShort: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldStart: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldEnd: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    when (mode) {
        RemotePadMode.DPad -> TclHoldDPad(
            size = width,
            isConnected = isConnected,
            hapticsEnabled = hapticsEnabled,
            onShort = onShort,
            onHoldStart = onHoldStart,
            onHoldEnd = onHoldEnd
        )
        RemotePadMode.Touchpad -> RemotePadStage(
            activePadMode = RemotePadMode.Touchpad,
            stageHeight = width,
            isConnected = isConnected,
            hapticsEnabled = hapticsEnabled,
            onAction = onShort
        )
        RemotePadMode.NumberPad -> TclNumberPad(
            width = width,
            height = height,
            onShort = onShort
        )
    }
}

@Composable
private fun TclHeldZone(
    modifier: Modifier,
    label: String,
    key: Remotemessage.RemoteKeyCode,
    isConnected: Boolean,
    hapticsEnabled: Boolean,
    onShort: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldStart: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldEnd: (Remotemessage.RemoteKeyCode) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val latestTap by rememberUpdatedState(onShort)
    val latestStart by rememberUpdatedState(onHoldStart)
    val latestEnd by rememberUpdatedState(onHoldEnd)
    Box(
        modifier = modifier
            .semantics { contentDescription = "$label — toque ou mantenha pressionado" }
            .pointerInput(key, isConnected, hapticsEnabled) {
                detectTapGestures(onPress = {
                    if (!isConnected) {
                        latestTap(key)
                        tryAwaitRelease()
                    } else {
                        if (hapticsEnabled) view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.KEYBOARD_TAP
                        )
                        val wasReleased = withTimeoutOrNull(270L) {
                            tryAwaitRelease()
                        }
                        when (wasReleased) {
                            true -> latestTap(key)
                            null -> {
                                // Short taps use SHORT. A sustained press sends
                                // START_LONG and END_LONG with a guaranteed
                                // minimum TV-side duration; otherwise a 300ms
                                // tap could look like just 20ms of key hold.
                                latestStart(key)
                                val startedAt = SystemClock.uptimeMillis()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    withContext(NonCancellable) {
                                        val elapsed = SystemClock.uptimeMillis() - startedAt
                                        if (elapsed < 430L) delay(430L - elapsed)
                                        // Never strand a TV key on navigation/cancel.
                                        latestEnd(key)
                                    }
                                }
                            }
                            false -> Unit
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TclHoldDPad(
    size: Dp,
    isConnected: Boolean,
    hapticsEnabled: Boolean,
    onShort: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldStart: (Remotemessage.RemoteKeyCode) -> Unit,
    onHoldEnd: (Remotemessage.RemoteKeyCode) -> Unit,
) {
    val top = MaterialTheme.colorScheme.surfaceContainerHigh
    val bottom = MaterialTheme.colorScheme.surfaceContainer
    val accent = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurface
    val arrow = (size * .17f).coerceIn(28.dp, 50.dp)
    @Composable
    fun directional(
        modifier: Modifier,
        title: String,
        key: Remotemessage.RemoteKeyCode,
        icon: ImageVector
    ) {
        TclHeldZone(
            modifier = modifier,
            label = title,
            key = key,
            isConnected = isConnected,
            hapticsEnabled = hapticsEnabled,
            onShort = onShort,
            onHoldStart = onHoldStart,
            onHoldEnd = onHoldEnd
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(arrow))
        }
    }

    Box(
        modifier = Modifier.size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(top, bottom)))
            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Four independent, generous hit rectangles around the central OK.
        // None is squeezed by fixed heights from media cards above.
        directional(
            Modifier.align(Alignment.TopCenter)
                .width(size * .61f).height(size * .32f),
            "Para cima",
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_UP,
            Icons.Filled.KeyboardArrowUp
        )
        directional(
            Modifier.align(Alignment.BottomCenter)
                .width(size * .61f).height(size * .32f),
            "Para baixo",
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_DOWN,
            Icons.Filled.KeyboardArrowDown
        )
        directional(
            Modifier.align(Alignment.CenterStart)
                .width(size * .32f).height(size * .61f),
            "Para esquerda",
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT,
            Icons.Filled.KeyboardArrowLeft
        )
        directional(
            Modifier.align(Alignment.CenterEnd)
                .width(size * .32f).height(size * .61f),
            "Para direita",
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT,
            Icons.Filled.KeyboardArrowRight
        )
        TclHeldZone(
            modifier = Modifier.size(size * .19f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(
                    accent.copy(alpha = 0.38f),
                    top
                )))
                .border(2.dp, accent, CircleShape),
            label = "OK",
            key = Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER,
            isConnected = isConnected,
            hapticsEnabled = hapticsEnabled,
            onShort = onShort,
            onHoldStart = onHoldStart,
            onHoldEnd = onHoldEnd
        ) {
            Text(
                "OK",
                color = foreground,
                fontSize = if (size < 228.dp) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TclNumberPad(
    width: Dp,
    height: Dp,
    onShort: (Remotemessage.RemoteKeyCode) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )
    val codes = listOf(
        Remotemessage.RemoteKeyCode.KEYCODE_1,
        Remotemessage.RemoteKeyCode.KEYCODE_2,
        Remotemessage.RemoteKeyCode.KEYCODE_3,
        Remotemessage.RemoteKeyCode.KEYCODE_4,
        Remotemessage.RemoteKeyCode.KEYCODE_5,
        Remotemessage.RemoteKeyCode.KEYCODE_6,
        Remotemessage.RemoteKeyCode.KEYCODE_7,
        Remotemessage.RemoteKeyCode.KEYCODE_8,
        Remotemessage.RemoteKeyCode.KEYCODE_9,
        Remotemessage.RemoteKeyCode.KEYCODE_STAR,
        Remotemessage.RemoteKeyCode.KEYCODE_0,
        Remotemessage.RemoteKeyCode.KEYCODE_POUND
    )
    Column(
        modifier = Modifier.width(width).height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp, MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(24.dp)
            )
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEachIndexed { rowIndex, items ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items.forEachIndexed { columnIndex, label ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clip(RoundedCornerShape(17.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                onShort(codes[rowIndex * 3 + columnIndex])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = if (width < 235.dp) 22.sp else 25.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
