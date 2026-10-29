package dk.fez.bluetoothquickconnect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import android.content.Intent;
import android.util.TypedValue;
import android.graphics.drawable.StateListDrawable;
import tcl.shared.TclUpdateManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * User-visible LAN companion. No accessibility, reading on-screen data,
 * system overlays, notification access, or Bluetooth scanning permissions.
 */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(0, 0, 0);
    private static final int TEXT = Color.rgb(245, 245, 245);
    private static final int MUTED = Color.rgb(155, 155, 155);
    private TextView status;
    private TextView updateStatus;
    private static final int REQ_BT_CONNECT = 11;
    private int dp(float v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
    private GradientDrawable buttonShape(int color, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(selected ? 2 : 1),
                selected ? Color.WHITE : Color.rgb(65, 65, 65));
        return bg;
    }

    private TextView label(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setPadding(0, dp(2), 0, dp(4));
        return text;
    }

    private Button action(String value, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setFocusable(true);
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        // Android TV D-pad focus state: bright border + blue background.
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[]{android.R.attr.state_pressed},
                buttonShape(Color.rgb(205, 24, 37), true));
        backgrounds.addState(new int[]{android.R.attr.state_focused},
                buttonShape(Color.rgb(185, 20, 34), true));
        backgrounds.addState(new int[]{},
                buttonShape(Color.rgb(27, 27, 27), false));
        button.setBackground(backgrounds);
        button.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        button.setOnClickListener(onClick);
        return button;
    }

    private LinearLayout actionRow(Button first, Button second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        for (Button button : new Button[]{first, second}) {
            LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setMargins(dp(4), dp(3), dp(4), dp(3));
            button.setLayoutParams(item);
            row.addView(button);
        }
        return row;
    }

    private String installedVersion() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return name == null ? "?" : name.split("-")[0];
        } catch (PackageManager.NameNotFoundException ex) {
            return "?";
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(14), dp(24), dp(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(600)),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        frame.addView(root, centered);

        TextView versionTag = label("v" + installedVersion(), 12, MUTED);
        versionTag.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        root.addView(versionTag, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tcl_banner);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(dp(220), dp(66)));
        root.addView(label("Conecte seu celular à TV", 20, TEXT));
        root.addView(label("Código de pareamento", 14, MUTED));

        TextView code = label(TvAppBridge.pairingCode(this), 36, Color.rgb(237, 45, 58));
        code.setTextIsSelectable(true);
        code.setPadding(0, 9, 0, 13);
        root.addView(code);

        root.addView(label(
                "No celular, informe o código acima. Use a mesma rede Wi-Fi.", 14, MUTED));

        status = label("Verificando serviço...", 16, TEXT);
        root.addView(status);

        Button connectButton = action("Conectar", v -> startBridge());
        root.addView(actionRow(
                connectButton,
                action("Atualizar", v ->
                        TclUpdateManager.check(this, msg -> {
                            if (updateStatus != null) updateStatus.setText(msg);
                        }))));
        updateStatus = label("", 13, MUTED);
        root.addView(updateStatus);

        TextView headphone = label("FONES BLUETOOTH", 13, MUTED);
        root.addView(headphone);
        root.addView(actionRow(
                action("Escolher fone", v -> chooseHeadphones()),
                action("Reconectar fone", v -> reconnectHeadphones())));
        root.addView(actionRow(
                action("Bluetooth da TV", v -> openBluetoothSettings()),
                action("Desativar conexão", v -> {
                    stopService(new Intent(this, TclCatalogService.class));
                    status.setText("Conexão desativada");
                })));

        root.addView(label("Conexão local e privada", 12, MUTED));

        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 26) connectButton.setDefaultFocusHighlightEnabled(true);
        connectButton.requestFocus();
        startBridge();
    }

    private boolean bluetoothReady() {
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT_CONNECT);
            if (status != null) status.setText("Autorize o acesso aos fones Bluetooth já pareados.");
            return false;
        }
        return true;
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT_CONNECT) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showHeadphones();
            else if (status != null)
                status.setText("Permissão Bluetooth negada. O catálogo de apps continua funcionando.");
        }
    }

    private void chooseHeadphones() {
        if (bluetoothReady()) showHeadphones();
    }

    private void showHeadphones() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                status.setText("Esta TV não informou um adaptador Bluetooth.");
                return;
            }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            List<BluetoothDevice> found = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            if (bonded != null) {
                for (BluetoothDevice device : bonded) {
                    String name = device.getName();
                    if (name == null || name.trim().isEmpty()) name = "Dispositivo Bluetooth";
                    found.add(device);
                    labels.add(name + " (" + device.getAddress() + ")");
                }
            }
            if (found.isEmpty()) {
                status.setText("Nenhum dispositivo pareado. Faça o pareamento nas configurações Bluetooth.");
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Escolher fones já pareados")
                    .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                        BluetoothDevice chosen = found.get(index);
                        String name = chosen.getName();
                        MappingStore.save(this, KeyEvent.KEYCODE_PROG_RED,
                                new MappingStore.DeviceMapping(chosen.getAddress(),
                                        name != null ? name : chosen.getAddress()));
                        status.setText("Fones selecionados: " +
                                (name != null ? name : chosen.getAddress()));
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (SecurityException error) {
            status.setText("Sem permissão para consultar dispositivos Bluetooth.");
        } catch (RuntimeException error) {
            status.setText("Bluetooth indisponível: " + error.getMessage());
        }
    }

    private void reconnectHeadphones() {
        if (!bluetoothReady()) return;
        MappingStore.DeviceMapping mapping =
                MappingStore.get(this, KeyEvent.KEYCODE_PROG_RED);
        if (mapping == null) {
            status.setText("Escolha primeiro um fone Bluetooth pareado.");
            return;
        }
        status.setText("Desconectando e reconectando " + mapping.name + "...");
        BluetoothRecoverAction.recover(this, mapping, result -> runOnUiThread(() -> {
            switch (result) {
                case "connected":
                    status.setText("Fones conectados novamente: " + mapping.name);
                    break;
                case "api_restricted":
                    status.setText("A TV bloqueou o comando Bluetooth. " +
                        "Use a opção Configurações Bluetooth, se desejar.");
                    break;
                case "not_paired":
                    status.setText("Fones não estão mais pareados com a TV.");
                    break;
                case "permission_required":
                    status.setText("Autorize o Bluetooth no aplicativo da TV.");
                    break;
                default:
                    status.setText("Não foi possível confirmar a conexão: " + result);
            }
        }));
    }

    private void openBluetoothSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (RuntimeException error) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (RuntimeException ignored) {
                status.setText("Abra o menu Bluetooth nas configurações da TV.");
            }
        }
    }

    private void startBridge() {
        try {
            Intent intent = new Intent(this, TclCatalogService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            status.setText("Iniciando conexão...");
            new Handler(Looper.getMainLooper()).postDelayed(this::refreshStatus, 450);
        } catch (RuntimeException error) {
            status.setText("Não foi possível ativar o serviço: " + error.getMessage());
        }
    }

    private void refreshStatus() {
        if (status != null) {
            status.setText(TclCatalogService.isRunning()
                    ? "Conexão ativa — aguardando celular"
                    : "Conexão inativa. Selecione Ativar para tentar novamente.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }
}
