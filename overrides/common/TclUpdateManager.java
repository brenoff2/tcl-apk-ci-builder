package tcl.shared;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-app updates for the original phone package and the Android TV companion.
 *
 * Sources ONLY the public, fixed repository's GitHub Releases. Downloads a
 * release asset, checks its published SHA-256, package name, versionCode and
 * installed signing certificate, then opens Android PackageInstaller.
 *
 * Android may require approval / "install unknown apps"; never sidestep it.
 * The companion has two legacy 1.3 signers. Select the 1.4 APK whose
 * signature matches THIS installation, preserving TV pairing preferences.
 */
public final class TclUpdateManager {
    private static final String TAG = "TclUpdater";
    private static final String REPO = "brenoff2/tcl-apk-ci-builder";
    private static final String API = "https://api.github.com/repos/" + REPO;
    private static final String DOWNLOAD_PREFIX =
            "https://github.com/" + REPO + "/releases/download/";
    private static final String PHONE_PACKAGE = "com.tclremote.livre";
    private static final String TV_PACKAGE = "com.tclremote.tvcompanion";
    private static final String PHONE_TAG = "tcl-phone-v2.2";
    private static final String TV_TAG = "tv-complemento-v1.4";
    private static final int MAX_DOWNLOAD = 85 * 1024 * 1024;
    private static final String TV_CERT_A =
            "3382dfb12a0f16eb9167e056f947ea9740448fe07b8ef9d0ace0c889bf591add";
    private static final String TV_CERT_B =
            "8b2b0c6f2a7a668f9aae829be811241dc70c3697af2b3e6c71e1a1767a5d83da";

    public interface Status {
        void show(String value);
    }

    private TclUpdateManager() { }

