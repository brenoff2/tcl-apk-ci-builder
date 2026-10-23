#!/usr/bin/env python3
"""Restore official Android TV auto-reconnect after an involuntary device loss.

The upstream disconnectCurrentDevice() means *manual* disconnect. When mDNS
reports device removal, persisting that flag prevents subsequent reconnect.
Only modify this exact onDeviceRemoved path; explicit user disconnect stays manual.
"""
from pathlib import Path

p = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt")
s = p.read_text(encoding="utf-8")
old = """    override fun onDeviceRemoved(manager: DiscoveryManager?, device: ConnectableDevice?) {
        device ?: return
        synchronized(discoveredDevices) { discoveredDevices.remove(device.id) }
        device.ipAddress?.let { reachableHosts.remove(it) }
        if (activeDeviceId == device.id) {
            disconnectCurrentDevice()
        } else {
            publishDeviceSnapshot()
        }
    }"""
new = old.replace(
    "disconnectCurrentDevice()",
    "// Involuntary network/discovery loss must NEVER store PREF_MANUAL_DISCONNECT.\n"
    "            disconnectCurrentDeviceInternal(manual = false)"
)
assert s.count(old) == 1, "Auto-reconnect upstream anchor changed: refuse silent patch"
p.write_text(s.replace(old, new), encoding="utf-8")
assert "disconnectCurrentDeviceInternal(manual = false)" in p.read_text()
print("ANYTV_AUTO_TV_RECONNECT_AFTER_DISCOVERY_LOSS_READY")
