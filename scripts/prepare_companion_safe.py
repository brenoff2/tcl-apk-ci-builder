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

# Common verified, user-confirmed APK updater.
shared = app / "src/main/java/tcl/shared"
shared.mkdir(parents=True, exist_ok=True)
for java_file in ("TclUpdateManager.java", "TclInstallResultReceiver.java"):
    copyfile("overrides/common/" + java_file, shared / java_file)

manifest = app / "src/main/AndroidManifest.xml"
copyfile(assets / "AndroidManifest.xml", manifest)

drawable = app / "src/main/res/drawable"
for file in drawable.glob("*.xml"):
    file.unlink()
from PIL import Image
brand_icon = Image.open("assets/anytv_icon.png").convert("RGBA")
brand_icon.thumbnail((432, 432), Image.Resampling.LANCZOS)
brand_icon.save(drawable / "tcl_icon.png", format="PNG", optimize=True)
logo = Image.open("assets/anytv_logo.png").convert("RGBA")
logo.thumbnail((580, 200), Image.Resampling.LANCZOS)
banner = Image.new("RGBA", (640, 360), (5, 12, 22, 255))
banner.alpha_composite(logo, ((640 - logo.width)//2, (360 - logo.height)//2))
banner.save(drawable / "tcl_banner.png", format="PNG", optimize=True)

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
    'versionCode = 1': 'versionCode = 12',
    'versionName = "0.1.0"': 'versionName = "2.1-AnyTV"',
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
assert "android.permission.REQUEST_INSTALL_PACKAGES" in actual
assert "TclInstallResultReceiver" in actual
assert "TclUpdateManager.check" in (base / "MainActivity.java").read_text()
assert "state_focused" in (base / "MainActivity.java").read_text()
assert "android.permission.BLUETOOTH" in actual
assert "android.permission.BLUETOOTH_SCAN" not in actual
assert "android.permission.BLUETOOTH_PRIVILEGED" not in actual
assert not (base / "RemoteButtonService.java").exists()

assert "installedApps()" in (base / "TvAppBridge.java").read_text()
assert '"/launch"' in (base / "TvAppBridge.java").read_text()
assert '"/headphones"' in (base / "TvAppBridge.java").read_text()
assert '"/title-search"' in (base / "TvAppBridge.java").read_text()
assert "MappingStore" in (base / "MainActivity.java").read_text()
print("TCL_TV_SAFE_1_3_NO_ACCESSIBILITY_REAL_APPS_AND_PAIRED_HEADPHONES_OK")
