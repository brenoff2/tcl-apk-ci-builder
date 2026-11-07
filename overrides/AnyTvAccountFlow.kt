package com.hari.androidtvremote.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Guest-first UI: never hold the catalog behind an account gate.
 * Only the Supabase SDK may mark the user as SignedIn; a browser
 * redirect or an earlier guest preference is not an authentication proof.
 */
@Composable
internal fun AnyTvAccountFlow(
    gateway: AnyTvAccountGateway,
    existingApp: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember(gateway) { mutableStateOf<AnyTvAccountSession>(AnyTvAccountSession.Loading) }
    var showAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gateway) {
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
            message = "Nao foi possivel verificar a sessao. O catalogo continua disponivel."
        }
    }

    // Keep navigation and local libraries mounted regardless of account status.
    Box(modifier = Modifier.fillMaxSize()) {
        existingApp()
        IconButton(
            onClick = { showAccount = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
        ) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Minha Conta")
        }
    }

    if (showAccount) {
        val signedIn = state is AnyTvAccountSession.SignedIn
        AlertDialog(
            onDismissRequest = { if (!busy) showAccount = false },
            title = { Text("Minha Conta") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(when (state) {
                        is AnyTvAccountSession.SignedIn -> "Conectado com Google"
                        AnyTvAccountSession.Loading -> "Verificando sessao..."
                        else -> "Modo convidado - dados salvos neste aparelho"
                    })
                    if (signedIn) {
                        Text("Conta autenticada pelo Supabase. Sua lista ainda e local, nao sincronizada.")
                    } else {
                        Text("Entre com Google quando desejar. O catalogo nao exige login.")
                    }
                    if (!message.isNullOrBlank()) Text(message.orEmpty())
                    if (busy) CircularProgressIndicator()
                }
            },
            confirmButton = {
                if (signedIn) {
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try {
                                gateway.signOut()
                                state = AnyTvAccountSession.Guest
                                message = null
                            } catch (_: Exception) {
                                message = "Nao foi possivel sair da conta."
                            } finally { busy = false }
                        }
                    }) { Text("Sair da conta") }
                } else {
                    Button(enabled = !busy, onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                gateway.signInWithGoogle()
                                showAccount = false
                            } catch (_: Exception) {
                                message = "Nao foi possivel iniciar o login Google."
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
