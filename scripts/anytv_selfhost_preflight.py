#!/usr/bin/env python3
"""Read-only readiness check for an isolated AnyTV self-hosted Supabase deployment.

Does not read or print secrets. No mutations or network requests.
Run on the n8n host to verify prerequisites without touching CLFCJ containers.
"""
from __future__ import annotations
import json
import os
import shutil
import socket
from pathlib import Path

STACK = Path("/opt/clfcj-supabase-e13-shadow")
DEST = Path("/opt/anytv-auth")
PORTS = (15433, 18081, 18181)
def port_free(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        return sock.connect_ex(("127.0.0.1", port)) != 0

def inspect() -> dict:
    return {
        "existing_stack_present": (STACK / "docker-compose.yml").is_file(),
        "existing_stack_version_marker": (STACK / ".supabase-version").is_file(),
        "isolated_target_exists": DEST.exists(),
        "docker_available": shutil.which("docker") is not None,
        "ports_free": {str(p): port_free(p) for p in PORTS},
        "safe_to_modify_existing_stack": False,
        "credentials_present": "NOT_CHECKED",
        "account_login_ready": False,
    }

if __name__ == "__main__":
    print(json.dumps(inspect(), indent=2, ensure_ascii=False))
