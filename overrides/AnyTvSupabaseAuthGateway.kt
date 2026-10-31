package com.hari.androidtvremote.ui.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google

/**
 * Auth adapter backed exclusively by the upstream supabase-kt Auth SDK.
 * The client is injected from the AnyTV-specific self-hosted endpoint,
 * never from the existing CLFCJ Supabase tenant.
 *
 * Do not activate the login route until a dedicated endpoint and session
 * callback (including Google deep links) have passed integration tests.
 */
internal class AnyTvSupabaseAuthGateway(
    private val client: SupabaseClient
) : AnyTvAccountGateway {
    private fun session(): AnyTvAccountSession =
        client.auth.currentUserOrNull()?.id?.let {
            AnyTvAccountSession.SignedIn(it)
        } ?: AnyTvAccountSession.Choosing

    override suspend fun restoreSession(): AnyTvAccountSession {
        client.auth.awaitInitialization()
        return session()
    }

    override suspend fun signInWithEmail(
        email: String, password: String
    ): AnyTvAccountSession {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return session()
    }

    override suspend fun signUpWithEmail(
        email: String, password: String
    ): AnyTvAccountSession {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // Email confirmation may be mandatory: never claim SignedIn
        // until the official SDK has an authenticated user.
        return session()
    }

    override suspend fun signInWithGoogle(): AnyTvAccountSession {
        // Opens the official OAuth flow; completion happens asynchronously
        // through handleDeeplinks(intent) in the Android Activity.
        client.auth.signInWith(Google)
        return session()
    }

    override suspend fun resetPassword(email: String) {
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}
