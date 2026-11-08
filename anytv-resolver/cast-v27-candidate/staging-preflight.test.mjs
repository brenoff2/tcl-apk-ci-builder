import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const preflight = readFileSync(
  new URL("../../.github/workflows/anytv-v27-cast-staging-readonly-preflight.yml",
    import.meta.url), "utf8");
const deviceP0 = readFileSync(
  new URL("../../.github/workflows/anytv-p0-d1-staging-preflight.yml",
    import.meta.url), "utf8");

test("cast staging preflight is manual and independent of device proof P0", () => {
  assert.match(preflight, /workflow_dispatch:/);
  assert.doesNotMatch(preflight, /\bpush:/);
  assert.match(preflight, /staging_database_id:/);
  assert.match(deviceP0, /device_keys", "device_nonces", "device_activations"/);
  assert.doesNotMatch(preflight, /device_keys", "device_nonces", "device_activations"/);
});
test("preflight checks canonical account, staging D1 and static validator before read", () => {
  assert.match(preflight, /48172e80b82e7f0a2100e512831c48d2/);
  assert.match(preflight, /9bab6598-d019-4e88-8b29-a327ff0cfd00/);
  assert.match(preflight, /check-staging-config\.mjs/);
  assert.match(preflight, /CAST_STAGING_REMOTE_D1_IDENTITY_OK/);
  assert.match(preflight, /record\.get\("name"\) == "anytv-cast-staging"/);
  assert.match(preflight, /preview_urls": False/);
  assert.match(preflight, /ANYTV_DEVICE_ACTIVATION_ENABLED": "false"/);
});
test("remote stage reads only sqlite schema without D1 migrations or Worker deployment", () => {
  assert.match(preflight, /permissions:\s*\n\s+contents: read/);
  assert.match(preflight, /--command "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"/);
  assert.match(preflight, /--remote/);
  assert.match(preflight, /names\.has\("link_cache"\)/);
  assert.doesNotMatch(preflight, /wrangler.*(?:deploy|versions upload|migrations apply)/);
  assert.doesNotMatch(preflight, /--command "(?:INSERT|UPDATE|DELETE|ALTER|DROP|CREATE)/);
  assert.match(preflight, /CAST_STAGING_INSPECTION_ONLY/);
});
