#!/usr/bin/env python3
"""Adapt Apache-2.0 Android TV Remote source for TCL: no ads, no analytics, IP pairing."""
from pathlib import Path

ROOT = Path("source")
SRC = ROOT / "app/src/main/java/com/hari/androidtvremote"
def change(path, replacements):
    p=ROOT/path
    s=p.read_text(encoding="utf-8")
    for old,new in replacements:
        matches=s.count(old)
        if matches!=1:
            raise RuntimeError(f"{path}: expected 1 occurrence, got {matches}: {old[:90]!r}")
        s=s.replace(old,new,1)
    p.write_text(s,encoding="utf-8")

change("build.gradle.kts", [
    ("    alias(libs.plugins.google.services) apply false\n",""),
    ("    alias(libs.plugins.firebase.crashlytics) apply false\n","")
])
change("app/build.gradle.kts", [
    ("    alias(libs.plugins.google.services)\n",""),
    ("    alias(libs.plugins.firebase.crashlytics)\n",""),
    ("    implementation(platform(libs.firebase.bom))\n",""),
    ("    implementation(libs.firebase.analytics)\n",""),
    ("    implementation(libs.firebase.crashlytics)\n",""),
    ('applicationId = "com.hari.androidtvremote"','applicationId = "com.tclremote.livre"'),
    ("targetSdk = 36","targetSdk = 35"),
    ('versionName = "1.10"','versionName = "1.0-TCL"')
])
change("app/src/main/res/values/strings.xml",[
    ('<string name="app_name">Android TV Remote</string>','<string name="app_name">Controle TCL Livre</string>')
])
(SRC/"utils/AnalyticsHelper.kt").write_text("""
package com.hari.androidtvremote.utils
import android.content.Context
import android.os.Bundle
/** Offline-only no-op. No analytics SDK and no telemetry calls. */
class AnalyticsHelper private constructor() {
    companion object {
        fun getInstance(context: Context): AnalyticsHelper = AnalyticsHelper()
    }
    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {}
    fun logEvent(eventName: String, params: Bundle? = null) {}
    fun logDeviceConnection(deviceName: String, success: Boolean, errorMsg: String? = null) {}
    fun logCastMedia(mediaType: String, success: Boolean) {}
}
""".strip()+"\n", encoding="utf-8")
(SRC/"utils/CrashlyticsTree.kt").write_text("""
package com.hari.androidtvremote.utils
import timber.log.Timber
/** No crash or event telemetry. */
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
}
""".strip()+"\n", encoding="utf-8")

# Default streaming shortcuts for Brazil; users can customize them in the app.
p=SRC/"ui/app/RemoteCustomization.kt"
s=p.read_text(encoding="utf-8")
start=s.index("private val defaultRemoteShortcutApps = listOf(")
end=s.index("\nfun defaultRemoteShortcutOrder()",start)
apps=[
    ("netflix","Netflix","N","D61F2C"),
    ("prime_video","Prime Video","PV","158DFF"),
    ("hbo_max","HBO Max","HM","7733FF"),
    ("disney_plus","Disney+","D+","164CBA"),
    ("apple_tv","Apple TV","AT","343A46"),
    ("youtube","YouTube","YT","FF3025"),
    ("iptv","IPTV","TV","33355F"),
]
out=["private val defaultRemoteShortcutApps = listOf("]
for i,(ident,label,mark,color) in enumerate(apps):
    out+=["    RemoteShortcutApp(","        id = \""+ident+"\",","        launchName = \""+label+"\",",
          "        label = \""+label+"\",","        mark = \""+mark+"\",",
          "        accent = Color(0xFF"+color+")","    )"+("," if i<len(apps)-1 else "")]
out.append(")")
s=s[:start]+"\n".join(out)+"\n"+s[end:]
p.write_text(s,encoding="utf-8")

