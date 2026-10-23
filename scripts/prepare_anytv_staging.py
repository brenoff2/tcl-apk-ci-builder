#!/usr/bin/env python3
"""Opt-in isolated AnyTV Android resolver endpoint for local/staging builds only."""
import argparse
from pathlib import Path
from urllib.parse import urlsplit

DEFAULT = "https://anytv-link-resolver-pilot.brenoff.workers.dev"
SOURCE = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvDeviceIdentity.kt")
OLD = f'RESOLVER_BASE = "{DEFAULT}"'


def validate(value: str) -> str:
    u = urlsplit(value)
    if (u.scheme != "https" or u.username or u.password or u.port or
            u.query or u.fragment or u.path not in ("", "/") or
            not u.hostname or u.hostname == "anytv-link-resolver-pilot.brenoff.workers.dev" or
            not u.hostname.endswith(".workers.dev") or
            not u.hostname.startswith("anytv-p0-staging-") or
            len(value) > 180):
        raise ValueError("Only a dedicated HTTPS anytv-p0-staging-*.workers.dev endpoint is allowed")
    return f"https://{u.hostname}"


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--staging-url", required=True)
    args = p.parse_args()
    base = validate(args.staging_url)
    original = SOURCE.read_text(encoding="utf-8")
    if original.count(OLD) != 1:
        raise RuntimeError("Expected one untouched production resolver constant")
    SOURCE.write_text(original.replace(OLD, f'RESOLVER_BASE = "{base}"'), encoding="utf-8")
    print("AnyTV isolated staging endpoint applied to generated Android source")


if __name__ == "__main__":
    main()
