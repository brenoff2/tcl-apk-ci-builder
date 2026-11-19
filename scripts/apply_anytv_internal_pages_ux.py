#!/usr/bin/env python3
"""Align native TV discovery and Settings interactions with AnyTV graphite/red.

Runs after existing patches. Changes only visual attributes; no discovery,
pairing, disconnect, updater, donation, or account API is replaced.
"""
from pathlib import Path

UI = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")


def replace_exact(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, got {count}")
    return source.replace(old, new, 1)


discovery = UI / "DeviceDiscoveryScreen.kt"
source = discovery.read_text(encoding="utf-8")
source = replace_exact(
    source,
    "device.isConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)",
    "device.isConnected -> Color(0xFFE9003B)",
    "connected device border",
)
source = replace_exact(
    source,
    "        color = MaterialTheme.colorScheme.surface,\n"
    "        tonalElevation = if (device.isConnected) 2.dp else 0.dp",
    "        color = Color(0xFF292929),\n"
    "        tonalElevation = 0.dp",
    "flat graphite device surface",
)
source = replace_exact(
    source,
    "                        FilledTonalButton(\n"
    "                            onClick = onConnect,",
    "                        Button(\n"
    "                            onClick = onConnect,\n"
    "                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(\n"
    "                                containerColor = Color(0xFFE9003B),\n"
    "                                contentColor = Color.White),",
    "red connect action",
)
source = replace_exact(
    source,
    "                            modifier = Modifier.height(34.dp)\n"
    "                        ) {\n"
    "                            Text(\n"
    '                                if (device.isPaired) "Reconnect" else "Connect",',
    "                            modifier = Modifier.height(44.dp)\n"
    "                        ) {\n"
    "                            Text(\n"
    '                                if (device.isPaired) "Reconnect" else "Connect",',
    "larger connect action target",
)
source = replace_exact(
    source,
    "                            modifier = Modifier.height(34.dp)\n"
    "                        ) {\n"
    "                            Icon(\n"
    "                                Icons.Filled.LinkOff,",
    "                            modifier = Modifier.height(44.dp)\n"
    "                        ) {\n"
    "                            Icon(\n"
    "                                Icons.Filled.LinkOff,",
    "larger disconnect action target",
)
discovery.write_text(source, encoding="utf-8")

settings = UI / "SettingsScreen.kt"
source = settings.read_text(encoding="utf-8")
source = replace_exact(
    source,
    "                                onCheckedChange = onAutoReconnectChange\n"
    "                            )",
    "                                onCheckedChange = onAutoReconnectChange,\n"
    "                                colors = androidx.compose.material3.SwitchDefaults.colors(\n"
    "                                    checkedTrackColor = Color(0xFFE9003B),\n"
    "                                    checkedThumbColor = Color.White)\n"
    "                            )",
    "red active reconnect setting",
)
settings.write_text(source, encoding="utf-8")
print("ANYTV_NATIVE_TV_AND_SETTINGS_UX_CONTRACT_APPLIED")
