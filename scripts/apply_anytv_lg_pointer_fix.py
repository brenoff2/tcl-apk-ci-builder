#!/usr/bin/env python3
"""LG webOS pointer/socket repair on top of the existing, pinned code93 LG patch.

Changes only webOS paths. Fail closed on all upstream/override anchor drift.
"""
from pathlib import Path

def replace_exact(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    assert text.count(old) == 1, f"LG_POINTER_ANCHOR_DRIFT {path} {text.count(old)} {old[:80]!r}"
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

vm = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt")
replace_exact(
    vm,
    '        // Pairing confirmation is shown by the LG itself, not the Android PIN modal.\n'
    '        DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.PROTECTED\n'
    '        service.setPairingType(DeviceService.PairingType.FIRST_SCREEN)',
    '        // PointerInputSocket needs CONTROL_MOUSE_AND_KEYBOARD (PersonalActivity),\n'
    '        // not just joystick or media playback rights.\n'
    '        DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.ON\n'
    '        service.setPairingType(DeviceService.PairingType.FIRST_SCREEN)\n'
    '        val pointerScopeKey = "lg_webos_pointer_scope_v1_" + device.id\n'
    '        val currentPermissions = service.getPermissions()\n'
    '        val required = listOf("CONTROL_MOUSE_AND_KEYBOARD", "CONTROL_INPUT_JOYSTICK",\n'
    '            "CONTROL_INPUT_TV", "READ_INPUT_DEVICE_LIST")\n'
    '        val targetPermissions = (currentPermissions + required).distinct()\n'
    '        // Migration of existing code93/code94 pairs: force one new LG approval.\n'
    '        // Do not reset the client key on every subsequent connection.\n'
    '        if (!prefs.getBoolean(pointerScopeKey, false) || targetPermissions != currentPermissions) {\n'
    '            service.setPermissions(targetPermissions)\n'
    '            prefs.edit { putBoolean(pointerScopeKey, true) }\n'
    '        }'
)
replace_exact(
    vm,
    '        val keys = device.getCapability(KeyControl::class.java)\n'
    '        val volume = device.getCapability(VolumeControl::class.java)\n'
    '        val channels = device.getCapability(TVControl::class.java)\n'
    '        val media = device.getCapability(MediaControl::class.java)',
    '        // Never choose a competing Cast/other service for LG key control.\n'
    '        val webOs = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return\n'
    '        val keys = webOs.getKeyControl()\n'
    '        val volume = webOs.getVolumeControl()\n'
    '        val channels = webOs.getTVControl()\n'
    '        val media = device.getCapability(MediaControl::class.java)'
)
replace_exact(
    vm,
    '                if (!isActiveConnection(token) || !service.isConnected) return\n'
    '                Constant.connectableDevice = connectableDevice\n'
    '                Constant.isConnected.value = true',
    '                if (!isActiveConnection(token) || !service.isConnected) return\n'
    '                // Pointer uses a separate LG WebSocket; prepare it after pairing.\n'
    '                service.connectMouse()\n'
    '                Constant.connectableDevice = connectableDevice\n'
    '                Constant.isConnected.value = true'
)
replace_exact(
    vm,
    '    private fun sendLgWebOsKey(key: Remotemessage.RemoteKeyCode) {',
    '''    fun sendLgWebOsPointerMove(dx: Float, dy: Float) {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId } ?: return
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return
        if (service.isConnected) service.getMouseControl().move(dx.toDouble(), dy.toDouble())
    }

    fun sendLgWebOsPointerClick() {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId } ?: return
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return
        if (service.isConnected) service.getMouseControl().click()
    }

    private fun sendLgWebOsKey(key: Remotemessage.RemoteKeyCode) {'''
)
nav = Path("source/app/src/main/java/com/hari/androidtvremote/navigation/AppNavGraph.kt")
replace_exact(
    nav,
    '                onKeyboardEnter = tvRemoteViewModel::sendKeyboardEnter,',
    '                onKeyboardEnter = tvRemoteViewModel::sendKeyboardEnter,\n'
    '                onLgPointerMove = tvRemoteViewModel::sendLgWebOsPointerMove,\n'
    '                onLgPointerClick = tvRemoteViewModel::sendLgWebOsPointerClick,'
)
home = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclApprovedHomeScreen.kt")
replace_exact(
    home,
    '    onKeyDirection: (Remotemessage.RemoteKeyCode, Remotemessage.RemoteDirection) -> Unit,',
    '    onKeyDirection: (Remotemessage.RemoteKeyCode, Remotemessage.RemoteDirection) -> Unit,\n'
    '    onLgPointerMove: (Float, Float) -> Unit,\n'
    '    onLgPointerClick: () -> Unit,'
)
replace_exact(
    home,
    '                                    onShort = { remoteKey(it) },\n'
    '                                    onHoldStart = {',
    '                                    onShort = { remoteKey(it) },\n'
    '                                    isLgWebOs = sessionState.connectedDevice?.type == "LG webOS",\n'
    '                                    onLgPointerMove = onLgPointerMove,\n'
    '                                    onLgPointerClick = onLgPointerClick,\n'
    '                                    onHoldStart = {'
)
service = Path("source/ConnectSDK/src/main/java/com/connectsdk/service/WebOSTVService.java")
replace_exact(
    service,
    '''    private void sendSpecialKey(final String key, final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.button(key);
            Util.postSuccess(listener, null);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.button(key);
                    Util.postSuccess(listener, null);
                }
            });
        }
    }''',
    '''    private void sendSpecialKey(final String key, final ResponseListener<Object> listener) {
        if (mouseSocket != null && mouseSocket.isConnected()) {
            mouseSocket.button(key);
            Util.postSuccess(listener, null);
        } else {
            // A non-null, disconnected pointer socket silently drops ALL navigation.
            disconnectMouse();
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    if (mouseSocket != null && mouseSocket.isConnected()) {
                        mouseSocket.button(key);
                        Util.postSuccess(listener, null);
                    }
                }
            });
        }
    }'''
)
replace_exact(
    service,
    '''    public void ok(final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.click();
            Util.postSuccess(listener, null);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.click();
                    Util.postSuccess(listener, null);
                }
            });
        }
    }''',
    '''    public void ok(final ResponseListener<Object> listener) {
        if (mouseSocket != null && mouseSocket.isConnected()) {
            mouseSocket.click();
            Util.postSuccess(listener, null);
        } else {
            disconnectMouse();
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    if (mouseSocket != null && mouseSocket.isConnected()) {
                        mouseSocket.click();
                        Util.postSuccess(listener, null);
                    }
                }
            });
        }
    }'''
)
replace_exact(
    service,
    '''    public void move(final double dx, final double dy) {
        if (mouseSocket != null) {
            mouseSocket.move(dx, dy);
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.move(dx, dy);
                }
            });
        }
    }''',
    '''    public void move(final double dx, final double dy) {
        if (mouseSocket != null && mouseSocket.isConnected()) {
            mouseSocket.move(dx, dy);
        } else {
            disconnectMouse();
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    if (mouseSocket != null && mouseSocket.isConnected()) {
                        mouseSocket.move(dx, dy);
                    }
                }
            });
        }
    }'''
)
replace_exact(
    service,
    '''    public void click() {
        if (mouseSocket != null) {
            mouseSocket.click();
        }
        else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    mouseSocket.click();
                }
            });
        }
    }''',
    '''    public void click() {
        if (mouseSocket != null && mouseSocket.isConnected()) {
            mouseSocket.click();
        } else {
            disconnectMouse();
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() {
                @Override
                public void onConnected() {
                    if (mouseSocket != null && mouseSocket.isConnected()) {
                        mouseSocket.click();
                    }
                }
            });
        }
    }'''
)
print("LG_WEBOS_POINTER_REPAIR_APPLIED; TCL_PATH_UNCHANGED")
