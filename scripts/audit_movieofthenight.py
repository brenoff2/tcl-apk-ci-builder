#!/usr/bin/env python3
"""Read-only, low-quota AnyTV evaluation of Movie of the Night / BR links.

Never prints API keys or raw upstream JSON. Not a public catalog export.
No APKs, channels, GitHub releases, or user TV settings are changed.
"""
from __future__ import annotations
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse, parse_qs
from urllib.request import Request, urlopen

BASE = "https://api.movieofthenight.com/v4"
SERVICES = ("netflix", "disney", "prime", "apple", "hbo")
REFERENCES = (
    ("Zootopia 2", "movie/1084242", "disney"),
    ("Birdman", "movie/194662", "hbo"),
    ("Jurassic Park", "movie/329", "prime"),
    ("O Irlandês", "movie/398978", "netflix"),
    ("O Projeto Adam", "movie/696806", "netflix"),
    ("Divertida-Mente 2", "movie/1022789", "disney"),
)
VALID_TYPES = {"subscription", "free", "addon", "rent", "buy"}
UUID = r"[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"
VALIDATION_PATTERNS = {
    "netflix": ("www.netflix.com", rf"/title/[0-9]{{6,9}}"),
    "disney": ("www.disneyplus.com", rf"/browse/entity-{UUID}"),
    "prime": ("app.primevideo.com", r"/detail"),
    "apple": ("tv.apple.com", r"(?:/[a-z]{2})?/(?:movie|show)/(?:[^/]+/)?umc\.cmc\.[a-z0-9]{15,40}"),
    "hbo": ("play.hbomax.com", rf"/(?:show|movie)/{UUID}"),
}


def inspect_link(provider: str, link: str) -> str:
    """Return a classification, never accept arbitrary URLs for Android intents."""
    if provider not in VALIDATION_PATTERNS or not isinstance(link, str):
        return "unrecognized"
    try:
        url = urlparse(link)
        host, pattern = VALIDATION_PATTERNS[provider]
        if url.scheme != "https" or url.hostname != host or url.username or url.password or url.port:
            return "unsafe"
        if url.fragment or not re.fullmatch(pattern, url.path):
            return "unknown_path"
        if provider == "prime":
            query = parse_qs(url.query, keep_blank_values=True)
            gti = query.get("gti", [])
            if list(query) != ["gti"] or len(gti) != 1 or not re.fullmatch(
                rf"amzn[0-9]\.dv\.gti\.{UUID}", gti[0]
            ):
                return "unknown_query"
        if provider in ("netflix", "disney") and url.query:
            return "unknown_query"
        # Apple and Max referral parameters need per-app testing before use.
        if provider in ("apple", "hbo") and url.query:
            return "candidate_with_query"
        return "candidate"
    except (ValueError, TypeError):
        return "unsafe"


def collect_offers(show: dict, service: str) -> dict:
    options = show.get("streamingOptions") or {}
    if not isinstance(options, dict):
        return {"found": False, "status": "malformed", "selected_count": 0,
                "link_count": 0, "links": {}, "types": {}}
    options = options.get("br") or options.get("BR") or []
    selected = [
        option for option in options if isinstance(option, dict)
        and isinstance(option.get("service"), dict)
        and option["service"].get("id") == service
    ]
    # Reject addons and rentals as proof of a basic subscription.
    basic = [x for x in selected if x.get("type") in ("subscription", "free")]
    links = [
        inspect_link(service, x.get("link", "")) for x in basic
        if x.get("link")
    ]
    return {
        "found": bool(basic),
        "status": "ok",
        "selected_count": len(selected),
        "basic_count": len(basic),
        "link_count": len(links),
        "links": dict(Counter(links)),
        "types": dict(Counter(x.get("type", "unknown") for x in selected)),
    }


class Quota:
    def __init__(self, api_key: str, limit: int = 11):
        if not api_key:
            raise ValueError("Missing API key")
        self.api_key = api_key
        self.calls = 0
        self.limit = limit

    def get(self, path: str, params: dict | None = None):
        if self.calls >= self.limit:
            raise RuntimeError("Audit API request budget reached")
        self.calls += 1
        suffix = ("?" + urlencode(params)) if params else ""
        request = Request(BASE + path + suffix, headers={
            "X-API-Key": self.api_key,
            "Accept": "application/json",
            "User-Agent": "AnyTV-commercial-feasibility/1.0",
        })
        try:
            with urlopen(request, timeout=18) as response:
                if response.status != 200:
                    raise RuntimeError(f"HTTP {response.status}")
                return json.load(response)
        except HTTPError as exc:
            # Never echo response bodies: an upstream error could contain secrets.
            raise RuntimeError(f"Provider HTTP {exc.code} for {path}") from None
        except URLError as exc:
            raise RuntimeError(f"Transport error for {path}: {type(exc.reason).__name__}") from None


def run(api_key: str) -> dict:
    client = Quota(api_key)
    report = {"status": "partial", "country": "br", "planned_max_calls": 11,
              "results": {}, "reference_checks": [], "api_calls": 0}
    for service in SERVICES:
        # One request gives up to ten movie cases per service.
        items = client.get("/shows/top", {"country": "br", "service": service,
                                          "show_type": "movie"})
        if not isinstance(items, list):
            raise RuntimeError(f"Unexpected top result for {service}")
        selected = items[:10]
        examined = [collect_offers(item, service) for item in selected if isinstance(item, dict)]
        report["results"][service] = {
            "cases": len(examined),
            "available_with_basic_subscription": sum(x["found"] for x in examined),
            "candidate_links": sum(x["links"].get("candidate", 0)
                                   + x["links"].get("candidate_with_query", 0)
                                   for x in examined),
            "missing_or_nonstandard_links": sum(
                x["link_count"] - x["links"].get("candidate", 0)
                - x["links"].get("candidate_with_query", 0) for x in examined),
            "types": dict(sum((Counter(x["types"]) for x in examined), Counter())),
        }
    for label, movie_id, service in REFERENCES:
        show = client.get("/shows/" + movie_id.replace("/", "%2F"),
                          {"country": "br", "series_granularity": "show"})
        expected_id = movie_id
        actual_id = show.get("tmdbId")
        valid_id = actual_id == expected_id
        if not valid_id:
            raise RuntimeError(f"Provider mismatched TMDB ID for {label}")
        sample = collect_offers(show, service)
        report["reference_checks"].append({
            "test": label, "provider": service, "tmdb_id_valid": valid_id,
            "subscription_in_br": sample["found"], "link_types": sample["links"],
        })
    report["api_calls"] = client.calls
    report["status"] = "live_data_obtained_not_tv_verified"
    return report


def main():
    key = os.environ.get("ANYTV_MOVIE_API_KEY", "")
    if not key:
        print("LIVE_AUDIT_BLOCKED: configure repository Actions secret ANYTV_MOVIE_API_KEY")
        return 3
    try:
        report = run(key)
    except Exception as exc:
        print("LIVE_AUDIT_ERROR:", str(exc)[:180])
        return 1
    # Publish only aggregate counts, never whole API responses, images, deep link lists,
    # commercial credentials, or provider metadata databases.
    output = Path(os.environ.get("GITHUB_STEP_SUMMARY_FILE", "anytv-audit-summary.json"))
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("LIVE_AUDIT_COMPLETED:", json.dumps(report, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
