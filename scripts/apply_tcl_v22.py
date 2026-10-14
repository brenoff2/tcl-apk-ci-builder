#!/usr/bin/env python3
"""TCL phone v2.2: actual TV Back via Android BackHandler, update button,
same shared installer protocol as TV v1.4, stable single-app package.
Run AFTER prepare_tcl.py + apply_tcl_v21.py. Fail closed on changes.
"""
from pathlib import Path
from shutil import copyfile

APP = Path("source/app")
SRC = APP / "src/main/java/com/hari/androidtvremote"


def replace_once(file: Path, old: str, new: str) -> None:
    source = file.read_text()
    if source.count(old) != 1:
        raise RuntimeError(f"Expected exact anchor once; {source.count(old)} in {file}: {old[:90]}")
    file.write_text(source.replace(old, new, 1))


# User-owned common updater; exact same code shared by phone and TV.
shared = APP / "src/main/java/tcl/shared"
shared.mkdir(parents=True, exist_ok=True)
for filename in ("TclUpdateManager.java", "TclInstallResultReceiver.java"):
    copyfile(Path("overrides/common") / filename, shared / filename)

manifest = APP / "src/main/AndroidManifest.xml"
replace_once(manifest,
    '<uses-permission android:name="android.permission.INTERNET" />',
    '<uses-permission android:name="android.permission.INTERNET" />\n'
    '    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />')
replace_once(manifest,
    '        <service\n            android:name=".services.CastService"',
    '        <receiver android:name="tcl.shared.TclInstallResultReceiver"\n'
    '            android:exported="false" />\n'
    '        <service\n            android:name=".services.CastService"')

screen = SRC / "ui/app/TclApprovedHomeScreen.kt"
replace_once(screen,
    'import androidx.activity.compose.rememberLauncherForActivityResult',
    'import androidx.activity.compose.BackHandler\n'
    'import androidx.activity.compose.rememberLauncherForActivityResult')
replace_once(screen,
    '    val isConnected = sessionState.connectedDevice != null\n',
    '    val isConnected = sessionState.connectedDevice != null\n'
    '    // Android device Back should control the TV while the remote page is\n'
    '    // active. Dialogs keep their usual dismissal behavior first.\n'
    '    BackHandler(enabled = isConnected && !showKeyboard && headsetStatus == null) {\n'
    '        onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK)\n'
    '    }\n')

# Because BackHandler is installed after some state variables, place it AFTER
# the local state declarations rather than before those variables exist.
screen_source = screen.read_text()
block = ('    // Android device Back should control the TV while the remote page is\n'
         '    // active. Dialogs keep their usual dismissal behavior first.\n'
         '    BackHandler(enabled = isConnected && !showKeyboard && headsetStatus == null) {\n'
         '        onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK)\n'
         '    }\n')
if screen_source.count(block) != 1:
    raise RuntimeError("BackHandler insertion failed")
screen_source = screen_source.replace(block, "")
anchor = '    var headsetStatus by remember { mutableStateOf<String?>(null) }\n'
if screen_source.count(anchor) != 1:
    raise RuntimeError("BackHandler state anchor changed")
screen_source = screen_source.replace(anchor, anchor + block, 1)
screen.write_text(screen_source)

settings = SRC / "ui/app/SettingsScreen.kt"
replace_once(settings,
    'import androidx.compose.material.icons.outlined.Autorenew',
    'import androidx.compose.material.icons.outlined.Autorenew\n'
    'import tcl.shared.TclUpdateManager')
replace_once(settings,
    '    val context = LocalContext.current\n',
    '    val context = LocalContext.current\n'
    '    var updateStatus by remember { mutableStateOf("Confira a versão oficial no GitHub") }\n')
replace_once(settings,
    '                item {\n                    SettingSubtitle(text = "Remote")\n                }\n',
    '                item {\n'
    '                    SettingGroupItem(\n'
    '                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),\n'
    '                        title = "Buscar atualizações",\n'
    '                        desc = updateStatus,\n'
    '                        icon = Icons.Outlined.Autorenew,\n'
    '                        onClick = {\n'
    '                            val activity = context as? android.app.Activity\n'
    '                            if (activity == null) {\n'
    '                                updateStatus = "Não foi possível abrir o instalador."\n'
    '                            } else {\n'
    '                                TclUpdateManager.check(activity) { updateStatus = it }\n'
    '                            }\n'
    '                        }\n'
    '                    )\n'
    '                }\n'
    '                item {\n'
    '                    SettingSubtitle(text = "Remote")\n'
    '                }\n')

gradle = APP / "build.gradle.kts"
replace_once(gradle, 'versionCode = 21', 'versionCode = 23')
replace_once(gradle, 'versionName = "2.1-TCL"', 'versionName = "2.3-TCL"')

assert 'applicationId = "com.tclremote.livre"' in gradle.read_text()
assert "BackHandler(enabled = isConnected && !showKeyboard" in screen.read_text()
assert "REQUEST_INSTALL_PACKAGES" in manifest.read_text()
assert "TclUpdateManager.check" in settings.read_text()
print("TCL_PHONE_22_BACK_AND_UPDATES_READY")
