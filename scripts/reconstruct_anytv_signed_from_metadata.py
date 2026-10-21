#!/usr/bin/env python3
"""Reconstruct an externally signed APK from verified public metadata only.
No keystore, password or signing authority is available on the CI runner.
"""
import base64
import hashlib
import json
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

source, spec_path, destination = map(Path, sys.argv[1:4])
spec = json.loads(spec_path.read_text())
original = source.read_bytes()
digest = lambda b: hashlib.sha256(b).hexdigest()
if digest(original) != spec["source_sha256"]:
    raise SystemExit("Unsigned APK SHA-256 mismatch")
with zipfile.ZipFile(source) as archive:
    entries = archive.infolist()
    lengths = spec["lengths"]
    if len(lengths) != len(entries) + 1:
        raise SystemExit("ZIP metadata segment count mismatch")
    metadata = bytearray()
    payloads = []
    last = 0
    for entry in entries:
        offset = entry.header_offset
        if original[offset:offset + 4] != bytes([0x50,0x4b,0x03,0x04]):
            raise SystemExit("Unexpected ZIP local header")
        filename_length, extra_length = struct.unpack_from("<HH", original, offset + 26)
        start = offset + 30 + filename_length + extra_length
        metadata.extend(original[last:start])
        payloads.append((start, entry.compress_size))
        last = start + entry.compress_size
    metadata.extend(original[last:])
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        unsigned_meta = root / "unsigned.meta"
        patch = root / "signed.patch.zst"
        signed_meta = root / "signed.meta"
        unsigned_meta.write_bytes(metadata)
        patch.write_bytes(base64.b64decode(spec["patch"], validate=True))
        subprocess.run(["zstd", "-d", "-q", "-f", "--patch-from=" + str(unsigned_meta),
                        str(patch), "-o", str(signed_meta)], check=True)
        signed_metadata = signed_meta.read_bytes()
    output = bytearray()
    cursor = 0
    for n, (offset, count) in zip(lengths, payloads):
        if n < 0:
            raise SystemExit("Negative metadata length")
        output.extend(signed_metadata[cursor:cursor + n])
        cursor += n
        output.extend(original[offset:offset + count])
    output.extend(signed_metadata[cursor:cursor + lengths[-1]])
    cursor += lengths[-1]
    if cursor != len(signed_metadata) or digest(output) != spec["target_sha256"]:
        raise SystemExit("Reconstructed signed APK SHA-256 mismatch")
    destination.write_bytes(output)
print("SIGNED_APK_BINARY_RECONSTRUCTION_VERIFIED")