    public static void check(Activity activity, Status callback) {
        final Context app = activity.getApplicationContext();
        final String pkg = app.getPackageName();
        if (!PHONE_PACKAGE.equals(pkg) && !TV_PACKAGE.equals(pkg)) {
            callback.show("Este aplicativo não pertence ao canal de atualizações TCL.");
            return;
        }
        callback.show("Buscando atualização oficial no GitHub...");
        new Thread(() -> {
            try {
                PackageManager pm = app.getPackageManager();
                int flags = Build.VERSION.SDK_INT >= 28 ?
                        PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
                PackageInfo installed = pm.getPackageInfo(pkg, flags);
                String signer = sha256(currentSigner(installed));
                String tag = TV_PACKAGE.equals(pkg) ? TV_TAG : PHONE_TAG;
                String base = DOWNLOAD_PREFIX + tag + "/";
                JSONObject release = new JSONObject(readUtf8(
                        API + "/releases/tags/" + tag, 800_000));
                if (!tag.equals(release.optString("tag_name"))) {
                    throw new IllegalStateException("A versão publicada não corresponde ao canal TCL.");
                }
                if (sameOrOlder(release.optString("tag_name"), installed.versionName)) {
                    post(activity, callback, "Você já tem a versão mais recente.");
                    return;
                }
                JSONArray assets = release.getJSONArray("assets");
                String expectedName;
                if (TV_PACKAGE.equals(pkg)) {
                    if (TV_CERT_A.equals(signer)) {
                        expectedName = "Controle_TCL_TV_v1.4_cert3382.apk";
                    } else if (TV_CERT_B.equals(signer)) {
                        expectedName = "Controle_TCL_TV_v1.4_cert8b2b.apk";
                    } else {
                        throw new IllegalStateException(
                                "Sua TV usa uma assinatura não reconhecida. " +
                                "Não vou apagar a configuração nem forçar uma reinstalação.");
                    }
                } else {
                    expectedName = "Controle_TCL_Livre_v2.2.apk";
                }

                JSONObject asset = null;
                for (int n = 0; n < assets.length(); n++) {
                    JSONObject candidate = assets.getJSONObject(n);
                    if (expectedName.equals(candidate.optString("name"))) {
                        asset = candidate;
                        break;
                    }
                }
                if (asset == null) {
                    throw new IllegalStateException(
                            "A atualização ainda não tem um APK compatível com este aparelho.");
                }
                String url = asset.getString("browser_download_url");
                String digest = asset.optString("digest", "").toLowerCase(Locale.ROOT);
                long size = asset.optLong("size", -1);
                if (!url.equals(base + expectedName) || !digest.matches("sha256:[0-9a-f]{64}")
                        || size < 15000 || size > MAX_DOWNLOAD) {
                    throw new SecurityException("Metadados do APK inválidos; atualização bloqueada.");
                }

                if (Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
                    post(activity, callback,
                            "Permita 'Instalar apps desconhecidos' para este aplicativo. " +
                            "Depois volte e toque novamente em Buscar atualizações.");
                    activity.runOnUiThread(() -> {
                        try {
                            Intent permission = new Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + pkg));
                            activity.startActivity(permission);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Unknown-app settings unavailable", e);
                        }
                    });
                    return;
                }

                post(activity, callback, "Atualização encontrada. Baixando e verificando...");
                File apk = new File(app.getCacheDir(), "tcl-update-verified.apk");
                download(url, apk, size);
                if (!digest.substring(7).equals(sha256File(apk))) {
                    apk.delete();
                    throw new SecurityException("Arquivo baixado não confere com o SHA-256 publicado.");
                }
                PackageInfo next = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
                if (next == null || !pkg.equals(next.packageName)) {
                    apk.delete();
                    throw new SecurityException("O APK não corresponde ao aplicativo instalado.");
                }
                long installedCode = Build.VERSION.SDK_INT >= 28 ?
                        installed.getLongVersionCode() : installed.versionCode;
                long nextCode = Build.VERSION.SDK_INT >= 28 ?
                        next.getLongVersionCode() : next.versionCode;
                if (nextCode <= installedCode) {
                    apk.delete();
                    post(activity, callback, "O aplicativo já está atualizado.");
                    return;
                }
                if (!MessageDigest.isEqual(currentSigner(installed), currentSigner(next))) {
                    apk.delete();
                    throw new SecurityException(
                            "Assinatura diferente da instalada. Atualização bloqueada sem desinstalar.");
                }
                post(activity, callback,
                        "Arquivo verificado. Abrindo a confirmação de instalação do Android...");
                install(app, apk, nextCode);
            } catch (Exception error) {
                Log.e(TAG, "Update failed safely", error);
                post(activity, callback, "Não foi possível atualizar: " +
                        (error.getMessage() == null ? "erro de rede ou instalador" : error.getMessage()));
            }
        }, "tcl-update-check").start();
    }

    private static void post(Activity activity, Status callback, String message) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing()) callback.show(message);
        });
    }

    private static boolean sameOrOlder(String remoteTag, String localVersion) {
        Matcher a = Pattern.compile("v(\\d+)\\.(\\d+)").matcher(remoteTag);
        Matcher b = Pattern.compile("(\\d+)\\.(\\d+)").matcher(localVersion == null ? "" : localVersion);
        if (!a.find() || !b.find()) return false;
        int remote = Integer.parseInt(a.group(1)) * 1000 + Integer.parseInt(a.group(2));
        int current = Integer.parseInt(b.group(1)) * 1000 + Integer.parseInt(b.group(2));
        return remote <= current;
    }

    private static byte[] currentSigner(PackageInfo info) {
        Signature[] signatures = Build.VERSION.SDK_INT >= 28
                ? info.signingInfo.getApkContentsSigners() : info.signatures;
        if (signatures == null || signatures.length != 1) {
            throw new SecurityException("Certificado ausente ou assinatura múltipla incompatível.");
        }
        return signatures[0].toByteArray();
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] raw = digest.digest(bytes);
        StringBuilder b = new StringBuilder(raw.length * 2);
        for (byte value : raw) b.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return b.toString();
    }

    private static String sha256File(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder b = new StringBuilder();
        for (byte value : digest.digest())
            b.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return b.toString();
    }

    private static HttpURLConnection connection(String uri) throws Exception {
        URL parsed = new URL(uri);
        if (!"https".equals(parsed.getProtocol()))
            throw new SecurityException("Atualizações exigem HTTPS.");
        HttpURLConnection c = (HttpURLConnection) parsed.openConnection();
        c.setRequestProperty("User-Agent", "Controle-TCL-Livre-Updater");
        c.setRequestProperty("Accept", "application/vnd.github+json,application/octet-stream,*/*");
        c.setConnectTimeout(12000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String readUtf8(String url, int max) throws Exception {
        HttpURLConnection c = connection(url);
        try {
            if (c.getResponseCode() != 200) {
                throw new IllegalStateException("Versão ainda não publicada (" +
                        c.getResponseCode() + ").");
            }
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int total = 0, n;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > max) throw new SecurityException("Resposta excessiva do servidor.");
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }

    private static void download(String uri, File destination, long expectedSize) throws Exception {
        HttpURLConnection c = connection(uri);
        File temp = new File(destination.getParentFile(), destination.getName() + ".partial");
        try {
            if (c.getResponseCode() != 200) {
                throw new IllegalStateException("Download indisponível (" +
                        c.getResponseCode() + ").");
            }
            long received = 0;
            try (InputStream in = c.getInputStream();
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) != -1) {
                    received += n;
                    if (received > MAX_DOWNLOAD) throw new SecurityException("APK excedeu o limite.");
                    out.write(buf, 0, n);
                }
            }
            if (received != expectedSize || !temp.renameTo(destination)) {
                throw new IllegalStateException("Download incompleto ou armazenamento insuficiente.");
            }
        } finally {
            c.disconnect();
            temp.delete();
        }
    }

    private static void install(Context app, File apk, long nextVersion) throws Exception {
        PackageInstaller installer = app.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(app.getPackageName());
        params.setSize(apk.length());
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(
                    PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
        }
        int sessionId = installer.createSession(params);
        boolean committed = false;
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("tcl-update", 0, apk.length())) {
                byte[] buffer = new byte[32768];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                session.fsync(out);
            }
            Intent result = new Intent(app, TclInstallResultReceiver.class);
            result.setAction("tcl.shared.UPDATE_INSTALL_RESULT");
            result.putExtra("version", nextVersion);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent callback = PendingIntent.getBroadcast(app, sessionId, result, flags);
            session.commit(callback.getIntentSender());
            committed = true;
        } finally {
            if (!committed) installer.abandonSession(sessionId);
        }
    }
}
