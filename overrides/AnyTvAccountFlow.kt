package com.hari.androidtvremote.ui.app

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Guest-first account flow, bound to a verified upstream Auth gateway.
 * Parent MUST supply an isolated AnyTV client and handle OAuth deep links.
 * Do not mount this flow until a dedicated self-hosted Auth URL is operational.
 */
@Composable
internal fun AnyTvAccountFlow(
    gateway: AnyTvAccountGateway,
    existingApp: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(gateway) {
        mutableStateOf<AnyTvAccountSession>(AnyTvAccountSession.Loading)
    }
    var showingLogin by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gateway) {
        state = try {
            when (val restored = gateway.restoreSession()) {
                is AnyTvAccountSession.SignedIn -> restored
                else -> if (AnyTvEntryPreference.isGuest(context))
                    AnyTvAccountSession.Guest else AnyTvAccountSession.Choosing
            }
        } catch (_: Exception) {
            // A saved guest choice works even if the authentication host is offline.
            if (AnyTvEntryPreference.isGuest(context))
                AnyTvAccountSession.Guest else AnyTvAccountSession.Choosing
        }
    }

    fun submit(action: suspend () -> AnyTvAccountSession) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                val result = action()
                if (result is AnyTvAccountSession.SignedIn) {
                    state = result
                    showingLogin = false
                } else {
                    message = "Verifique seu e-mail ou conclua a autorização para entrar."
                }
            } catch (_: Exception) {
                message = "Não foi possível autenticar. Confira os dados e tente novamente."
            } finally {
                busy = false
            }
        }
    }

    when {
        state is AnyTvAccountSession.SignedIn || state == AnyTvAccountSession.Guest ->
            existingApp()

        state == AnyTvAccountSession.Loading -> CircularProgressIndicator()

        showingLogin -> AnyTvSignInScreen(
            busy = busy, error = message,
            onSignIn = { email, password ->
                submit { gateway.signInWithEmail(email, password) }
            },
            onSignUp = { email, password ->
                submit { gateway.signUpWithEmail(email, password) }
            },
            onGoogle = { submit { gateway.signInWithGoogle() } },
            onResetPassword = { email ->
                if (!busy) {
                    busy = true
                    scope.launch {
                        try {
                            gateway.resetPassword(email)
                            message = "Se o e-mail estiver cadastrado, enviaremos instruções."
                        } catch (_: Exception) {
                            message = "Não foi possível solicitar a recuperação."
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            onGuest = {
                if (AnyTvEntryPreference.selectGuest(context)) {
                    state = AnyTvAccountSession.Guest
                    showingLogin = false
                }
            }
        )

        else -> AnyTvAccountEntry(
            onContinueAsGuest = {
                if (AnyTvEntryPreference.selectGuest(context))
                    state = AnyTvAccountSession.Guest
            },
            onSignIn = { showingLogin = true }
        )
    }
}
