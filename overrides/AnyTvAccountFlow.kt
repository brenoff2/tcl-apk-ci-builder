package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal val LocalAnyTvOpenAccount = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Guest-first UI: never hold the catalog behind an account gate.
 * Only the Supabase SDK may mark the user as SignedIn; a browser
 * redirect or an earlier guest preference is not an authentication proof.
 */
@Composable
internal fun AnyTvAccountFlow(
    gateway: AnyTvAccountGateway?,
    existingApp: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember(gateway) { mutableStateOf<AnyTvAccountSession>(AnyTvAccountSession.Loading) }
    var showAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gateway) {
        if (gateway == null) {
            state = AnyTvAccountSession.Guest
            return@LaunchedEffect
        }
        try {
            state = gateway.restoreSession()
            gateway.sessionUpdates().collect { update ->
                state = update
                if (update is AnyTvAccountSession.SignedIn) {
                    busy = false
                    message = null
                }
            }
        } catch (_: Exception) {
            state = AnyTvAccountSession.Choosing
            message = "Não foi possível verificar a sessão. O catálogo continua disponível."
        }
    }

    // The account action lives in the main remote header, not over TV discovery controls.
    CompositionLocalProvider(LocalAnyTvOpenAccount provides { showAccount = true }) {
        existingApp()
    }

    if (showAccount) {
        val signedIn = state is AnyTvAccountSession.SignedIn
        AlertDialog(
            onDismissRequest = { if (!busy) showAccount = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFF292929),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            title = { Text("Minha conta", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(when (state) {
                        is AnyTvAccountSession.SignedIn -> "Conectado com Google"
                        AnyTvAccountSession.Loading -> "Verificando sessão…"
                        else -> "Modo convidado · dados salvos neste aparelho"
                    })
                    if (signedIn) {
                        val user = state as? AnyTvAccountSession.SignedIn
                        if (!user?.email.isNullOrBlank()) Text(user?.email.orEmpty())
                        Text("Conta conectada. Favoritos e acompanhamento permanecem salvos neste aparelho.")
                    } else {
                        Text("Você pode entrar com Google. O catálogo também funciona sem conta.")
                    }
                    if (!message.isNullOrBlank()) Text(message.orEmpty())
                    if (busy) CircularProgressIndicator()
                }
            },
            confirmButton = {
                if (signedIn && gateway != null) {
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try {
                                gateway.signOut()
                                state = AnyTvAccountSession.Guest
                                message = null
                            } catch (_: Exception) {
                                message = "Não foi possível sair da conta."
                            } finally { busy = false }
                        }
                    }) { Text("Sair da conta") }
                } else if (gateway != null) {
                    Button(enabled = !busy, onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                gateway.signInWithGoogle()
                                showAccount = false
                            } catch (_: Exception) {
                                message = "Não foi possível iniciar o login Google."
                            } finally { busy = false }
                        }
                    }) { Text("Entrar com Google") }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showAccount = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