vm="app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt"
change(vm, [
    ("    private val discoveredDevices = linkedMapOf<String, ConnectableDevice>()",
     "    private val discoveredDevices = linkedMapOf<String, ConnectableDevice>()\n"
     "    private val manualDeviceIds = mutableSetOf<String>()"),
    ("        refreshDiscoveredDevices()\n        if (isWifiConnected()) {",
     "        refreshDiscoveredDevices()\n"
     "        prefs.getString(Constant.HOST, null)?.let { host ->\n"
     "            if (prefs.getBoolean(Constant.PIN, false)) registerManualDevice(host)\n"
     "        }\n"
     "        if (isWifiConnected()) {"),
    ("            discoveredDevices.clear()\n            discoveryManager?.allDevices?.values?.forEach",
     "            val manualEntries = discoveredDevices.filterKeys { it in manualDeviceIds }\n"
     "            discoveredDevices.clear()\n"
     "            discoveredDevices.putAll(manualEntries)\n"
     "            discoveryManager?.allDevices?.values?.forEach"),
    ("    fun connectToDevice(deviceId: String) {\n",
     "    private fun registerManualDevice(host: String): ConnectableDevice {\n"
     "        val id = \"manual:$host\"\n"
     "        val device = ConnectableDevice(host, \"TCL (IP $host)\", \"Android TV\", \"TCL\")\n"
     "        device.id = id\n"
     "        synchronized(discoveredDevices) {\n"
     "            manualDeviceIds.add(id)\n"
     "            discoveredDevices[id] = device\n"
     "        }\n"
     "        publishDeviceSnapshot()\n"
     "        return device\n"
     "    }\n\n"
     "    fun connectToIp(raw: String) {\n"
     "        val host = raw.trim()\n"
     "        val octets = host.split('.')\n"
     "        if (octets.size != 4 || octets.any { it.toIntOrNull()?.let { n -> n in 0..255 } != true }) {\n"
     "            showStatus(\"Informe um IP valido, ex.: 192.168.1.50\", isError = true)\n"
     "            return\n"
     "        }\n"
     "        val device = registerManualDevice(host)\n"
     "        connectToDeviceInternal(device.id, userInitiated = true)\n"
     "    }\n\n"
     "    fun connectToDevice(deviceId: String) {\n"),
    ("        if (!device.isChromecastServiceDevice()) {\n            synchronized(discoveredDevices) { discoveredDevices.remove(deviceId) }\n",
     "        if (!device.isChromecastServiceDevice() && deviceId !in manualDeviceIds) {\n            synchronized(discoveredDevices) { discoveredDevices.remove(deviceId) }\n"),
    ("        device.addListener(listener)\n        device.connect()",
     "        device.addListener(listener)\n"
     "        if (device.id in manualDeviceIds) listener.onDeviceReady(device)\n"
     "        else device.connect()"),
    ("                .filter { it.isChromecastServiceDevice() }\n"
     "                .filter { device -> activeDeviceId == device.id || isDeviceReachable(device) }",
     "                .filter { it.isChromecastServiceDevice() || it.id in manualDeviceIds }\n"
     "                .filter { device -> device.id in manualDeviceIds || activeDeviceId == device.id || isDeviceReachable(device) }"),
    ("            supportsCast = true,\n",
     "            supportsCast = id !in manualDeviceIds,\n"),
    ('"Netflix"                      -> "https://www.netflix.com/title.*"',
     '"Netflix"                      -> "netflix://"')
])
nav="app/src/main/java/com/hari/androidtvremote/navigation/AppNavGraph.kt"
change(nav, [
    ("                onConnect = { deviceId ->\n",
     "                onConnectIp = { ip ->\n"
     "                    connectRequested = true\n"
     "                    tvRemoteViewModel.connectToIp(ip)\n"
     "                },\n"
     "                onConnect = { deviceId ->\n")
])
discovery="app/src/main/java/com/hari/androidtvremote/ui/app/DeviceDiscoveryScreen.kt"
change(discovery, [
    ("    onConnect: (String) -> Unit,\n",
     "    onConnect: (String) -> Unit,\n    onConnectIp: (String) -> Unit,\n"),
    ("    var disconnectPendingDevice by remember { mutableStateOf<DeviceUiModel?>(null) }",
     "    var disconnectPendingDevice by remember { mutableStateOf<DeviceUiModel?>(null) }\n"
     "    var manualIp by remember { mutableStateOf(\"\") }"),
    ("                // Section header\n",
     "                item {\n"
     "                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n"
     "                        Text(\"Conectar pelo endereco IP da TV\")\n"
     "                        androidx.compose.material3.OutlinedTextField(\n"
     "                            value = manualIp,\n"
     "                            onValueChange = { manualIp = it },\n"
     "                            modifier = Modifier.fillMaxWidth(),\n"
     "                            singleLine = true,\n"
     "                            label = { Text(\"IP: 192.168.1.50\") }\n"
     "                        )\n"
     "                        Button(onClick = { onConnectIp(manualIp) }, enabled = manualIp.isNotBlank()) {\n"
     "                            Text(\"Conectar pelo IP\")\n"
     "                        }\n"
     "                    }\n"
     "                }\n\n"
     "                // Section header\n")
])
print("TCL_PATCH_OK")


