package dk.fez.bluetoothquickconnect;

import android.content.Context;
import android.Manifest;
import android.os.Build;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.app.SearchManager;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Paired local-network bridge. Android TV Remote v2 does NOT expose installed
 * TV apps, so the Android TV-side application lists the actual leanback
 * launchers using PackageManager and makes the official icon/label available.
 *
 * Pairing code is shown only by the TV UI. No analytics, cloud, discovery
 * broadcast or privileged API. Requests are restricted to private LAN peers.
 */
public final class TvAppBridge {
    public static final int PORT = 18765;
    private static final String TAG = "TclAppBridge";
    private static final String PREFS = "tcl_companion_bridge";
    private static final String CODE = "pair_code";
    private static final int MAX_APPS = 160;
    private static final String DISNEY_PACKAGE = "com.disney.disneyplus";
    private static final String ZOOTOPIA_2_LINK =
            "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185";

    private final Context context;
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final AtomicInteger badCodes = new AtomicInteger();
    private volatile boolean running = false;
    private volatile String headphoneStatus = "idle";
    private ServerSocket server;

    public TvAppBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    public static String pairingCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String existing = prefs.getString(CODE, null);
        if (existing != null && existing.matches("[0-9]{8}")) return existing;
        String created = String.format(java.util.Locale.ROOT, "%08d",
                new SecureRandom().nextInt(100000000));
        prefs.edit().putString(CODE, created).apply();
        return created;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        pairingCode(context);
        Thread acceptor = new Thread(() -> {
            try (ServerSocket listener = new ServerSocket(PORT)) {
                server = listener;
                listener.setReuseAddress(true);
                while (running) {
                    Socket peer = listener.accept();
                    workers.execute(() -> handle(peer));
                }
            } catch (Exception e) {
                if (running) Log.e(TAG, "TV LAN service unavailable", e);
            } finally {
                running = false;
            }
        }, "tcl-app-bridge");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        workers.shutdownNow();
    }

    private boolean localPeer(InetAddress address) {
        return address.isSiteLocalAddress() || address.isLoopbackAddress();
    }

    private void handle(Socket peer) {
        try (Socket socket = peer) {
            socket.setSoTimeout(4000);
            if (!localPeer(socket.getInetAddress())) {
                write(socket.getOutputStream(), 403, "{\"error\":\"not_local\"}");
                return;
            }
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            String request = line(in);
            if (request == null || request.length() > 2500) return;
            String[] parts = request.split(" ");
            if (parts.length != 3) return;
            String key = "";
            int size = 0;
            for (int n = 0; n < 50; n++) {
                String header = line(in);
                if (header == null) return;
                if (header.isEmpty()) break;
                int colon = header.indexOf(':');
                if (colon < 0) continue;
                String name = header.substring(0, colon).trim();
                String value = header.substring(colon+1).trim();
                if (name.equalsIgnoreCase("X-TCL-Pair")) key = value;
                if (name.equalsIgnoreCase("Content-Length"))
                    size = Integer.parseInt(value);
            }
            if (badCodes.get() >= 8) {
                Thread.sleep(2500);
                badCodes.set(0);
            }
            byte[] expected = pairingCode(context).getBytes(StandardCharsets.UTF_8);
            boolean authorized = MessageDigest.isEqual(
                    key.getBytes(StandardCharsets.UTF_8), expected);
            if (!authorized) {
                badCodes.incrementAndGet();
                write(socket.getOutputStream(), 401, "{\"error\":\"invalid_pairing_code\"}");
                return;
            }
            badCodes.set(0);
            if (parts[0].equals("GET") && parts[1].equals("/apps")) {
                write(socket.getOutputStream(), 200, installedApps().toString());
            } else if (parts[0].equals("POST") && parts[1].equals("/launch")) {
                if (size < 1 || size > 2048) {
                    write(socket.getOutputStream(), 400, "{\"error\":\"invalid_body\"}");
                    return;
                }
                byte[] bytes = new byte[size];
                int read = 0;
                while (read < size) {
                    int count = in.read(bytes, read, size-read);
                    if (count < 0) throw new java.io.IOException("Incomplete HTTP body");
                    read += count;
                }
                JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                String pkg = obj.optString("package", "");
                boolean opened = launchInstalled(pkg);
                write(socket.getOutputStream(), opened ? 200 : 404,
                        "{\"launched\":" + opened + "}");
            } else if (parts[0].equals("POST") && parts[1].equals("/title-search")) {
                // Opt-in, paired LAN request. Search is delegated ONLY to the
                // selected app if it explicitly exports ACTION_SEARCH.
                if (size < 1 || size > 2048) {
                    write(socket.getOutputStream(), 400, "{\"error\":\"invalid_body\"}");
                    return;
                }
                byte[] bytes = new byte[size];
                int read = 0;
                while (read < size) {
                    int count = in.read(bytes, read, size - read);
                    if (count < 0) throw new java.io.IOException("Incomplete HTTP body");
                    read += count;
                }
                JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                String pkg = obj.optString("package", "");
                String title = obj.optString("title", "").trim();
                if (title.length() < 1 || title.length() > 180 ||
                    !pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+") ||
                    !launchers().containsKey(pkg)) {
                    write(socket.getOutputStream(), 400, "{\"error\":\"invalid_search\"}");
                    return;
                }
                String mode = searchTitleInApp(pkg, title);
                // Preserve mode=search for AnyTV 2.9.2 compatibility when a
                // known official title link was dispatched, not confirmed.
                write(socket.getOutputStream(), 200, "{\"mode\":\"" + mode + "\"}");
            } else if (parts[0].equals("POST") && parts[1].equals("/title-open")) {
                // Future clients may submit a provider URL they resolved
                // independently. Never accept arbitrary web URLs or packages.
                if (size < 1 || size > 2048) {
                    write(socket.getOutputStream(), 400, "{\"error\":\"invalid_body\"}");
                    return;
                }
                byte[] bytes = new byte[size];
                int read = 0;
                while (read < size) {
                    int count = in.read(bytes, read, size - read);
                    if (count < 0) throw new java.io.IOException("Incomplete HTTP body");
                    read += count;
                }
                JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                String pkg = obj.optString("package", "");
                String url = obj.optString("url", "");
                if (!validDisneyTitleLink(pkg, url)) {
                    write(socket.getOutputStream(), 400, "{\"error\":\"invalid_title_link\"}");
                    return;
                }
                boolean requested = openDisneyTitle(pkg, url);
                write(socket.getOutputStream(), requested ? 200 : 409,
                        "{\"mode\":\"" + (requested ? "detail_requested" : "unavailable") + "\"}");
            } else if (parts[0].equals("POST") && parts[1].equals("/headphones")) {
                // Only pre-paired devices explicitly selected in this TV app.
                String result = requestHeadphones();
                write(socket.getOutputStream(), 200, "{\"status\":\"" + result + "\"}");
            } else if (parts[0].equals("POST") && parts[1].equals("/headphones/settings")) {
                // Explicit user request. Android can still disallow launching UI from background.
                boolean opened = false;
                try {
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                    opened = true;
                } catch (RuntimeException error) {
                    Log.w(TAG, "Bluetooth settings require foreground interaction", error);
                }
                write(socket.getOutputStream(), opened ? 200 : 409,
                        "{\"opened\":" + opened + "}");
            } else if (parts[0].equals("GET") && parts[1].equals("/headphones/status")) {
                write(socket.getOutputStream(), 200,
                        "{\"status\":\"" + headphoneStatus + "\"}");
            } else {
                write(socket.getOutputStream(), 404, "{\"error\":\"invalid_route\"}");
            }
        } catch (Exception e) {
            Log.w(TAG, "LAN request failed", e);
        }
    }

    private String requestHeadphones() {
        if (Build.VERSION.SDK_INT >= 31 &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return "permission_required";
        }
        MappingStore.DeviceMapping mapping =
            MappingStore.get(context, KeyEvent.KEYCODE_PROG_RED);
        if (mapping == null) return "not_configured";
        try {
            headphoneStatus = "reconnecting";
            BluetoothRecoverAction.recover(context, mapping, result ->
                    headphoneStatus = result);
            // Final status is polled through /headphones/status.
            return "requested";
        } catch (SecurityException error) {
            return "permission_required";
        } catch (RuntimeException error) {
            Log.e(TAG, "Headphone recovery unavailable", error);
            return "not_supported";
        }
    }

    private String line(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (buf.size() > 4096) return null;
            if (b != '\r') buf.write(b);
        }
        if (b == -1 && buf.size() == 0) return null;
        return buf.toString("UTF-8");
    }

    private void write(OutputStream out, int status, String json) throws java.io.IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Error") + "\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Cache-Control: no-store\r\n" +
                "Connection: close\r\n" +
                "Content-Length: " + data.length + "\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    private Map<String, ResolveInfo> launchers() {
        PackageManager pm = context.getPackageManager();
        Map<String, ResolveInfo> apps = new LinkedHashMap<>();
        for (String category : new String[]{
                Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(category);
            List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
            for (ResolveInfo info : results) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                if (!pkg.equals(context.getPackageName()) && !apps.containsKey(pkg))
                    apps.put(pkg, info);
            }
        }
        return apps;
    }

    private String pngBase64(Drawable input) {
        try {
            Drawable icon = input.mutate();
            Bitmap image = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            icon.setBounds(0, 0, 96, 96);
            icon.draw(canvas);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 95, output);
            image.recycle();
            return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Throwable e) {
            return "";
        }
    }

    private JSONArray installedApps() {
        PackageManager pm = context.getPackageManager();
        JSONArray array = new JSONArray();
        for (Map.Entry<String, ResolveInfo> entry : launchers().entrySet()) {
            if (array.length() >= MAX_APPS) break;
            ResolveInfo info = entry.getValue();
            try {
                JSONObject app = new JSONObject();
                app.put("package", entry.getKey());
                app.put("name", String.valueOf(info.loadLabel(pm)));
                app.put("icon", pngBase64(info.loadIcon(pm)));
                array.put(app);
            } catch (Exception ignored) {}
        }
        return array;
    }

    private String searchTitleInApp(String pkg, String title) {
        // Known, independently verified provider entity links are safe to
        // dispatch without scraping or guessing IDs from the TMDB id.
        String direct = verifiedDisneyTitleLink(pkg, title);
        if (direct != null && openDisneyTitle(pkg, direct)) {
            // Legacy phone 2.9.2 understands only "search" or "opened_app".
            // "search" prevents it from re-launching the provider home.
            return "search";
        }
        try {
            Intent search = new Intent(Intent.ACTION_SEARCH);
            search.setPackage(pkg);
            search.putExtra(SearchManager.QUERY, title);
            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (search.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(search);
                return "search";
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Installed app refused external title search", e);
        }
        return launchInstalled(pkg) ? "opened_app" : "unavailable";
    }

    private String verifiedDisneyTitleLink(String pkg, String title) {
        if (!DISNEY_PACKAGE.equals(pkg)) return null;
        String value = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT).trim();
        if (value.equals("zootopia 2") || value.equals("zootropolis 2")) {
            return ZOOTOPIA_2_LINK;
        }
        return null;
    }

    private boolean validDisneyTitleLink(String pkg, String url) {
        if (!DISNEY_PACKAGE.equals(pkg) || url == null || url.length() > 200) return false;
        try {
            Uri uri = Uri.parse(url);
            // No userinfo, arbitrary ports, query parameters, fragments,
            // redirects or non-Disney domains are accepted through LAN.
            return "https".equals(uri.getScheme())
                    && "www.disneyplus.com".equals(uri.getHost())
                    && uri.getPort() == -1
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && uri.getPath() != null
                    && uri.getPath().matches("/browse/entity-[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean openDisneyTitle(String pkg, String url) {
        if (!validDisneyTitleLink(pkg, url) || !launchers().containsKey(pkg)) return false;
        Uri uri = Uri.parse(url);
        // Launch only the verified installed provider, never an unpinned
        // browser intent or a third-party activity.
        Intent deepLink = new Intent(Intent.ACTION_VIEW, uri);
        deepLink.setPackage(pkg);
        deepLink.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (deepLink.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(deepLink);
                return true;  // Request dispatched, NOT proof the film was displayed.
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Disney+ official entity VIEW refused", e);
        }
        // Disney+ also advertises this provider scheme in multistream's
        // verified manifest inventory. Only derive it from validated UUID.
        String id = uri.getLastPathSegment().substring("entity-".length());
        Intent alternate = new Intent(Intent.ACTION_VIEW, Uri.parse("disneyplus://" + id));
        alternate.setPackage(pkg);
        alternate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (alternate.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(alternate);
                return true;
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Disney+ app scheme refused", e);
        }
        return false;
    }

    private boolean launchInstalled(String pkg) {
        if (pkg == null || !pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+"))
            return false;
        if (!launchers().containsKey(pkg)) return false;
        PackageManager pm = context.getPackageManager();
        try {
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "App launch failed: " + pkg, e);
            return false;
        }
    }
}
