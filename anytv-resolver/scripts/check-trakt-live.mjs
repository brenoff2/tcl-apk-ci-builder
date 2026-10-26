// Read-only release preflight. Never passes Trakt credentials to the client.
// Usage: ANYTV_WORKER_ORIGIN=https://... node scripts/check-trakt-live.mjs
const origin = process.env.ANYTV_WORKER_ORIGIN;
const version = process.env.ANYTV_TRAKT_TEST_VERSION;
const workerName = "anytv-link-resolver-pilot";
// Version override is valid only after the candidate joins a deployment at 0%.
if (version && !/^[0-9a-f]{8}-[0-9a-f-]{27}$/.test(version)) {
  throw Error("Invalid candidate version UUID");
}
if (!origin || !/^https:\/\/[a-z0-9.-]+$/i.test(origin)) {
  console.error("Set ANYTV_WORKER_ORIGIN to the HTTPS Worker origin (without a path).");
  process.exitCode = 2;
} else {
  const checks = ["movie", "tv"];
  let failures = 0;
  for (const kind of checks) {
    try {
      const response = await fetch(`${origin}/v1/catalog/signals/trakt/${kind}`, {
        headers: {
          Accept: "application/json",
          ...(version ? { "Cloudflare-Workers-Version-Overrides":
            `${workerName}="${version}"` } : {}),
        },
        signal: AbortSignal.timeout(8000), redirect: "manual",
      });
      const body = await response.json();
      const valid = response.status === 200 && body.source === "trakt" &&
        body.kind === kind && Array.isArray(body.results) &&
        body.results.every(x => Number.isSafeInteger(x.id) && x.id > 0 &&
          Number.isSafeInteger(x.watchers) && x.watchers >= 0);
      console.log(`${kind}: ${valid ? "PASS" : "FAIL"} HTTP ${response.status}${version ? " [version override requested]" : ""}`);
      if (!valid) failures++;
    } catch (err) {
      failures++;
      console.log(`${kind}: FAIL ${err?.name || "network_error"}`);
    }
  }
  if (failures) process.exitCode = 1;
}