# TCL v1.1: permanently visible media bar on the remote home view.
from shutil import copyfile
copyfile("overrides/PlaybackTransportBar.kt", SRC / "ui/app/PlaybackTransportBar.kt")
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
    ("                    RemoteLayoutConfig.SECTION_DPAD_STAGE -> {\n                        Spacer(modifier = Modifier.height(6.dp))",
     "                    RemoteLayoutConfig.SECTION_DPAD_STAGE -> {\n                        PlaybackTransportBar(onRemoteKey = wrappedOnRemoteKey)\n                        Spacer(modifier = Modifier.height(6.dp))"),
    ("            if (hasTopStrip) maxHeight * 0.44f else maxHeight * 0.52f,\n            352.dp\n        ).coerceAtLeast(350.dp)",
     "            if (hasTopStrip) maxHeight * 0.34f else maxHeight * 0.43f,\n            295.dp\n        ).coerceAtLeast(240.dp)")
])
change("app/build.gradle.kts", [
    ("versionCode = 10", "versionCode = 11"),
    ('versionName = "1.0-TCL"', 'versionName = "1.1-TCL"'),
    ('applicationId = "com.tclremote.livre"', 'applicationId = "com.tclremote.livre.v11"')
])
change("app/src/main/res/values/strings.xml", [('Controle TCL Livre', 'Controle TCL 1.1')])
print("TCL_V1_1_PLAYBACK_PATCH_OK")


# TCL 1.2: square offline service logos and an IPTV launch shortcut.
from urllib.request import urlopen
asset_sha = "2ade7af6c86f0bf7de0b29cd62af99b3458b7af1"
resources = ROOT / "app/src/main/res/drawable-nodpi"
resources.mkdir(parents=True,exist_ok=True)
logos = {
    "netflix": "netflix",
    "prime_video": "prime-video",
    "max": "max",
    "disney_plus": "disney-plus",
    "apple_tv": "apple-tv-plus",
    "youtube": "youtube"
}
for dest, source in logos.items():
    url = f"https://raw.githubusercontent.com/homarr-labs/dashboard-icons/{asset_sha}/png/{source}.png"
    binary = urlopen(url, timeout=25).read()
    if not binary.startswith(b"\x89PNG\r\n\x1a\n") or len(binary)<1000:
        raise RuntimeError("Invalid brand icon asset: "+source)
    (resources/f"tcl_brand_{dest}.png").write_bytes(binary)

copyfile("overrides/AppShortcutTiles.kt", SRC/"ui/app/AppShortcutTiles.kt")
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt",[
    ("RemoteShelfMode.Applications -> AppShortcutStrip(",
     "RemoteShelfMode.Applications -> AppShortcutStripSquare("),
    ("            if (hasTopStrip) maxHeight * 0.34f else maxHeight * 0.43f,\n            295.dp\n        ).coerceAtLeast(240.dp)",
     "            if (hasTopStrip) maxHeight * 0.28f else maxHeight * 0.36f,\n            265.dp\n        ).coerceAtLeast(200.dp)")
])
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteCustomization.kt",[
    ("    return savedOrder + missingCustom",
     "    return savedOrder + defaultIds.filter { it !in savedOrder } + missingCustom")
])
change("app/build.gradle.kts",[
    ("versionCode = 11", "versionCode = 12"),
    ('versionName = "1.1-TCL"', 'versionName = "1.2-TCL"'),
    ('applicationId = "com.tclremote.livre.v11"', 'applicationId = "com.tclremote.livre"')
])
change("app/src/main/res/values/strings.xml",[
    ("Controle TCL 1.1","Controle TCL 1.2")
])
print("TCL_V1_2_IPTV_BRAND_EPISODE_PATCH_OK")

