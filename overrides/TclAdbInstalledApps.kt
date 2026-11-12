package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Opt-in Android TV 9 ADB/TCP inventory. This does not enable debugging, bypass
 * RSA authorization, change TV settings, or install a TV-side component.
 * ADB TCP/5555 is unencrypted; use only on a trusted local network.
 */
internal class TclAdbInstalledApps(private val context: Context) {
    companion object {
        private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private const val maxShellBytes = 2 * 1024 * 1024
        private const val maxApkBytes = 48 * 1024 * 1024
        private const val maxApksPerApp = 12

        internal fun parsePackages(raw: String): List<String> =
            raw.lineSequence().map { it.trim() }
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:") }.filter { packagePattern.matches(it) }
                .distinct().sorted().toList()

        internal fun parseApkPaths(raw: String): List<String> =
            raw.lineSequence().map { it.trim() }
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:") }
                .filter { it.startsWith("/") && it.endsWith(".apk") &&
                    !it.contains("..") && !it.contains("'") && !it.contains("\n") }
                .take(maxApksPerApp).toList()

        internal fun parseLaunchablePackages(raw: String): Set<String> =
            Regex("([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+)/[A-Za-z0-9_.$]+")
                .findAll(raw).map { it.groupValues[1] }
                .filter { packagePattern.matches(it) }.toSet()
    }

    private fun keyPair(): AdbCrypto {
        // Android app-private storage; never export the ADB identity or store in preferences.
        val dir = File(context.filesDir, "tcl_adb_keys").apply { mkdirs() }
        val priv = File(dir, "key.pk8")
        val pub = File(dir, "key.pub")
        val base64 = AdbBase64 { bytes -> Base64.encodeToString(bytes, Base64.NO_WRAP) }
        if (priv.isFile && pub.isFile) return AdbCrypto.loadAdbKeyPair(base64, priv, pub)
        val crypto = AdbCrypto.generateAdbKeyPair(base64)
        crypto.saveAdbKeyPair(priv, pub)
        return crypto
    }

