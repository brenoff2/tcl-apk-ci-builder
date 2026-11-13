package anytv.adb;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One-shot, read-only device-side metadata. Invoked with app_process over previously authorized ADB. */
public final class AnyTvRemoteMeta {
    private static Context systemContext() throws Exception {
        Class<?> type = Class.forName("android.app.ActivityThread");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object thread = constructor.newInstance();
        Field current = type.getDeclaredField("sCurrentActivityThread");
        current.setAccessible(true);
        current.set(null, thread);
        Method method = type.getDeclaredMethod("getSystemContext");
        method.setAccessible(true);
        Context context = (Context) method.invoke(thread);
        Method newApplication = Class.forName("android.app.Instrumentation")
            .getMethod("newApplication", Class.class, Context.class);
        Object application = newApplication.invoke(null, Application.class, context);
        Field initial = type.getDeclaredField("mInitialApplication");
        initial.setAccessible(true);
        initial.set(thread, application);
        return context;
    }

    private static void collect(Map<String, ResolveInfo> found, PackageManager pm, String category) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(category);
        List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                String pkg = info.activityInfo.packageName;
                // Prefer leanback metadata when the same package supports both.
                if (!found.containsKey(pkg) || Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)) {
                    found.put(pkg, info);
                }
            }
        }
    }

    private static String clean(String value) {
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String b64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        PackageManager pm = systemContext().getPackageManager();
        Map<String, ResolveInfo> apps = new LinkedHashMap<>();
        collect(apps, pm, Intent.CATEGORY_LAUNCHER);
        collect(apps, pm, Intent.CATEGORY_LEANBACK_LAUNCHER);
        System.out.println("ANYTV_REMOTE_META_V1");
        for (Map.Entry<String, ResolveInfo> item : apps.entrySet()) {
            String pkg = item.getKey();
            if (!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) continue;
            try {
                ResolveInfo info = item.getValue();
                CharSequence label = info.loadLabel(pm);
                String name = clean(label == null ? pkg : label.toString());
                String icon = "";
                try {
                    Drawable drawable = info.loadIcon(pm);
                    if (drawable != null) {
                        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
                        try {
                            drawable.setBounds(0, 0, 80, 80);
                            drawable.draw(new Canvas(bitmap));
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    && out.size() <= 32768) icon = b64(out.toByteArray());
                        } finally {
                            bitmap.recycle();
                        }
                    }
                } catch (Exception ignored) { }
                System.out.println("ANYTV_APP\t" + pkg + "\t"
                    + b64(name.getBytes(StandardCharsets.UTF_8)) + "\t" + icon);
            } catch (Exception error) {
                System.out.println("ANYTV_FAIL\t" + pkg + "\t"
                    + error.getClass().getSimpleName());
            }
        }
        System.out.println("ANYTV_REMOTE_META_END");
    }
}
