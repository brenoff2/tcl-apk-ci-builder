package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.bean.Icon
import net.dongliu.apk.parser.bean.AdaptiveIcon
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

/** A partial ADB read never replaces a previously verified label or real PNG. */
internal fun mergeTclInventoryMetadata(
    previous: List<TvOfficialApp>, fresh: List<TvOfficialApp>
): List<TvOfficialApp> {
    val old = previous.associateBy { it.packageName }
    return fresh.map { item ->
        val cached = old[item.packageName]
        val name = item.name.takeIf { it.isNotBlank() && it != item.packageName }
            ?: cached?.name?.takeIf { it.isNotBlank() && it != item.packageName }
            ?: item.packageName
        val icon = item.icon.ifBlank { cached?.icon.orEmpty() }
        item.copy(name = name, icon = icon)
    }
}

internal class TclAdbInstalledApps(private val context: Context) {
    @Volatile
    var lastInventoryWarnings: List<String> = emptyList()
        private set

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

    private fun readRealApp(
        adb: AdbConnection, host: String, pkg: String,
        warnings: MutableList<String>
    ): TvOfficialApp {
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
            // The phone may refuse to parse the TV's archive as an installed APK.
            // Archive metadata is independent of the phone's PackageManager.
            val archive = runCatching {
                context.packageManager.getPackageArchiveInfo(files[0].absolutePath, 0)
            }.getOrNull()
            val info = archive?.applicationInfo?.apply {
                sourceDir = files[0].absolutePath
                publicSourceDir = files[0].absolutePath
                if (files.size > 1) {
                    splitSourceDirs = files.drop(1).map { it.absolutePath }.toTypedArray()
                    splitPublicSourceDirs = splitSourceDirs
                }
            }
            // Prefer real manifest and resources in the APK, never an app catalog.
            // Do not request the remote package through the phone's installed-PM
            // namespace. Its package is installed only on the TV.
            val parsed = runCatching {
                ApkFile(files[0]).use { apk ->
                    apk.setPreferredLocale(java.util.Locale.forLanguageTag("pt-BR"))
                    val meta = apk.apkMeta
                    val name = meta.label?.trim()?.takeIf {
                        it.isNotEmpty() && !it.startsWith("@") && it != pkg
                    }
                    // The manifest names the icon; its bitmap may live in a
                    // density split. Resolve only the referenced filename, not
                    // a guessed icon or a fixed package-to-artwork catalog.
                    val iconBytes = apk.allIcons.flatMap { face ->
                        when (face) {
                            is Icon -> listOf(face)
                            // Android TV 9 supports adaptive launcher icons. The
                            // parser returns these as AdaptiveIcon, NOT Icon.
                            is AdaptiveIcon -> listOfNotNull(face.foreground)
                            else -> emptyList()
                        }
                    }.sortedByDescending { it.density }
                        .firstNotNullOfOrNull { item ->
                            val filename = item.path.substringAfterLast('/')
                            val bytes = item.data ?: files.drop(1).asSequence()
                                .mapNotNull { split ->
                                    if (!filename.matches(Regex(
                                            "[A-Za-z0-9_]+[.](png|webp|jpg|jpeg)"))) {
                                        null
                                    } else java.util.zip.ZipFile(split).use { zip ->
                                        val entry = zip.entries().asSequence()
                                            .firstOrNull { it.name.startsWith("res/") &&
                                                it.name.endsWith("/" + filename) &&
                                                it.size in 100..(2 * 1024 * 1024) }
                                        entry?.let { zip.getInputStream(it).use { stream ->
                                            stream.readBytes()
                                        } }
                                    }
                                }.firstOrNull()
                            bytes?.takeIf { data ->
                                if (data.size !in 100..(2 * 1024 * 1024)) false else {
                                    val bounds = BitmapFactory.Options().apply {
                                        inJustDecodeBounds = true
                                    }
                                    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                                    bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096
                                }
                            }
                        }
                    Pair(name, iconBytes)
                }
            }.onFailure { error ->
                warnings.add(pkg + ": falha do parser APK (" +
                    error.javaClass.simpleName + ")")
            }.getOrNull()
            val label = parsed?.first ?: runCatching {
                val name = context.packageManager.getApplicationLabel(requireNotNull(info)).toString()
                name.takeIf { it.isNotBlank() && it != pkg }
            }.getOrNull() ?: pkg
            val icon = parsed?.second?.let { data ->
                val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
                bmp?.let { original ->
                    try {
                        val scaled = Bitmap.createScaledBitmap(original, 96, 96, true)
                        try {
                            val output = ByteArrayOutputStream()
                            check(scaled.compress(Bitmap.CompressFormat.PNG, 100, output))
                            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                        } finally { if (scaled !== original) scaled.recycle() }
                    } finally { original.recycle() }
                }
            } ?: runCatching {
                // Legacy archive resource fallback, never an invented icon.
                val appInfo = requireNotNull(info) { "Pacote ausente do PM local" }
                require(appInfo.icon != 0) { "Aplicativo sem ícone de manifesto" }
                val resources = context.packageManager.getResourcesForApplication(appInfo)
                val drawable = resources.getDrawable(appInfo.icon, null)
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


    /** Temporary remote DEX: no TV APK installation, no RSA changes, no persistent helper. */
    private fun nativeMetadata(adb: AdbConnection): Map<String, TvOfficialApp> {
        val dex = context.assets.open("anytv_remote_meta.dex").use { it.readBytes() }
        require(dex.size in 256..(192 * 1024)) { "Metadados remotos ausentes" }
        val remote = "/data/local/tmp/anytv_meta_" +
            java.util.UUID.randomUUID().toString().replace("-", "") + ".dex"
        try {
            val target = (remote + ",0644").toByteArray(Charsets.UTF_8)
            adb.open("sync:").use { stream ->
                stream.write(ByteBuffer.allocate(8 + target.size).order(ByteOrder.LITTLE_ENDIAN)
                    .put("SEND".toByteArray(Charsets.US_ASCII))
                    .putInt(target.size).put(target).array())
                // tananaev/adblib 1.3 advertises CONNECT_MAXDATA=4096.
                // A sync DATA record (8-byte header included) must fit one WRTE.
                var offset = 0
                while (offset < dex.size) {
                    val length = minOf(2048, dex.size - offset)
                    stream.write(ByteBuffer.allocate(8 + length)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .put("DATA".toByteArray(Charsets.US_ASCII))
                        .putInt(length).put(dex, offset, length).array())
                    offset += length
                }
                stream.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    .put("DONE".toByteArray(Charsets.US_ASCII)).putInt(0).array())
                var response = ByteArray(0)
                while (response.size < 8) response += stream.read()
                val code = response.copyOfRange(0, 4).toString(Charsets.US_ASCII)
                if (code == "FAIL") {
                    val size = ByteBuffer.wrap(response, 4, 4)
                        .order(ByteOrder.LITTLE_ENDIAN).int
                    if (size in 1..2048) {
                        while (response.size < 8 + size) response += stream.read()
                        val reason = response.copyOfRange(8, 8 + size)
                            .toString(Charsets.UTF_8).take(180)
                        error("A TCL recusou o DEX: " + reason)
                    }
                }
                require(code == "OKAY") { "TCL recusou upload DEX: " + code }
            }
            val answer = shell(adb,
                "CLASSPATH=" + remote + " app_process / anytv.adb.AnyTvRemoteMeta")
            require(answer.contains("ANYTV_REMOTE_META_V1") &&
                answer.contains("ANYTV_REMOTE_META_END")) { "Runtime da TCL não aceitou metadados nativos" }
            val found = linkedMapOf<String, TvOfficialApp>()
            for (line in answer.lineSequence()) {
                if (!line.startsWith("ANYTV_APP\t")) continue
                val fields = line.trimEnd('\r', '\n').split('\t', limit = 4)
                if (fields.size != 4 || !packagePattern.matches(fields[1])) continue
                val name = runCatching {
                    Base64.decode(fields[2], Base64.DEFAULT).toString(Charsets.UTF_8)
                        .trim().take(100)
                }.getOrDefault(fields[1])
                val icon = fields[3].takeIf { it.length <= 45000 &&
                    runCatching { Base64.decode(it, Base64.DEFAULT).size in 100..32768 }
                        .getOrDefault(false) }.orEmpty()
                found[fields[1]] = TvOfficialApp(fields[1], name, icon)
            }
            require(found.isNotEmpty()) { "TCL retornou lista nativa vazia" }
            return found
        } finally {
            // ADB shell runs as authorized user; remove only our unique temporary DEX.
            runCatching { shell(adb, "rm -f " + remote) }
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
            val leanback = parseLaunchablePackages(shell(adb,
                "cmd package query-activities --brief -a android.intent.action.MAIN " +
                    "-c android.intent.category.LEANBACK_LAUNCHER"))
            val standard = parseLaunchablePackages(shell(adb,
                "cmd package query-activities --brief -a android.intent.action.MAIN " +
                    "-c android.intent.category.LAUNCHER"))
            val launchable = leanback + standard
            require(launchable.isNotEmpty()) {
                "A TCL não retornou os aplicativos do launcher. Inventário indisponível."
            }
            val warnings = mutableListOf<String>()
            val native = runCatching { nativeMetadata(adb) }
                .onFailure { warnings.add("Extração nativa: " +
                    it.javaClass.simpleName + ": " + it.message.orEmpty().take(90)) }
                .getOrDefault(emptyMap())
            val apps = launchable.filter { it in installed }.sorted().map { pkg ->
                val fromTv = native[pkg]
                if (fromTv?.name != pkg && !fromTv?.icon.isNullOrBlank()) {
                    fromTv!!
                } else {
                    val archive = runCatching { readRealApp(adb, host, pkg, warnings) }
                        .getOrElse { error ->
                            warnings.add(pkg + ": " + error.javaClass.simpleName +
                                ": " + error.message.orEmpty().take(90))
                            TvOfficialApp(pkg, pkg, "")
                        }
                    mergeTclInventoryMetadata(listOfNotNull(fromTv), listOf(archive))[0]
                }.also { app ->
                    if (app.name == pkg || app.icon.isBlank()) {
                        warnings.add(pkg + ": nome ou ícone incompleto")
                    }
                }
            }.sortedBy { it.name.lowercase() }
            if (apps.none { it.name != it.packageName || it.icon.isNotBlank() }) {
                warnings.add("Nenhum nome ou ícone foi resolvido; verifique os recursos dos APKs.")
            }
            lastInventoryWarnings = warnings.take(6)
            apps
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
                "com.netflix.ninja", "com.netflix.mediaclient" ->
                    setOf("www.netflix.com", "netflix.com")
                "com.disney.disneyplus" -> setOf("www.disneyplus.com", "disneyplus.com")
                "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient" ->
                    setOf("www.primevideo.com", "primevideo.com", "app.primevideo.com")
                "com.wbd.stream" ->
                    setOf("www.max.com", "max.com", "play.max.com", "play.hbomax.com")
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
                // Prefer the TV's own launchable activity, including system apps.
                // Fall back only when the activity manager rejects the intent.
                val response = shell(adb,
                    "am start -a android.intent.action.MAIN " +
                        "-c android.intent.category.LEANBACK_LAUNCHER -p " + packageName)
                if (response.contains("Starting: Intent") &&
                    !response.contains("Error:", ignoreCase = true) &&
                    !response.contains("unable", ignoreCase = true)) {
                    true
                } else {
                    val standard = shell(adb,
                        "am start -a android.intent.action.MAIN " +
                            "-c android.intent.category.LAUNCHER -p " + packageName)
                    if (standard.contains("Starting: Intent") &&
                        !standard.contains("Error:", ignoreCase = true) &&
                        !standard.contains("unable", ignoreCase = true)) {
                        true
                    } else {
                        // Android TV launchers may expose only LEANBACK_LAUNCHER.
                        // Monkey defaults to LAUNCHER/MONKEY unless -c is supplied.
                        shell(adb, "monkey -p " + packageName +
                            " -c android.intent.category.LEANBACK_LAUNCHER 1")
                            .contains("Events injected: 1")
                    }
                }
            }
        }
}