# TCL 1.3: compose opaque 256x256 tiles. Brand logos remain unchanged,
# but their backgrounds are baked into the PNG itself for high contrast.
from PIL import Image
brand_backgrounds = {
    "netflix": ("#151515", 0.74),
    "prime_video": ("#F0FAFF", 0.97),
    "max": ("#F3F0FF", 0.99),
    "disney_plus": ("#070F32", 0.92),
    "apple_tv": ("#FFFFFF", 0.94),
    "youtube": ("#FAFAFA", 0.76)
}
for brand, (background, fill) in brand_backgrounds.items():
    target = resources / f"tcl_brand_{brand}.png"
    original = Image.open(target).convert("RGBA")
    original.thumbnail((int(256 * fill), int(256 * fill)), Image.Resampling.LANCZOS)
    icon = Image.new("RGBA", (256, 256), background)
    icon.alpha_composite(original, ((256 - original.width) // 2, (256 - original.height) // 2))
    icon.convert("RGB").save(target, format="PNG", optimize=True)
    audit = Image.open(target)
    assert audit.mode == "RGB" and audit.size == (256, 256), brand

change("app/build.gradle.kts", [
    ("versionCode = 12", "versionCode = 13"),
    ('versionName = "1.2-TCL"', 'versionName = "1.3-TCL"')
])
change("app/src/main/res/values/strings.xml", [
    ("Controle TCL 1.2", "Controle TCL 1.3")
])
assert 'applicationId = "com.tclremote.livre"' in (ROOT/"app/build.gradle.kts").read_text()
print("TCL_V1_3_OPAQUE_ICONS_AND_EPISODES_OK")

# TCL 1.4: launch installed apps instead of browser URLs. Home Assistant
# Android TV Remote documents market://launch?id=<package>. Users can long
# press a logo and try the registered native link if their TV doesn't honor it.
change("app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt",[
    ('"Netflix"                      -> "netflix://"',
     '"Netflix"                      -> "market://launch?id=com.netflix.ninja"'),
    ('"YouTube"                      -> "https://www.youtube.com"',
     '"YouTube"                      -> "market://launch?id=com.google.android.youtube.tv"'),
    ('"Prime", "Prime Video"         -> "https://www.primevideo.com/"',
     '"Prime", "Prime Video"         -> "market://launch?id=com.amazon.amazonvideo.livingroom"'),
    ('"Disney+"                      -> "https://www.disneyplus.com/"',
     '"Disney+"                      -> "market://launch?id=com.disney.disneyplus"'),
    ('"Apple TV"                     -> "https://tv.apple.com/"',
     '"Apple TV"                     -> "market://launch?id=com.apple.atve.androidtv.appletv"'),
    ('"HBO Max", "Max"               -> "https://max.com/"',
     '"HBO Max", "Max"               -> "market://launch?id=com.wbd.stream"')
])
# Placeholder makes Android aapt include R.drawable.tcl_brand_iptv. Before
# release signing the placeholder is replaced with the cropped user screenshot.
(resources/"tcl_brand_iptv.png").write_bytes(
    (resources/"tcl_brand_prime_video.png").read_bytes()
)
change("app/build.gradle.kts", [
    ("versionCode = 13", "versionCode = 14"),
    ('versionName = "1.3-TCL"', 'versionName = "1.4-TCL"')
])
change("app/src/main/res/values/strings.xml", [
    ("Controle TCL 1.3", "Controle TCL 1.4")
])
print("TCL_V1_4_APP_LAUNCH_AND_IPTV_PICKER_OK")

# TCL 1.5: same package/certificate, 2/3-sized edge-to-edge icons,
# installed TV-app selection via optional on-TV accessibility companion.
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
    ("                                onQuickApp = wrappedOnQuickApp,\n                                shortcutSize = shelfButtonSize",
     "                                onQuickApp = wrappedOnQuickApp,\n"
     "                                onRemoteKey = wrappedOnRemoteKey,\n"
     "                                shortcutSize = shelfButtonSize")
])
change("app/build.gradle.kts", [
    ("versionCode = 14", "versionCode = 15"),
    ('versionName = "1.4-TCL"', 'versionName = "1.5-TCL"')
])
change("app/src/main/res/values/strings.xml", [
    ("Controle TCL 1.4", "Controle TCL 1.5")
])
assert 'applicationId = "com.tclremote.livre"' in (ROOT/"app/build.gradle.kts").read_text()
print("TCL_V1_5_TV_PICKER_BT_BUTTON_OK")

