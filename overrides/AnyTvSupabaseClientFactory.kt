package com.hari.androidtvremote.ui.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Public AnyTV endpoint configuration only. Never use the CLFCJ auth URL,
 * its anon key, or a service_role key. No secrets are checked into the APK.
 */
internal data class AnyTvPublicAuthConfig(
    val endpoint: String,
    val publishableKey: String
) {
    init {
        require(endpoint.startsWith("https://")) { "AnyTV auth requires HTTPS" }
        require(endpoint == endpoint.trimEnd('/')) { "Remove trailing slash" }
        require(publishableKey.isNotBlank()) { "Missing public AnyTV key" }
        require(!publishableKey.contains("\n")) { "Invalid key" }
    }
}

/** Official SDK plugins, not a custom password/session implementation. */
internal object AnyTvSupabaseClientFactory {
    fun create(config: AnyTvPublicAuthConfig): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = config.endpoint,
            supabaseKey = config.publishableKey
        ) {
            install(Auth)
            install(Postgrest)
        }
}
