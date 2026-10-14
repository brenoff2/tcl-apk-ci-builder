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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * User-visible LAN companion. No accessibility, reading on-screen data,
 * system overlays, notification access, or Bluetooth scanning permissions.
 */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(5, 12, 22);
    private static final int TEXT = Color.rgb(236, 244, 255);
    private static final int MUTED = Color.rgb(164, 188, 217);
    private TextView status;
    private TextView updateStatus;
    private static final int REQ_BT_CONNECT = 11;
    private int dp(float v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
    private GradientDrawable buttonShape(int color, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(selected ? 3 : 1),
                selected ? Color.rgb(255, 230, 86) : Color.rgb(75, 111, 148));
        return bg;
    }

    private TextView label(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setPadding(0, 5, 0, 9);
        return text;
    }

    private Button action(String value, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMaxLines(3);
        button.setFocusable(true);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        // Android TV D-pad focus state: bright border + blue background.
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[]{android.R.attr.state_pressed},
                buttonShape(Color.rgb(20, 118, 207), true));
        backgrounds.addState(new int[]{android.R.attr.state_focused},
                buttonShape(Color.rgb(19, 88, 150), true));
        backgrounds.addState(new int[]{},
                buttonShape(Color.rgb(16, 43, 75), false));
        button.setBackground(backgrounds);
        button.setMinHeight(dp(76));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(8));
        button.setLayoutParams(params);
        button.setOnClickListener(onClick);
        return button;
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
        root.setPadding(dp(24), dp(18), dp(24), dp(22));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        root.addView(label("CONTROLE TCL TV", 28, TEXT));
        root.addView(label("Complemento de aplicativos instalados", 17, MUTED));
        root.addView(label("Código de vinculação da TV", 17, MUTED));

        TextView code = label(TvAppBridge.pairingCode(this), 39, Color.rgb(83, 168, 255));
        code.setTextIsSelectable(true);
        code.setPadding(0, 9, 0, 13);
        root.addView(code);

        root.addView(label(
                "No celular, abra Controle TCL Livre e informe o código uma única vez. "
                + "A TV e o celular precisam estar na mesma rede local.", 15, MUTED));

        status = label("Verificando serviço...", 16, TEXT);
        root.addView(status);

        root.addView(action("Ativar conexão com o celular", v -> startBridge()));

        root.addView(action("Buscar atualizações", v ->
                TclUpdateManager.check(this, msg -> {
                    if (updateStatus != null) updateStatus.setText(msg);
                })));
        updateStatus = label("Atualizações: verifique a nova versão aqui.", 15, MUTED);
        root.addView(updateStatus);

        TextView headphone = label("Fones Bluetooth pareados", 16, MUTED);
        root.addView(headphone);
        root.addView(action("Selecionar fone de ouvido", v -> chooseHeadphones()));
        root.addView(action("Reconectar fone selecionado", v -> reconnectHeadphones()));
        root.addView(action("Configurações Bluetooth da TV", v -> openBluetoothSettings()));
        root.addView(action("Desativar conexão", v -> {
            stopService(new Intent(this, TclCatalogService.class));
            status.setText("Conexão desativada");
        }));

        root.addView(label(
                "O catálogo de aplicativos usa somente rede local. A conexão com fones "
                + "já pareados solicita Bluetooth quando você escolher os fones. "
                + "Não há permissão de Acessibilidade nem de leitura da tela.", 14, MUTED));
        root.addView(label(
                "O serviço permanece ativo ao voltar para a tela inicial da TV. "
                + "Se a TV reiniciar, abra este aplicativo para reativar.", 14, MUTED));

        setContentView(scroll);
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
                status.setText("Esta TCL não informou um adaptador Bluetooth.");
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
                    status.setText("A TCL bloqueou o comando Bluetooth. " +
                        "Use a opção Configurações Bluetooth, se desejar.");
                    break;
                case "not_paired":
                    status.setText("Fones não estão mais pareados com a TCL.");
                    break;
                case "permission_required":
                    status.setText("Autorize o Bluetooth no aplicativo da TCL.");
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
                status.setText("Abra o menu Bluetooth nas configurações da TCL.");
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