# RESCUE SWITCH: rebuild the previously usable 1.5 layout while retaining
# the Android app ID and upgrade-compatible signing certificate.
# No 8-digit code, no new LAN service, no new UI layout until validated.
import os
if os.environ.get("TCL_RESCUE_STABLE") == "1":
    # Stage 1: keep the known 1.5 controls, but make their measured
    # height accessible on short displays rather than compressing the deck.
    # With a scrollable Column, ColumnScope.weight is ignored; therefore
    # the control deck needs a finite height derived from its button grid.
    change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
        (
            "            .collectAsStateWithLifecycle(initialValue = RemoteLayoutConfig.default)\n\n        Column(",
            "            .collectAsStateWithLifecycle(initialValue = RemoteLayoutConfig.default)\n\n"
            "        val activeRockers = listOf(layoutConfig.leftRocker, layoutConfig.rightRocker)\n"
            "            .count { it != RemoteLayoutConfig.ROCKER_NONE }\n"
            "        val gridWidth = (maxWidth - horizontalPadding * 2\n"
            "            - rockerWidth * activeRockers - controlSpacing * activeRockers)\n"
            "            .coerceAtLeast(0.dp)\n"
            "        val gridCell = ((gridWidth - controlSpacing * 2) / 3).coerceAtLeast(0.dp)\n"
            "        val gridRows = (layoutConfig.gridButtons.size + 2) / 3\n"
            "        val gridHeight = gridCell * gridRows +\n"
            "            controlSpacing * (gridRows - 1).coerceAtLeast(0)\n"
            "        val deckHeight = maxOf(rockerHeight, gridHeight)\n\n"
            "        Column("
        ),
        (
            "                .padding(horizontal = horizontalPadding, vertical = 10.dp),\n"
            "            verticalArrangement = Arrangement.spacedBy(verticalGap)",
            "                .padding(horizontal = horizontalPadding, vertical = 10.dp)\n"
            "                .verticalScroll(rememberScrollState()),\n"
            "            verticalArrangement = Arrangement.spacedBy(verticalGap)"
        ),
        (
            "                        Box(modifier = Modifier.weight(1f)) {\n"
            "                            RemoteControlDeck(",
            "                        Box(modifier = Modifier.fillMaxWidth().height(deckHeight)) {\n"
            "                            RemoteControlDeck("
        )
    ])
    change("app/build.gradle.kts", [
        ("versionCode = 15", "versionCode = 18"),
        ('versionName = "1.5-TCL"', 'versionName = "1.8-Resgate-Teste"')
    ])
    change("app/src/main/res/values/strings.xml", [
        ("Controle TCL 1.5", "Controle TCL 1.8")
    ])
    if os.environ.get("TCL_APPROVED_UI") == "1":
        # The screenshot-approved screen must remain a separate install until
        # the private signer for the user's real 1.7 APK is recovered.
        if os.environ.get("TCL_PREVIEW_SIDE_BY_SIDE") != "1":
            raise RuntimeError("Approved UI build requires TCL_PREVIEW_SIDE_BY_SIDE=1; no incompatible updates.")
        copyfile("overrides/TclApprovedHomeScreen.kt",
                 SRC / "ui/app/TclApprovedHomeScreen.kt")
        copyfile("overrides/TclInstalledApps.kt",
                 SRC / "ui/app/TclInstalledApps.kt")
        # One-time 8-digit pairing enables LAN inventory requests to the
        # companion on the TV; subsequent connects fetch automatically.
        change("app/src/main/AndroidManifest.xml", [
            ('android:hardwareAccelerated="true"',
             'android:hardwareAccelerated="true"\n        android:usesCleartextTraffic="true"')
        ])
        change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
            ("private fun RemotePadStage(", "internal fun RemotePadStage("),
            ("private fun KeyboardDialog(", "internal fun KeyboardDialog(")
        ])
        change("app/src/main/java/com/hari/androidtvremote/navigation/AppNavGraph.kt", [
            ("import com.hari.androidtvremote.ui.app.HomeScreen",
             "import com.hari.androidtvremote.ui.app.TclApprovedHomeScreen"),
            ("            HomeScreen(\n", "            TclApprovedHomeScreen(\n"),
            ("                onCyclePadMode = {\n"
             "                    activePadMode = activePadMode.toggleNumberPad(defaultPadMode)\n"
             "                },\n",
             "                onCyclePadMode = {\n"
             "                    activePadMode = when (activePadMode) {\n"
             "                        RemotePadMode.DPad -> RemotePadMode.Touchpad\n"
             "                        RemotePadMode.Touchpad -> RemotePadMode.NumberPad\n"
             "                        RemotePadMode.NumberPad -> RemotePadMode.DPad\n"
             "                    }\n"
             "                },\n")
        ])
        change("app/build.gradle.kts", [
            ("versionCode = 18", "versionCode = 19"),
            ('versionName = "1.8-Resgate-Teste"', 'versionName = "1.9-Tela-Aprovada-Preview"'),
            ('applicationId = "com.tclremote.livre"',
             'applicationId = "com.tclremote.livre.preview"')
        ])
        change("app/src/main/res/values/strings.xml", [
            ("Controle TCL 1.8", "Controle TCL Visual")
        ])
        print("TCL_TELA_APROVADA_PREVIEW_SIDE_BY_SIDE_OK")
    screen = (SRC/"ui/app/RemoteScreen.kt").read_text()
    assert "AppShortcutStripSquare(" in screen
    assert "TclInstalledAppsBar" not in screen
    if os.environ.get("TCL_APPROVED_UI") == "1":
        assert (SRC/"ui/app/TclInstalledApps.kt").exists()
    else:
        assert not (SRC/"ui/app/TclInstalledApps.kt").exists()
    expected_app_id = ("com.tclremote.livre.preview" if os.environ.get("TCL_APPROVED_UI") == "1" else "com.tclremote.livre")
    assert 'applicationId = "' + expected_app_id + '"' in (ROOT/"app/build.gradle.kts").read_text()
    print("TCL_RESGATE_ESTAVEL_SEM_VINCULACAO_OK")
    raise SystemExit(0)

