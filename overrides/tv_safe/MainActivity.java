package dk.fez.bluetoothquickconnect;

import android.app.Activity;
import android.content.Intent;
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
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(18, 65, 115));
        bg.setCornerRadius(20f);
        button.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 66);
        params.setMargins(0, 9, 0, 9);
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
        root.setPadding(50, 28, 50, 28);
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
                "No celular, abra Controle TCL Livre 1.9 e informe o código uma única vez. "
                + "A TV e o celular precisam estar na mesma rede local.", 15, MUTED));

        status = label("Verificando serviço...", 16, TEXT);
        root.addView(status);

        root.addView(action("Ativar conexão com o celular", v -> startBridge()));
        root.addView(action("Desativar conexão", v -> {
            stopService(new Intent(this, TclCatalogService.class));
            status.setText("Conexão desativada");
        }));

        root.addView(label(
                "Permissões: somente acesso à rede local e um serviço visível enquanto estiver ativo. "
                + "Nenhuma permissão de Acessibilidade ou acesso aos dados da tela.", 14, MUTED));
        root.addView(label(
                "O serviço permanece ativo ao voltar para a tela inicial da TV. "
                + "Se a TV reiniciar, abra este aplicativo para reativar.", 14, MUTED));

        setContentView(scroll);
        startBridge();
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
