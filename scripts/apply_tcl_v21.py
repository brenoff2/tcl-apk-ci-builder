#!/usr/bin/env python3
"""Surgical v2.1 patch, applied AFTER prepare_tcl.py in single-app mode.

- Keep the correct original applicationId and every existing companion feature.
- Larger D-pad, 4-row keypad, real START_LONG/END_LONG on the Android TV link.
- IME batch edit as documented in tronikos/androidtvremote2 (no double injection).
- Ordered live typing, paste and backspace actions.
"""
from pathlib import Path
from shutil import copyfile

ROOT = Path("source/app")
SRC = ROOT / "src/main/java/com/hari/androidtvremote"
UI = SRC / "ui/app"
REMOTE = SRC / "androidLib/remote"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text()
    hits = text.count(old)
    if hits != 1:
        raise RuntimeError(f"v2.1 patch requires one occurrence; found {hits} in {path}: {old[:90]!r}")
    path.write_text(text.replace(old, new, 1))


copyfile("overrides/TclResponsivePad.kt", UI / "TclResponsivePad.kt")

viewmodel = UI / "TvRemoteViewModel.kt"
replace_once(viewmodel, "import kotlinx.coroutines.launch\n",
             "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.channels.Channel\n")

replace_once(viewmodel,
    "    private var imeCounter = 0\n",
    """    private var imeCounter = 0

    // Serialized socket writes: typing, deletions and long-key sequences must
    // keep their gesture order even though Android dispatches them on IO.
    private val keyAndTextWrites = Channel<() -> Unit>(Channel.UNLIMITED)
""")

replace_once(viewmodel,
    "    init {\n        discoveryManager?.addListener(this)\n",
    """    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (write in keyAndTextWrites) {
                try {
                    write()
                } catch (error: Exception) {
                    Timber.e(error, "[RemoteInput] Failed to write queued TV command")
                }
            }
        }
        discoveryManager?.addListener(this)
""")

replace_once(viewmodel,
    "    fun sendText(text: String, submit: Boolean) {\n",
    """    fun sendHeldKey(
        keyCode: Remotemessage.RemoteKeyCode,
        direction: Remotemessage.RemoteDirection
    ) {
        if (!checkConnectionGuard()) return
        // Only the two protocol hold states are valid for a held key.
        if (direction != Remotemessage.RemoteDirection.START_LONG &&
            direction != Remotemessage.RemoteDirection.END_LONG) return
        keyAndTextWrites.trySend {
            app.androidRemoteTv?.sendCommand(keyCode, direction)
        }
    }

    fun sendText(text: String, submit: Boolean) {
""")

replace_once(viewmodel,
    """    fun sendText(text: String, submit: Boolean) {
        if (!checkConnectionGuard()) return
        if (text.isBlank() && !submit) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendImeTextUpdate(text)
                if (submit) {
                    app.androidRemoteTv?.sendImeEnter()
                }
            } catch (error: Exception) {
                Timber.e(error, "[Keyboard] Failed to send text")
            }
        }
    }

    fun sendKeyboardText(text: String) {
        if (!checkConnectionGuard()) return
        if (text.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remote = app.androidRemoteTv ?: return@launch
                val currentFieldCounter = resolvedImeFieldCounter()
                text.forEach { character ->
                    remote.sendText(character.toString(), imeCounter++, currentFieldCounter)
                }
                Timber.d("[Keyboard] Sent 0 character(s), fieldCounter=$currentFieldCounter")
            } catch (error: Exception) {
                Timber.e(error, "[Keyboard] Failed to send keyboard text")
            }
        }
    }

    fun sendKeyboardBackspace(count: Int = 1) {
        if (!checkConnectionGuard()) return
        if (count <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            repeat(count) {
                app.androidRemoteTv?.sendCommand(
                    Remotemessage.RemoteKeyCode.KEYCODE_DEL,
                    Remotemessage.RemoteDirection.SHORT
                )
            }
        }
    }
""",
    """    fun sendText(text: String, submit: Boolean) {
        if (!checkConnectionGuard()) return
        if (text.isEmpty() && !submit) return
        keyAndTextWrites.trySend {
            sendImeTextUpdate(text)
            if (submit) app.androidRemoteTv?.sendImeEnter()
        }
    }

    fun sendKeyboardText(text: String) {
        if (!checkConnectionGuard()) return
        if (text.isEmpty()) return
        // Paste and live-keyboard diffs use a single ordered IME batch per delta.
        // Repeatedly resending the whole accumulated string duplicates characters.
        keyAndTextWrites.trySend {
            app.androidRemoteTv?.sendText(
                text, ++imeCounter, resolvedImeFieldCounter()
            )
        }
    }

    fun sendKeyboardBackspace(count: Int = 1) {
        if (!checkConnectionGuard() || count <= 0) return
        keyAndTextWrites.trySend {
            repeat(count.coerceAtMost(4096)) {
                app.androidRemoteTv?.sendCommand(
                    Remotemessage.RemoteKeyCode.KEYCODE_DEL,
                    Remotemessage.RemoteDirection.SHORT
                )
            }
        }
    }
""")

session = REMOTE / "RemoteSession.java"
replace_once(session,
    """        byte[] part1 = mMessageManager.createImeText(
                text, resolvedIme, resolvedField, syncedTextInt5, syncedTextLabel, syncedAppInfo, false);
        byte[] part2 = mMessageManager.createImeBatchEditCompatV2(text, resolvedIme, resolvedField);
        byte[] combined = new byte[part1.length + part2.length];
        System.arraycopy(part1, 0, combined, 0, part1.length);
        System.arraycopy(part2, 0, combined, part1.length, part2.length);
""",
    """        // Android TV Remote v2 IME: one RemoteImeBatchEdit, as in the
        // maintained tronikos/androidtvremote2 implementation. Sending a
        // second RemoteImeKeyInject for the same input confuses some TVs.
        byte[] combined = mMessageManager.createImeBatchEditCompatV2(
                text, resolvedIme, resolvedField);
""")

message = REMOTE / "RemoteMessageManager.java"
replace_once(message,
    """        final String safeText = text == null ? "" : text;
        int cursor = safeText.length();

        try {""",
    """        final String safeText = text == null ? "" : text;
        // Android TV Remote v2 expects the last-character index, not length.
        // Zero is correct for a one-character insert.
        int cursor = Math.max(0, safeText.length() - 1);

        try {""")

nav = SRC / "navigation/AppNavGraph.kt"
replace_once(nav,
    "                onRemoteKey = tvRemoteViewModel::sendKey,\n",
    "                onRemoteKey = tvRemoteViewModel::sendKey,\n"
    "                onKeyDirection = tvRemoteViewModel::sendHeldKey,\n")

gradle = ROOT / "build.gradle.kts"
replace_once(gradle, "versionCode = 20", "versionCode = 21")
replace_once(gradle, 'versionName = "2.0-TCL"', 'versionName = "2.1-TCL"')
assert 'applicationId = "com.tclremote.livre"' in gradle.read_text()
assert 'applicationId = "com.tclremote.livre.preview"' not in gradle.read_text()
assert "RemoteDirection.START_LONG" in viewmodel.read_text()
assert "RemoteDirection.END_LONG" in viewmodel.read_text()
assert "byte[] combined = mMessageManager.createImeBatchEditCompatV2(" in session.read_text()
assert "TclResponsivePad.kt" in {p.name for p in UI.glob("*.kt")}
print("TCL_V21_PAD_IME_HOLD_PATCH_OK")
