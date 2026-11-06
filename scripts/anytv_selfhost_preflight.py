#!/usr/bin/env python3
"""Read-only preflight for the isolated AnyTV Google OAuth self-hosted instance.

Reads local configuration only to return boolean readiness flags.
Never outputs or exports keys, passwords, OAuth secrets, or token values.
No network requests, Docker changes, CLFCJ/n8n mutations, or releases.
"""
from __future__ import annotations
import json
import shutil
import socket
from pathlib import Path
from urllib.parse import urlparse

STACK = Path("/opt/clfcj-supabase-e13-shadow")
DEST = Path("/home/ubuntu/automacao/anytv-supabase/instance")
PORTS = (18680, 18632, 18643)
AUTH_REDIRECT = "com.tclremote.livre.v2://auth"
GOOGLE_VARS = (
    "GOTRUE_EXTERNAL_GOOGLE_ENABLED",
    "GOTRUE_EXTERNAL_GOOGLE_CLIENT_ID",
    "GOTRUE_EXTERNAL_GOOGLE_SECRET",
    "GOTRUE_EXTERNAL_GOOGLE_REDIRECT_URI",
)


def port_free(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        return sock.connect_ex(("127.0.0.1", port)) != 0


def google_readiness() -> dict:
    env = DEST / ".env"
    compose = DEST / "docker-compose.anytv.yml"
    # Read only the necessary values. Never include their contents in results.
    vals = {}
    if env.is_file():
        for line in env.read_text(encoding="utf-8").splitlines():
            if line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            if key.strip() in (
                "GOOGLE_ENABLED", "GOOGLE_CLIENT_ID", "GOOGLE_SECRET",
                "API_EXTERNAL_URL", "SITE_URL", "ADDITIONAL_REDIRECT_URLS",
            ):
                vals[key.strip()] = value.strip().strip('"').strip("'")
    url = urlparse(vals.get("API_EXTERNAL_URL", ""))
    https = (
        url.scheme == "https" and bool(url.hostname)
        and url.hostname not in {"localhost", "127.0.0.1", "::1"}
        and url.path.rstrip("/") == "/auth/v1"
    )
    site_url = urlparse(vals.get("SITE_URL", ""))
    redirect_uris = vals.get("ADDITIONAL_REDIRECT_URLS", "").split(",")
    if compose.is_file():
        lines = compose.read_text(encoding="utf-8").splitlines()
        active = {
            line.split(":", 1)[0].strip()
            for line in lines if line.lstrip().startswith(GOOGLE_VARS)
        }
    else:
        active = set()
    report = {
        "isolated_compose_present": compose.is_file(),
        "isolated_env_present": env.is_file(),
        "google_provider_enabled": vals.get("GOOGLE_ENABLED") == "true",
        "google_web_client_present": vals.get("GOOGLE_CLIENT_ID", "").endswith(".apps.googleusercontent.com"),
        "google_secret_present": bool(vals.get("GOOGLE_SECRET")),
        "public_auth_https": https,
        "site_https": site_url.scheme == "https" and bool(site_url.hostname),
        "android_redirect_allowed": AUTH_REDIRECT in [uri.strip() for uri in redirect_uris],
        "google_compose_passthrough": set(GOOGLE_VARS).issubset(active),
    }
    report["config_ready"] = all(report.values())
    return report


def inspect() -> dict:
    return {
        "existing_clfcj_stack_present": (STACK / "docker-compose.yml").is_file(),
        "isolated_anytv_stack_present": (DEST / "docker-compose.anytv.yml").is_file(),
        "docker_available": shutil.which("docker") is not None,
        "ports_free_before_first_start": {str(p): port_free(p) for p in PORTS},
        "google_oauth": google_readiness(),
        "safe_to_modify_existing_clfcj_stack": False,
        # Only a real HTTPS provider check + Android login can establish this.
        "google_login_end_to_end_verified": False,
    }


if __name__ == "__main__":
    print(json.dumps(inspect(), indent=2, ensure_ascii=False))
