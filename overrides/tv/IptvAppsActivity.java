package dk.fez.bluetoothquickconnect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual list of installed, launchable applications on the TV.
 * Selected TV app is persisted locally on the TV, not guessed on the phone.
 */
public final class IptvAppsActivity extends Activity {
    private static final String TAG = "TclTvApps";
    private static final String PREFS = "tcl_tv_selected_apps";
    private static final String KEY = "iptv_package";

    static void openChooser(Context context) {
        Intent intent = new Intent(context, IptvAppsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    static void launchSavedOrChoose(Context context) {
        String pkg = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (pkg != null && launchApp(context, pkg)) return;
        openChooser(context);
    }

    private static boolean launchApp(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "TV app no longer available: " + pkg, e);
            return false;
        }
    }

    private static final class TvApp {
        String packageName;
        String label;
        Drawable icon;
        TvApp(String pkg, String name, Drawable drawable) {
            packageName=pkg; label=name; icon=drawable;
        }
    }

    private List<TvApp> installedApps() {
        PackageManager pm=getPackageManager();
        Map<String,TvApp> packages = new LinkedHashMap<>();
        for (String category: new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent query=new Intent(Intent.ACTION_MAIN);
            query.addCategory(category);
            List<ResolveInfo> matches=pm.queryIntentActivities(query, 0);
            for (ResolveInfo entry: matches) {
                if(entry.activityInfo==null) continue;
                String pkg=entry.activityInfo.packageName;
                if(pkg.equals(getPackageName())||packages.containsKey(pkg)) continue;
                String label=String.valueOf(entry.loadLabel(pm));
                Drawable icon=entry.loadIcon(pm);
                packages.put(pkg, new TvApp(pkg,label,icon));
            }
        }
        List<TvApp> result=new ArrayList<>(packages.values());
        Collections.sort(result, new Comparator<TvApp>() {
            private int rank(String label) {
                String n=label.toLowerCase(java.util.Locale.ROOT);
                if (n.contains("iptv")||n.contains("tivimate")||n.contains("xciptv")
                    ||n.contains("smarters")||n.contains("televizo")
                    ||n.contains("kodi")||n.contains("stream")||n.contains("ott")) return 0;
                return 1;
            }
            @Override public int compare(TvApp a,TvApp b) {
                int ra=rank(a.label), rb=rank(b.label);
                if(ra!=rb) return ra-rb;
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return result;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private TextView text(String txt, float sp, int color) {
        TextView v=new TextView(this);
        v.setText(txt);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(dp(4),dp(8),dp(4),dp(12));
        return v;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Escolha seu IPTV");
        drawUi();
    }

    private void drawUi() {
        int textColor=Color.WHITE;
        int secondary=Color.rgb(187,197,213);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(Color.rgb(18,24,36));
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42),dp(26),dp(42),dp(35));
        scroll.addView(root);
        root.addView(text("Escolha o IPTV instalado na TCL", 28, textColor));
        root.addView(text("A lista abaixo foi lida da própria televisão. Toque em um aplicativo com OK para salvar e abrir. No celular, toque no ícone IPTV novamente para abrir o selecionado. Segure o ícone IPTV para alterar.", 16, secondary));
        String current=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY,"");
        if(!current.isEmpty()) {
            Button reset=new Button(this);
            reset.setAllCaps(false);
            reset.setText("Redefinir aplicativo IPTV");
            reset.setOnClickListener(v->{
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove(KEY).apply();
                drawUi();
            });
            root.addView(reset);
        }
        List<TvApp> apps=installedApps();
        if(apps.isEmpty()) {
            root.addView(text("Nenhum aplicativo encontrado. Verifique se a TV está no modo Android TV/Google TV.",18,textColor));
        }
        for(TvApp app:apps) {
            Button b=new Button(this);
            b.setText((app.packageName.equals(current) ? "✓  " : "")+app.label);
            b.setAllCaps(false);
            b.setTextSize(19);
            b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            b.setTextColor(Color.WHITE);
            b.setPadding(dp(18),dp(10),dp(12),dp(10));
            b.setMinHeight(dp(70));
            if(app.icon!=null) {
                try {
                    app.icon.setBounds(0,0,dp(42),dp(42));
                    b.setCompoundDrawables(app.icon,null,null,null);
                    b.setCompoundDrawablePadding(dp(18));
                } catch(RuntimeException ignore) {}
            }
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            p.bottomMargin=dp(7);
            root.addView(b,p);
            b.setOnClickListener(v->{
                if(launchApp(this,app.packageName)) {
                    getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                        .putString(KEY,app.packageName).apply();
                    finish();
                }else {
                    Toast.makeText(this,"A TCL não permitiu abrir este aplicativo.",Toast.LENGTH_LONG).show();
                }
            });
        }
        Button refresh=new Button(this);
        refresh.setText("Atualizar lista de aplicativos");
        refresh.setAllCaps(false);
        refresh.setOnClickListener(v->drawUi());
        root.addView(refresh);
        setContentView(scroll);
    }
}
