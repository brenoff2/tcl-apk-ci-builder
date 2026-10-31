package com.hari.androidtvremote.ui.app

/**
 * Auth-independent state for AnyTV account entry. Supabase Auth is the sole
 * authority for SignedIn: do not infer it from a saved email or guest flag.
 */
internal sealed interface AnyTvAccountSession {
    data object Choosing : AnyTvAccountSession
    data object Guest : AnyTvAccountSession
    data object Loading : AnyTvAccountSession
    data class SignedIn(val userId: String) : AnyTvAccountSession {
        init { require(userId.isNotBlank()) }
    }
    data class Error(val message: String) : AnyTvAccountSession
}

/** Distinct from TV device pairing and its Cloudflare authorization. */
internal interface AnyTvAccountGateway {
    suspend fun restoreSession(): AnyTvAccountSession
    suspend fun signInWithEmail(email: String, password: String): AnyTvAccountSession
    suspend fun signUpWithEmail(email: String, password: String): AnyTvAccountSession
    suspend fun signInWithGoogle(): AnyTvAccountSession
    suspend fun resetPassword(email: String)
    suspend fun signOut()
}

/**
 * Call only after verified Supabase session restoration; it is never an
 * authentication shortcut. Existing users can remain guests on first upgrade.
 */
internal fun anyTvInitialAccountSession(
    guestPreviouslySelected: Boolean,
    verifiedUserId: String?
): AnyTvAccountSession = when {
    !verifiedUserId.isNullOrBlank() -> AnyTvAccountSession.SignedIn(verifiedUserId)
    guestPreviouslySelected -> AnyTvAccountSession.Guest
    else -> AnyTvAccountSession.Choosing
}
