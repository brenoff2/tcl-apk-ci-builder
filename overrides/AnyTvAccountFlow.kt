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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Google-only first slice: launch the system browser and wait for an authenticated
 * Supabase session. Returning from the browser is not itself proof of sign-in.
 * Email/password and library synchronization are intentionally not enabled here.
 */
@Composable
internal fun AnyTvAccountFlow(
    gateway: AnyTvAccountGateway,
    existingApp: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(gateway) {
        mutableStateOf<AnyTvAccountSession>(
            if (AnyTvEntryPreference.isGuest(context))
                AnyTvAccountSession.Guest else AnyTvAccountSession.Loading
        )
    }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gateway) {
        try {
            val restored = gateway.restoreSession()
            state = if (restored is AnyTvAccountSession.SignedIn) restored
                else if (AnyTvEntryPreference.isGuest(context))
                    AnyTvAccountSession.Guest else AnyTvAccountSession.Choosing
            gateway.sessionUpdates().collect { update ->
                state = when (update) {
                    is AnyTvAccountSession.SignedIn -> update
                    AnyTvAccountSession.Loading -> state
                    else -> if (AnyTvEntryPreference.isGuest(context))
                        AnyTvAccountSession.Guest else AnyTvAccountSession.Choosing
                }
                if (state is AnyTvAccountSession.SignedIn) {
                    busy = false
                    message = null
                }
            }
        } catch (_: Exception) {
            state = if (AnyTvEntryPreference.isGuest(context))
                AnyTvAccountSession.Guest else AnyTvAccountSession.Choosing
        }
    }

    when (state) {
        is AnyTvAccountSession.SignedIn, AnyTvAccountSession.Guest -> existingApp()
        AnyTvAccountSession.Loading -> CircularProgressIndicator()
        else -> AnyTvAccountEntry(
            busy = busy,
            error = message,
            onContinueAsGuest = {
                if (AnyTvEntryPreference.selectGuest(context))
                    state = AnyTvAccountSession.Guest
            },
            onSignIn = {
                if (!busy) {
                    busy = true
                    message = null
                    scope.launch {
                        try {
                            // Opening Google does not authenticate; the SDK session flow does.
                            gateway.signInWithGoogle()
                        } catch (_: Exception) {
                            message = "Nao foi possivel iniciar o login Google. Tente novamente."
                        } finally {
                            busy = false
                        }
                    }
                }
            }
        )
    }
}
