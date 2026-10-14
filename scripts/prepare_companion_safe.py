#!/usr/bin/env python3
"""Create a minimal Android TV companion that DOES NOT declare Accessibility.

This intentionally excludes the legacy keyboard-interception/Bluetooth utility:
Google Play Protect blocked its sideloaded AccessibilityService. The Android TV
installed-app catalog is a separate user-visible foreground service.
"""
from pathlib import Path
from shutil import copyfile

app = Path("tv-source/app")
base = app / "src/main/java/dk/fez/bluetoothquickconnect"
assets = Path("overrides/tv_safe")

if not base.is_dir():
    raise RuntimeError("Pinned upstream Android project is not checked out")

# Remove ALL upstream compiled Java classes, not just the accessibility entry.
# Unused legacy classes should not end up in the distributed binary.
# The three harmless upstream Bluetooth helper files are retained, but
# their remote-color-key accessibility entry point is excluded.
safe_bluetooth_helpers = {
    "BluetoothConnectAction.java", "MappingStore.java",
    "DeviceSettingsFallback.java"
}
for file in base.glob("*.java"):
    if file.name not in safe_bluetooth_helpers:
        file.unlink()

for file in ("MainActivity.java", "TclCatalogService.java", "TvAppBridge.java"):
    copyfile(assets / file, base / file)
copyfile("overrides/tv/BluetoothRecoverAction.java", base / "BluetoothRecoverAction.java")

manifest = app / "src/main/AndroidManifest.xml"
copyfile(assets / "AndroidManifest.xml", manifest)

drawable = app / "src/main/res/drawable"
for file in drawable.glob("*.xml"):
    file.unlink()
copyfile(assets / "tcl_icon.xml", drawable / "tcl_icon.xml")
copyfile(assets / "tcl_banner.xml", drawable / "tcl_banner.xml")

# The old accessibility metadata XML is not part of this build.
xml_dir = app / "src/main/res/xml"
if xml_dir.exists():
    for file in xml_dir.iterdir():
        if file.is_file():
            file.unlink()

gradle = app / "build.gradle.kts"
config = gradle.read_text()
changes = {
    'compileSdk = 33': 'compileSdk = 35',
    'applicationId = "dk.fez.bluetoothquickconnect"':
        'applicationId = "com.tclremote.tvcompanion"',
    'targetSdk = 33': 'targetSdk = 34',
    'versionCode = 1': 'versionCode = 4',
    'versionName = "0.1.0"': 'versionName = "1.3-Catalogo-Fones"',
}
for old, new in changes.items():
    if config.count(old) != 1:
        raise RuntimeError(f"Unexpected upstream Gradle config: {old}")
    config = config.replace(old, new, 1)
gradle.write_text(config)

actual = manifest.read_text()
assert "android.permission.INTERNET" in actual
assert "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in actual
assert "TclCatalogService" in actual
assert "BIND_ACCESSIBILITY_SERVICE" not in actual
assert "RemoteButtonService" not in actual
assert "android.accessibilityservice" not in actual
assert "android.permission.BLUETOOTH_CONNECT" in actual
assert "android.permission.BLUETOOTH" in actual
assert "android.permission.BLUETOOTH_SCAN" not in actual
assert "android.permission.BLUETOOTH_PRIVILEGED" not in actual
assert not (base / "RemoteButtonService.java").exists()

assert "installedApps()" in (base / "TvAppBridge.java").read_text()
assert '"/launch"' in (base / "TvAppBridge.java").read_text()
assert '"/headphones"' in (base / "TvAppBridge.java").read_text()
assert "MappingStore" in (base / "MainActivity.java").read_text()
print("TCL_TV_SAFE_1_3_NO_ACCESSIBILITY_REAL_APPS_AND_PAIRED_HEADPHONES_OK")