# TCL 1.6: dynamic official installed-app catalog with per-TV favorites.
from shutil import copyfile
copyfile("overrides/TclInstalledApps.kt", SRC / "ui/app/TclInstalledApps.kt")
change("app/src/main/AndroidManifest.xml", [
    ('android:hardwareAccelerated="true"',
     'android:hardwareAccelerated="true"\n        android:usesCleartextTraffic="true"')
])
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
    ("RemoteShelfMode.Applications -> AppShortcutStripSquare(\n"
     "                                quickApps = quickApps,\n"
     "                                onQuickApp = wrappedOnQuickApp,\n"
     "                                onRemoteKey = wrappedOnRemoteKey,\n"
     "                                shortcutSize = shelfButtonSize\n"
     "                            )",
     "RemoteShelfMode.Applications -> TclInstalledAppsBar(\n"
     "                                tvHost = sessionState.connectedDevice?.ipAddress,\n"
     "                                modifier = Modifier.fillMaxWidth()\n"
     "                            )")
])
# Unified playback card + D-pad flanked by volume and channel controls.
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
    ("                        PlaybackTransportBar(onRemoteKey = wrappedOnRemoteKey)\n"
     "                        Spacer(modifier = Modifier.height(6.dp))\n"
     "                        RemotePadStage(\n"
     "                            activePadMode = activePadMode,\n"
     "                            stageHeight = padStageHeight,\n"
     "                            isConnected = isConnected,\n"
     "                            hapticsEnabled = hapticsEnabled,\n"
     "                            onAction = onRemoteKey\n"
     "                        )",
     "                        Column(modifier = Modifier.fillMaxWidth()\n"
     "                            .clip(RoundedCornerShape(18.dp))\n"
     "                            .background(MaterialTheme.colorScheme.surfaceContainer)\n"
     "                            .padding(9.dp)) {\n"
     "                            Text(\"Reprodução\", fontSize = 13.sp,\n"
     "                                fontWeight = FontWeight.SemiBold,\n"
     "                                color = MaterialTheme.colorScheme.onSurface)\n"
     "                            PlaybackTransportBar(onRemoteKey = wrappedOnRemoteKey)\n"
     "                        }\n"
     "                        Spacer(modifier = Modifier.height(7.dp))\n"
     "                        Row(modifier = Modifier.fillMaxWidth().height(padStageHeight),\n"
     "                            horizontalArrangement = Arrangement.spacedBy(8.dp),\n"
     "                            verticalAlignment = Alignment.CenterVertically) {\n"
     "                            RemoteVerticalRocker(\n"
     "                                modifier = Modifier.width(52.dp).fillMaxHeight(),\n"
     "                                label = \"VOL\", fluidLevel = sessionState.volumeFraction,\n"
     "                                isMuted = sessionState.isMuted,\n"
     "                                topIcon = Icons.Filled.Add,\n"
     "                                bottomIcon = Icons.Filled.Remove,\n"
     "                                iconSize = actionIconSize,\n"
     "                                contentColor = MaterialTheme.colorScheme.onSurface,\n"
     "                                onTopClick = wrappedOnVolumeUp,\n"
     "                                onBottomClick = wrappedOnVolumeDown\n"
     "                            )\n"
     "                            Box(modifier = Modifier.weight(1f)) {\n"
     "                                RemotePadStage(\n"
     "                                    activePadMode = activePadMode,\n"
     "                                    stageHeight = padStageHeight,\n"
     "                                    isConnected = isConnected,\n"
     "                                    hapticsEnabled = hapticsEnabled,\n"
     "                                    onAction = onRemoteKey\n"
     "                                )\n"
     "                            }\n"
     "                            RemoteVerticalRocker(\n"
     "                                modifier = Modifier.width(52.dp).fillMaxHeight(),\n"
     "                                label = \"CH\",\n"
     "                                topIcon = Icons.Filled.KeyboardArrowUp,\n"
     "                                bottomIcon = Icons.Filled.KeyboardArrowDown,\n"
     "                                iconSize = actionIconSize,\n"
     "                                contentColor = MaterialTheme.colorScheme.onSurface,\n"
     "                                onTopClick = { wrappedOnRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_UP) },\n"
     "                                onBottomClick = { wrappedOnRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_DOWN) }\n"
     "                            )\n"
     "                        }")
])
change("app/src/main/java/com/hari/androidtvremote/ui/app/RemoteScreen.kt", [
    ("    onPower: () -> Unit,\n    layoutConfig: RemoteLayoutConfig\n) {",
     "    onPower: () -> Unit,\n    layoutConfig: RemoteLayoutConfig,\n    showRockers: Boolean = true\n) {"),
    ("        when (layoutConfig.leftRocker) {",
     "        if (showRockers) when (layoutConfig.leftRocker) {"),
    ("        when (layoutConfig.rightRocker) {",
     "        if (showRockers) when (layoutConfig.rightRocker) {"),
    ("                                layoutConfig = layoutConfig\n                            )",
     "                                layoutConfig = layoutConfig,\n                                showRockers = false\n                            )")
])
change("app/build.gradle.kts", [
    ("versionCode = 15", "versionCode = 16"),
    ('versionName = "1.5-TCL"', 'versionName = "1.6-TCL"')
])
change("app/src/main/res/values/strings.xml", [
    ("Controle TCL 1.5", "Controle TCL 1.6")
])
print("TCL_V1_6_OFFICIAL_INSTALLED_APPS_FAVORITES_OK")
