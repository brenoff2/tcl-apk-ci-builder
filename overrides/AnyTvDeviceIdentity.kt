package com.hari.androidtvremote.ui.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/**
 * Installation-scoped signing identity. No private key or shared backend
 * token is persisted in app preferences or included in the APK.
 * Activation requires an independently authorized single-use code.
 */
internal object AnyTvDeviceIdentity {
    private const val ALIAS = "anytv-resolver-p256-v1"
    private const val PREFS = "anytv_resolver_device"
    internal const val RESOLVER_BASE = "https://anytv-link-resolver-pilot.brenoff.workers.dev"
    private val random = SecureRandom()

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun pair() = keyStore().getCertificate(ALIAS)?.publicKey ?: run {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        generator.generateKeyPair().public
    }

    private fun base64url(input: ByteArray) =
        Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /** WebCrypto P-256 raw/uncompressed public point, not X.509 SPKI. */
    fun publicKey(): String {
        val ec = pair() as java.security.interfaces.ECPublicKey
        val x = fixed32(ec.w.affineX.toByteArray())
        val y = fixed32(ec.w.affineY.toByteArray())
        return "p256:" + base64url(byteArrayOf(4) + x + y)
    }

    private fun fixed32(bytes: ByteArray): ByteArray {
        require(bytes.size <= 33)
        val compact = if (bytes.size == 33 && bytes[0] == 0.toByte())
            bytes.copyOfRange(1, 33) else bytes
        require(compact.size <= 32)
        return ByteArray(32 - compact.size) + compact
    }

    fun deviceId(context: Context): String {
        pair()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("id", null) ?: synchronized(this) {
            prefs.getString("id", null) ?: UUID.randomUUID().toString().also {
                check(prefs.edit().putString("id", it).commit())
            }
        }
    }

    /** Only call following an explicit, trusted operator activation flow. */
    fun activate(context: Context, activationCode: String): Boolean {
        require(activationCode.matches(Regex("[A-Za-z0-9_-]{43}")))
        val id = deviceId(context)
        val key = publicKey()
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyStore().getKey(ALIAS, null) as java.security.PrivateKey)
        val payload = listOf("ANYTV-ACTIVATE1", id, key, activationCode).joinToString("\n")
        signature.update(payload.toByteArray(StandardCharsets.UTF_8))
        val proof = base64url(derToRaw(signature.sign()))
        val json = JSONObject()
            .put("device_id", id)
            .put("public_key", key)
            .put("activation_code", activationCode)
            .put("proof", proof)
        val connection = URL("$RESOLVER_BASE/v1/device/activate").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use {
                it.write(json.toString().toByteArray(StandardCharsets.UTF_8))
            }
            connection.responseCode == 201
        } finally {
            connection.disconnect()
        }
    }

    /** Signature raw format required by WebCrypto (r||s). */
    fun attachProof(context: Context, connection: HttpURLConnection, pathAndQuery: String) {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = ByteArray(24).also(random::nextBytes)
        val nonceEncoded = nonce.joinToString("") { "%02x".format(it) }
        val message = listOf("ANYTV1", "GET", pathAndQuery, timestamp, nonceEncoded).joinToString("\n")
        deviceId(context) // Ensure the Keystore alias exists before signing.
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyStore().getKey(ALIAS, null) as java.security.PrivateKey)
        signature.update(message.toByteArray(StandardCharsets.UTF_8))
        val der = signature.sign()
        connection.setRequestProperty("X-AnyTV-Device", deviceId(context))
        connection.setRequestProperty("X-AnyTV-Time", timestamp)
        connection.setRequestProperty("X-AnyTV-Nonce", nonceEncoded)
        connection.setRequestProperty("X-AnyTV-Signature", base64url(derToRaw(der)))
    }

    private fun derToRaw(der: ByteArray): ByteArray {
        // JCA SHA256withECDSA emits ASN.1 DER; Worker WebCrypto verifies raw r||s.
        var i = 0
        fun length(): Int {
            val first = der[i++].toInt() and 255
            if (first < 128) return first
            val count = first and 127
            require(count in 1..2 && i + count <= der.size)
            var value = 0
            repeat(count) { value = value * 256 + (der[i++].toInt() and 255) }
            return value
        }
        require((der[i++].toInt() and 255) == 0x30)
        val sequenceLength = length()
        require(i + sequenceLength == der.size)
        fun integer(): ByteArray {
            require((der[i++].toInt() and 255) == 0x02)
            val size = length()
            require(size in 1..33 && i + size <= der.size)
            val out = der.copyOfRange(i, i + size)
            i += size
            return fixed32(out)
        }
        val r = integer()
        val s = integer()
        require(i == der.size)
        return r + s
    }
}
