#!/usr/bin/env python3
"""Preserve Android TV TLS validation, but recover stale saved pairings.

The TV can reject a previously paired phone certificate.  In that case the
reconnect callback must enter the EXISTING PIN pairing flow, not dead-end in a
banner saying to re-pair with no way to do so.
"""
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote")
REMOTE = ROOT / "androidLib/AndroidRemoteTv.java"
VIEW_MODEL = ROOT / "ui/app/TvRemoteViewModel.kt"


def replace_exact(path: Path, old: str, new: str) -> None:
    content = path.read_text(encoding="utf-8")
    if content.count(old) != 1:
        raise RuntimeError(f"Expected exactly one original pairing anchor in {path}")
    path.write_text(content.replace(old, new), encoding="utf-8")


def main() -> None:
    # Never bypass SSL, rotate the AndroidKeyStore identity or clear app data.
    # Initial (fresh) pairing must report TLS errors instead of silently hanging.
    replace_exact(
        REMOTE,
        "            public void onSslError() {\n\n            }\n\n            @Override\n            public void onDisconnected() {",
        "            public void onSslError() {\n                notifyOnError(\"Secure pairing handshake failed. Please retry.\");\n            }\n\n            @Override\n            public void onDisconnected() {",
    )
    # Reconnect SSL rejection is the missing trigger for the existing
    # ViewModel onSecretRequested() -> connectRemote() -> TV PIN dialog path.
    replace_exact(
        REMOTE,
        'androidTvListener.onError("SSL certificate error. Re-pair the device.");',
        "androidTvListener.onSecretRequested();",
    )
    # During automatic reconnect, stop the watchdog before waiting for the
    # human to enter the six-digit code on the TV (not bounded by 20 seconds).
    replace_exact(
        VIEW_MODEL,
        "                    override fun onSecretRequested() {\n\n                        if (!isActiveConnection(connectionToken)) return\n                        prefs.edit {",
        "                    override fun onSecretRequested() {\n                        if (!isActiveConnection(connectionToken)) return\n                        connectionWatchdogJob?.cancel()\n                        connectionWatchdogJob = null\n                        autoReconnectInFlight.set(false)\n                        prefs.edit {",
    )
    replace_exact(
        VIEW_MODEL,
        "                        if (allowFreshFallback) {\n                            connectRemote(",
        "                        if (allowFreshFallback) {\n                            _navigateToDiscoveryTrigger.value = true\n                            connectRemote(",
    )
    replace_exact(
        VIEW_MODEL,
        'onSecretFallbackMessage = "Pairing code needed. Enter it to finish reconnecting."',
        'onSecretFallbackMessage = "A TV precisa de novo pareamento. Digite o PIN exibido nela."',
    )
    print("ANYTV_SECURE_SSL_REPAIR_FALLBACK_INSTALLED")


if __name__ == "__main__":
    main()
