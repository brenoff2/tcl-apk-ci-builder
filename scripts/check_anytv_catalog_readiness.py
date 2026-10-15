#!/usr/bin/env python3
"""Release gate: verify the public catalogue actually returns live data."""
import json
import sys
import urllib.request

ROOT = "https://anytv-link-resolver-pilot.brenoff.workers.dev"
ROUTES = [
    ("/v1/catalog/genre/movie/list?language=pt-BR", "genres"),
    ("/v1/catalog/watch/providers/movie?language=pt-BR&watch_region=BR", "results"),
]
for route, array_name in ROUTES:
    try:
        request = urllib.request.Request(ROOT + route, headers={"Accept": "application/json"})
        with urllib.request.urlopen(request, timeout=15) as response:
            data = response.read(450001)
            assert response.status == 200 and len(data) <= 450000
        payload = json.loads(data)
        assert isinstance(payload, dict) and isinstance(payload.get(array_name), list)
        assert payload[array_name], "The live catalog is empty"
    except Exception:
        print("BLOCKED: catalog backend is not ready for OTA release", file=sys.stderr)
        raise SystemExit(1) from None
    print("PASS " + route.split("?")[0])
print("ANYTV_CATALOG_LIVE_READINESS_OK")
