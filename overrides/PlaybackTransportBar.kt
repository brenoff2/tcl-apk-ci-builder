package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hari.androidtvremote.androidLib.remote.Remotemessage

/**
 * Playback controls for Android TV Remote v2.
 * Streaming apps interpret seeking and repeat acceleration independently:
 * the x labels represent repeated transport requests, not confirmed speed.
 */
@Composable
fun PlaybackTransportBar(
    onRemoteKey: (Remotemessage.RemoteKeyCode) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(1, 2, 4, 8, 16)
    var forwardRequests by rememberSaveable { mutableStateOf(0) }
    var rewindRequests by rememberSaveable { mutableStateOf(0) }

    fun reset() {
        forwardRequests = 0
        rewindRequests = 0
    }

    fun accelerateForward() {
        rewindRequests = 0
        if (forwardRequests == levels.size) {
            reset()
            onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY)
        } else {
            forwardRequests++
            onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD)
        }
    }

    fun accelerateBackward() {
        forwardRequests = 0
        if (rewindRequests == levels.size) {
            reset()
            onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY)
        } else {
            rewindRequests++
            onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_REWIND)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PlaybackChip(
                text = "−10 s", description = "Voltar dez segundos conforme a TV",
                modifier = Modifier.weight(1.1f)
            ) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT)
            }
            PlaybackChip(text = "▶", description = "Reproduzir", modifier = Modifier.weight(0.8f)) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY)
            }
            PlaybackChip(text = "Ⅱ", description = "Pausar", modifier = Modifier.weight(0.8f)) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PAUSE)
            }
            PlaybackChip(text = "■", description = "Parar", modifier = Modifier.weight(0.8f)) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_STOP)
            }
            PlaybackChip(
                text = "+10 s", description = "Avançar dez segundos conforme a TV",
                modifier = Modifier.weight(1.1f)
            ) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PlaybackChip(
                text = "≪ " + (if (rewindRequests == 0) "1×" else levels[rewindRequests - 1].toString() + "×"),
                description = "Retrocesso acelerado: toques sucessivos 1, 2, 4, 8 e 16 vezes",
                modifier = Modifier.weight(1f),
                onClick = { accelerateBackward() }
            )
            PlaybackChip(text = "▶ Normal", description = "Voltar à reprodução normal", modifier = Modifier.weight(1.2f)) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY)
            }
            PlaybackChip(
                text = if (forwardRequests == 0) "1× ≫" else levels[forwardRequests - 1].toString() + "× ≫",
                description = "Avanço acelerado: toques sucessivos 1, 2, 4, 8 e 16 vezes",
                modifier = Modifier.weight(1f),
                onClick = { accelerateForward() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PlaybackChip(
                text = "↺ Do início",
                description = "Solicita reinício do episódio; alguns streamings interpretam como anterior",
                modifier = Modifier.weight(1f)
            ) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PREVIOUS)
            }
            PlaybackChip(
                text = "☷ Episódios",
                description = "Voltar para a tela anterior para selecionar episódios",
                modifier = Modifier.weight(1f)
            ) {
                reset()
                onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK)
            }
        }
    }
}

@Composable
private fun PlaybackChip(
    text: String,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(40.dp).semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
