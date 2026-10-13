#!/usr/bin/env python3
"""Adapt MIT Bluetooth Quick Connect into TCL TV companion (local only)."""
from pathlib import Path
from shutil import copyfile

root=Path("tv-source")
base=root/"app/src/main/java/dk/fez/bluetoothquickconnect"
source=Path("overrides/tv")
def change(path,old,new):
    p=root/path
    s=p.read_text()
    n=s.count(old)
    if n!=1:
        raise RuntimeError(f"{path}: source changed, count={n}: {old[:80]}")
    p.write_text(s.replace(old,new,1))

for name in ("IptvAppsActivity.java","RemoteButtonService.java","BluetoothRecoverAction.java","TvAppBridge.java"):
    copyfile(source/name, base/name)

change("app/build.gradle.kts",
       'applicationId = "dk.fez.bluetoothquickconnect"',
       'applicationId = "com.tclremote.tvcompanion"')
change("app/build.gradle.kts",
       'versionName = "0.1.0"',
       'versionName = "1.0-TCL"')

manifest="app/src/main/AndroidManifest.xml"
change(manifest,
       '<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />',
       '<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />\n'
       '    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />\n\n'
       '    <queries>\n'
       '        <intent>\n'
       '            <action android:name="android.intent.action.MAIN" />\n'
       '            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />\n'
       '        </intent>\n'
       '        <intent>\n'
       '            <action android:name="android.intent.action.MAIN" />\n'
       '            <category android:name="android.intent.category.LAUNCHER" />\n'
       '        </intent>\n'
       '    </queries>')
change(manifest,
       'android:label="Bluetooth Quick Connect"\n        android:theme=',
       'android:label="Controle TCL TV"\n        android:theme=')
change(manifest,
       '        <service\n            android:name=".RemoteButtonService"',
       '        <activity\n            android:name=".IptvAppsActivity"\n'
       '            android:exported="false" />\n\n'
       '        <service\n            android:name=".RemoteButtonService"')
change(manifest,
       'android:label="Bluetooth Quick Connect"\n            android:permission=',
       'android:label="Controle TCL TV"\n            android:permission=')
change("app/src/main/res/values/styles.xml",
       "Maps the remote color buttons to selected paired Bluetooth devices.",
       "Usa apenas as teclas de cor para acessar aplicativos e recuperar fones Bluetooth.")

main="app/src/main/java/dk/fez/bluetoothquickconnect/MainActivity.java"
change(main,
       "import android.app.Activity;",
       "import android.Manifest;\n"
       "import android.content.Intent;\n"
       "import android.content.pm.PackageManager;\n"
       "import android.os.Build;\n"
       "import android.view.KeyEvent;\n"
       "import android.app.Activity;")
change(main,
       "        loadPairedDevices();\n        setContentView(createContentView());",
       '        if (Build.VERSION.SDK_INT >= 31 &&\n'
       '            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)\n'
       '                != PackageManager.PERMISSION_GRANTED) {\n'
       '            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);\n'
       '            setContentView(createContentView());\n'
       '        } else {\n'
       '            loadPairedDevices();\n'
       '            setContentView(createContentView());\n'
       '        }')
change(main,
       "    private View createContentView() {",
       '    @Override\n'
       '    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {\n'
       '        super.onRequestPermissionsResult(requestCode, permissions, grantResults);\n'
       '        if(requestCode==101 && grantResults.length>0 &&\n'
       '                grantResults[0]==PackageManager.PERMISSION_GRANTED) {\n'
       '            loadPairedDevices();\n'
       '            setContentView(createContentView());\n'
       '        }\n'
       '    }\n\n'
       '    private View createContentView() {')
change(main,
       'title.setText("Bluetooth Quick Connect");',
       'title.setText("Controle TCL TV");')
change(main,
       'description.setText("Choose which already paired Bluetooth device each color button connects. Select Disabled to leave a color button untouched.");',
       'description.setText("Escolha seus fones Bluetooth pareados abaixo. '
       'No celular, use Fones para tentar reiniciar a conexao. '
       'Use IPTV no celular para escolher aplicativos realmente instalados na TV. '
       'Ative o servico Controle TCL TV em Configuracoes > Acessibilidade.");')
change(main,
       '        for (MappingStore.ColorButton button : MappingStore.COLOR_BUTTONS) {\n            addMappingRow(root, button);\n        }',
       '        for (MappingStore.ColorButton button : MappingStore.COLOR_BUTTONS) {\n'
       '            if (button.keyCode == KeyEvent.KEYCODE_PROG_RED) addMappingRow(root, button);\n'
       '        }\n\n'
       '        Button appsButton = new Button(this);\n'
       '        appsButton.setText("Escolher IPTV entre os apps instalados");\n'
       '        styleActionButton(appsButton);\n'
       '        appsButton.setOnClickListener(view -> startActivity(new Intent(this,IptvAppsActivity.class)));\n'
       '        root.addView(appsButton);\n')
change(main,
       'refreshButton.setText("Refresh paired devices");',
       'refreshButton.setText("Atualizar fones pareados");')
change(main,
       'label.setText(button.label + " button");',
       'label.setText("Fones Bluetooth (botao vermelho)");')
change(main,
       'testButton.setText("Connect selected");',
       'testButton.setText("Testar / reiniciar fones");')
change(main,
       'BluetoothConnectAction.connectOrOpen(MainActivity.this, mapping);',
       'BluetoothRecoverAction.recoverOrOpen(MainActivity.this, mapping);')
change(main,
       'button.setText("Choose device: Disabled");',
       'button.setText("Escolher fones: ainda nao selecionados");')
change(main,
       'button.setText("Choose device: " + mapping.name);',
       'button.setText("Fones escolhidos: " + mapping.name);')
change(main,
       'setTitle(button.label + " button device")',
       'setTitle("Escolher fones Bluetooth")')
# Don't request privileged Bluetooth permissions: only normal BLUETOOTH_CONNECT.
assert "BLUETOOTH_PRIVILEGED" not in (root/manifest).read_text()
print("TCL_TV_COMPANION_V1_PATCH_OK")

# TV companion 1.1: paired local network API, lists actual installed app icons.
change(manifest,
       '    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />',
       '    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />\\n'
       '    <uses-permission android:name="android.permission.INTERNET" />')
change(main,
       'root.addView(title);',
       'root.addView(title);\\n\\n'
       '        TextView pairingCode = new TextView(this);\\n'
       '        pairingCode.setText("Codigo para conectar os apps ao celular: " + TvAppBridge.pairingCode(this));\\n'
       '        pairingCode.setTextSize(23);\\n'
       '        pairingCode.setTextColor(Color.rgb(149,205,255));\\n'
       '        pairingCode.setPadding(0, 8, 0, 18);\\n'
       '        root.addView(pairingCode);')
change("app/build.gradle.kts",
       'versionCode = 1', 'versionCode = 2')
change("app/build.gradle.kts",
       'versionName = "1.0-TCL"', 'versionName = "1.1-TCL"')
print("TCL_TV_APPS_BRIDGE_READY")
