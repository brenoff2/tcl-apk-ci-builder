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
        request = urllib.request.Request(ROOT + route, headers={"Accept": "application/json", "User-Agent": "AnyTV/2.9.8 (Android; streaming catalog client)"})
        with urllib.request.urlopen(request, timeout=15) as response:
            data = response.read(450001)
            assert response.status == 200 and len(data) <= 450000
        payload = json.loads(data)
        assert isinstance(payload, dict) and isinstance(payload.get(array_name), list)
        assert payload[array_name], "The live catalog is empty"
    except Exception as error:
        if hasattr(error, "headers"):
            print("UPSTREAM_STATUS: " + str(error.headers.get("x-anytv-upstream-status", "not_set")), file=sys.stderr)
            print("WORKER_FAILURE_STAGE: " + str(error.headers.get("x-anytv-failure-stage", "not_set")), file=sys.stderr)
            print("WORKER_FAILURE_TYPE: " + str(error.headers.get("x-anytv-failure-type", "not_set")), file=sys.stderr)
            print("HTTP_DIAG: server=" + str(error.headers.get("server")) + " cf-ray=" + str(bool(error.headers.get("cf-ray"))) + " content-type=" + str(error.headers.get("content-type")), file=sys.stderr)
            try:
                body = json.loads(error.read(1000))
                if isinstance(body, dict):
                    print("HTTP_BODY_STATUS: " + str(body.get("status", body.get("code", "unknown")))[:80], file=sys.stderr)
                    print("HTTP_BODY_KEYS: " + ",".join(sorted(body.keys()))[:160], file=sys.stderr)
                    print("HTTP_BODY_ERRORS: " + str(body.get("errors", "none"))[:240], file=sys.stderr)
                    print("CF_ERROR_DIAG: " + " ".join(str(body.get(k, ""))[:90] for k in ("error_code","error_name","error_category","title","detail","cloudflare_error")), file=sys.stderr)
                    print("HTTP_BODY_MESSAGE: " + str(body.get("message", body.get("error", "not_provided")))[:180], file=sys.stderr)
            except Exception:
                pass
        print("BLOCKED: catalog backend is not ready for OTA release: " + type(error).__name__ + " " + str(error)[:160], file=sys.stderr)
        raise SystemExit(1) from None
    print("PASS " + route.split("?")[0])
print("ANYTV_CATALOG_LIVE_READINESS_OK")
