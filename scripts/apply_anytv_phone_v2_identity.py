#!/usr/bin/env python3
"""Repackage a fully prepared AnyTV phone as side-by-side V2 with GitHub-only V2 OTA.

Run only AFTER the established preparation/regression steps, never alter companion TV.
"""
from __future__ import annotations
import json
from pathlib import Path

PACKAGE_V2 = "com.tclremote.livre.v2"
OLD_CERT = "2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f"
base = Path("source/app")
meta = json.loads(Path("signing-recovery/v2/identity.json").read_text(encoding="utf-8"))
cert = meta["certificate_sha256"]
assert len(cert) == 64 and all(c in "0123456789abcdef" for c in cert)
assert cert != OLD_CERT, "A new signer identity is mandatory"

def replace_exact(path: Path, old: str, new: str) -> None:
    s = path.read_text(encoding="utf-8")
    assert s.count(old) == 1, (str(path), old, s.count(old))
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

replace_exact(base / "build.gradle.kts",
              'applicationId = "com.tclremote.livre"',
              'applicationId = "' + PACKAGE_V2 + '"')
shared = base / "src/main/java/tcl/shared/TclUpdateManager.java"
replace_exact(shared,
              'private static final String PHONE_PACKAGE = "com.tclremote.livre";',
              'private static final String PHONE_PACKAGE = "' + PACKAGE_V2 + '";')
replace_exact(shared, OLD_CERT, cert)
replace_exact(shared, 'suffix = "android/cert2026";', 'suffix = "android/v2";')

# Do not permit the new package to use the unrelated legacy update channel.
replace_exact(shared,
              '} else if (PHONE_CERT_LEGACY.equals(signer)) {\n                        suffix = "android";\n',
              '')
# The approved launcher display name remains AnyTV (package and signer isolate V2).
assert '<string name="app_name">AnyTV</string>' in (
    base / "src/main/res/values/strings.xml"
).read_text(encoding="utf-8")
assert PACKAGE_V2 in (base / "build.gradle.kts").read_text()
assert cert in shared.read_text()
assert "android/v2" in shared.read_text()
print("ANYTV_PHONE_V2_NEW_PACKAGE_AND_SIGNER_UPDATER_BINDING_READY")