    private fun <T> withAdb(
        host: String,
        timeoutMs: Long = 30000L,
        callback: (AdbConnection) -> T
    ): T {
        val octets = host.split('.').map { it.toIntOrNull() }
        require(octets.size == 4 && octets.all { it != null && it in 0..255 }) {
            "Endereço IPv4 inválido"
        }
        val a = octets[0]!!
        val b = octets[1]!!
        require(a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)) {
            "ADB permitido apenas na rede local privada"
        }
        val socket = Socket()
        val watchdog = java.util.Timer("anytv-adb-deadline", true)
        val expired = java.util.concurrent.atomic.AtomicBoolean(false)
        watchdog.schedule(object : java.util.TimerTask() {
            override fun run() {
                expired.set(true)
                runCatching { socket.close() }
            }
        }, timeoutMs)
        try {
            socket.connect(InetSocketAddress(host, 5555), 4000)
            socket.soTimeout = 15000
            AdbConnection.create(socket, keyPair()).use { adb ->
                if (!adb.connect(22, TimeUnit.SECONDS, false)) {
                    throw IllegalStateException("Autorize o AnyTV na mensagem de depuração da TCL.")
                }
                return callback(adb)
            }
        } catch (error: Exception) {
            if (expired.get()) throw java.io.IOException(
                "Tempo limite na comunicação ADB. Verifique a autorização e a TV.", error)
            throw error
        } finally {
            watchdog.cancel()
            runCatching { socket.close() }
        }
    }

    private fun shell(adb: AdbConnection, command: String): String {
        val output = ByteArrayOutputStream()
        adb.open("shell:" + command).use { stream ->
            while (true) {
                val part = try { stream.read() } catch (end: java.io.IOException) {
                    if (stream.isClosed) break else throw end
                }
                if (output.size() + part.size > maxShellBytes) error("Resposta ADB excessiva")
                output.write(part)
            }
        }
        return output.toString("UTF-8")
    }

    /** ADB sync protocol RECV. Read only. Temporary APKs remain app-private and are deleted. */
    private fun copyApk(adb: AdbConnection, remotePath: String, file: File) {
        require(remotePath.startsWith("/") && remotePath.endsWith(".apk") &&
            !remotePath.contains("..") && !remotePath.contains("'"))
        val pathBytes = remotePath.toByteArray(Charsets.UTF_8)
        require(pathBytes.size in 1..1024)
        adb.open("sync:").use { stream ->
            stream.write(ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
                .put("RECV".toByteArray(Charsets.US_ASCII)).putInt(pathBytes.size)
                .put(pathBytes).array())
            val output = ByteArrayOutputStream()
            var pending = ByteArray(0)
            while (true) {
                while (pending.size < 8) pending += stream.read()
                val command = pending.copyOfRange(0, 4).toString(Charsets.US_ASCII)
                val length = ByteBuffer.wrap(pending, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                pending = pending.copyOfRange(8, pending.size)
                if (command == "DONE") {
                    require(output.size() > 0) { "APK vazio" }
                    file.writeBytes(output.toByteArray())
                    return
                }
                require(command == "DATA" || command == "FAIL") { "Erro ADB sync" }
                require(length in 0..maxApkBytes && output.size() + length <= maxApkBytes) {
                    "APK excede limite de leitura"
                }
                while (pending.size < length) pending += stream.read()
                val data = pending.copyOfRange(0, length)
                pending = pending.copyOfRange(length, pending.size)
                if (command == "FAIL") error("TCL recusou leitura do APK")
                output.write(data)
            }
        }
    }

    private fun readRealApp(adb: AdbConnection, host: String, pkg: String): TvOfficialApp {
        val paths = parseApkPaths(shell(adb, "pm path " + pkg))
        if (paths.isEmpty()) return TvOfficialApp(pkg, pkg, "")
        val tmp = File(context.cacheDir, "anytv_adb_apks").apply { mkdirs() }
        val prefix = MessageDigest.getInstance("SHA-256")
            .digest((host + "/" + pkg).toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        val files = mutableListOf<File>()
        try {
            for ((index, path) in paths.withIndex()) {
                val local = File(tmp, prefix + "_" + index + ".apk")
                files.add(local)
                copyApk(adb, path, local)
            }
            val archive = context.packageManager.getPackageArchiveInfo(
                files[0].absolutePath, 0) ?: return TvOfficialApp(pkg, pkg, "")
            val info = archive.applicationInfo ?: return TvOfficialApp(pkg, pkg, "")
            info.sourceDir = files[0].absolutePath
            info.publicSourceDir = files[0].absolutePath
            if (files.size > 1) {
                info.splitSourceDirs = files.drop(1).map { it.absolutePath }.toTypedArray()
                info.splitPublicSourceDirs = info.splitSourceDirs
            }
            // Label/icon must resolve from the APK's own manifest/resources, not a catalog.
            val label = runCatching {
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: pkg
            val icon = runCatching {
                // Never serialize PackageManager's fallback Android icon as a real TV icon.
                require(info.icon != 0) { "Aplicativo sem ícone de manifesto" }
                val resources = context.packageManager.getResourcesForApplication(info)
                val drawable = resources.getDrawable(info.icon, null)
                val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                try {
                    drawable.setBounds(0, 0, 96, 96)
                    drawable.draw(Canvas(bmp))
                    val output = ByteArrayOutputStream()
                    check(bmp.compress(Bitmap.CompressFormat.PNG, 100, output))
                    Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                } finally {
                    bmp.recycle()
                }
            }.getOrDefault("")
            return TvOfficialApp(pkg, label, icon)
        } finally {
            files.forEach { runCatching { it.delete() } }
        }
    }

    /** Read-only heartbeat. An authorized TV may briefly drop its TCP socket. */
    suspend fun probe(host: String): Boolean = withContext(Dispatchers.IO) {
        withAdb(host, 28000L) { true }
    }

    suspend fun ping(host: String): Boolean = withContext(Dispatchers.IO) {
        withAdb(host, 18000L) { adb ->
            check(shell(adb, "echo ANYTV_ADB_CONNECTED")
                .contains("ANYTV_ADB_CONNECTED")) { "A conexão ADB foi interrompida" }
            true
        }
    }

    suspend fun inventory(host: String): List<TvOfficialApp> = withContext(Dispatchers.IO) {
        // Bounded: a stalled APK transfer must never leave the UI in perpetual loading.
        withAdb(host, 90000L) { adb ->
            val installed = parsePackages(shell(adb, "pm list packages")).toSet()
            // Deliberately fail closed when firmware cannot enumerate launcher activities.
            val launchable = parseLaunchablePackages(shell(adb,
                "cmd package query-activities --brief -a android.intent.action.MAIN " +
                    "-c android.intent.category.LEANBACK_LAUNCHER"))
            require(launchable.isNotEmpty()) {
                "A TCL não retornou os aplicativos do launcher. Inventário indisponível."
            }
            launchable.filter { it in installed }.sorted().map { pkg ->
                runCatching { readRealApp(adb, host, pkg) }
                    .getOrDefault(TvOfficialApp(pkg, pkg, ""))
            }.sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Best-effort, provider-scoped HTTPS deep link; requests an Android intent,
     * not playback confirmation. A rejected link must not be advertised as opened.
     * No URL from a catalog is interpolated into a shell without validation.
     */
    suspend fun openTitleLink(host: String, packageName: String, url: String): Boolean =
        withContext(Dispatchers.IO) {
            require(packagePattern.matches(packageName)) { "Pacote inválido" }
            val uri = java.net.URI(url)
            val domain = uri.host?.lowercase() ?: ""
            val allowed = when (packageName) {
                "com.netflix.ninja" -> setOf("www.netflix.com", "netflix.com")
                "com.disney.disneyplus" -> setOf("www.disneyplus.com", "disneyplus.com")
                "com.amazon.amazonvideo.livingroom" ->
                    setOf("www.primevideo.com", "primevideo.com")
                "com.wbd.stream" -> setOf("www.max.com", "max.com", "play.max.com")
                "com.apple.atve.androidtv.appletv" -> setOf("tv.apple.com")
                else -> emptySet()
            }
            require(uri.scheme.equals("https", ignoreCase = true) &&
                uri.userInfo == null && uri.port == -1 && domain in allowed &&
                url.length in 10..2048 && url.none { it == '\'' || it.isISOControl() }) {
                "Link não autorizado para este aplicativo"
            }
            withAdb(host) { adb ->
                require(packageName in parsePackages(shell(adb, "pm list packages"))) {
                    "Aplicativo não está instalado na TCL."
                }
                val response = shell(adb,
                    "am start -a android.intent.action.VIEW -d '$url' -p $packageName")
                response.contains("Starting: Intent") &&
                    !response.contains("Error:", ignoreCase = true) &&
                    !response.contains("unable", ignoreCase = true)
            }
        }

    suspend fun launch(host: String, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            require(packagePattern.matches(packageName)) { "Pacote inválido" }
            withAdb(host) { adb ->
                require(packageName in parsePackages(shell(adb, "pm list packages"))) {
                    "Aplicativo não instalado"
                }
                shell(adb, "monkey -p " + packageName +
                    " -c android.intent.category.LEANBACK_LAUNCHER 1")
                    .contains("Events injected: 1")
            }
        }
}
