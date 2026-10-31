package com.hari.androidtvremote.ui.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Guest-first wrapper for the existing app. The sign-in path is intentionally
 * not invoked without a working, configured auth gateway.
 * This host must be wired into MainActivity only when authentication is live.
 */
@Composable
internal fun AnyTvEntryHost(
    verifiedUserId: String?,
    onLoginRequested: () -> Unit,
    existingApp: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var guest by remember(context) {
        mutableStateOf(AnyTvEntryPreference.isGuest(context))
    }
    when (anyTvInitialAccountSession(guest, verifiedUserId)) {
        AnyTvAccountSession.Choosing -> AnyTvAccountEntry(
            onContinueAsGuest = {
                if (AnyTvEntryPreference.selectGuest(context)) guest = true
            },
            onSignIn = onLoginRequested,
        )
        AnyTvAccountSession.Guest, is AnyTvAccountSession.SignedIn -> existingApp()
        AnyTvAccountSession.Loading, is AnyTvAccountSession.Error -> existingApp()
    }
}
