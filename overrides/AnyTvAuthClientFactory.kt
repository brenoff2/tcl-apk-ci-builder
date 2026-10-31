package com.hari.androidtvremote.ui.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth

/**
 * Public configuration only, supplied by the AnyTV release process after
 * an isolated self-hosted tenant and verified TLS route exist.
 * NEVER put a service-role key or OAuth client secret in an Android APK.
 */
internal data class AnyTvPublicAuthConfig(
    val apiUrl: String,
    val publishableKey: String
) {
    fun validate() {
        val parsed = java.net.URI(apiUrl)
        require(parsed.scheme == "https" && !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null && parsed.query == null &&
            parsed.fragment == null) { "AnyTV Auth requires its own HTTPS endpoint" }
        require(publishableKey.isNotBlank() && publishableKey.length >= 20) {
            "Missing AnyTV public API key"
        }
    }
}

/** Official upstream supabase-kt Auth initialization; no custom password flow. */
internal fun createAnyTvAuthClient(config: AnyTvPublicAuthConfig): SupabaseClient {
    config.validate()
    return createSupabaseClient(
        supabaseUrl = config.apiUrl.trimEnd('/'),
        supabaseKey = config.publishableKey
    ) {
        install(Auth)
    }
}
