import test from "node:test";
import assert from "node:assert/strict";
import { webcrypto as crypto } from "node:crypto";
import { verifyDeviceProof } from "./device-proof.mjs";

const b64 = bytes => Buffer.from(bytes).toString("base64url");
const now = 1792530000000;
const id = "12345678-1234-4234-8234-123456789abc";

async function fixture() {
  const pair = await crypto.subtle.generateKey("Ed25519", true, ["sign", "verify"]);
  const publicKey = b64(await crypto.subtle.exportKey("raw", pair.publicKey));
  const seen = new Set();
  let requests = 0;
  const record = { public_key: publicKey, enabled: 1, expires_at: now / 1000 + 1000 };
  const env = { DB: { prepare(sql) {
    return { bind(...args) { return {
      async first() { requests++; assert.match(sql, /SELECT public_key/); return { ...record }; },
      async run() {
        assert.match(sql, /INSERT OR IGNORE INTO device_nonces/);
        const key = args[0] + ":" + args[1];
        if (seen.has(key)) return { meta: { changes: 0 } };
        seen.add(key); return { meta: { changes: 1 } };
      }
    }; } };
  } } };
  async function signed(path = "/v1/resolve/movie/329/prime", opts = {}) {
    const time = String(Math.floor(now / 1000) + (opts.offset || 0));
    const nonce = opts.nonce || "a".repeat(32);
    const message = ["ANYTV1", "GET", path, time, nonce].join("\n");
    const signature = b64(await crypto.subtle.sign("Ed25519", pair.privateKey,
      new TextEncoder().encode(message)));
    return new Request("https://resolver.test" + (opts.requestPath || path), {
      headers: { "x-anytv-device": id, "x-anytv-time": time,
        "x-anytv-nonce": nonce, "x-anytv-signature": signature }
    });
  }
  return { env, record, signed, count: () => requests, deps: { now: () => now, subtle: crypto.subtle } };
}

test("preapproved device valid proof succeeds only once", async () => {
  const f = await fixture(); const req = await f.signed();
  assert.equal(await verifyDeviceProof(req, f.env, f.deps), true);
  assert.equal(await verifyDeviceProof(req, f.env, f.deps), false);
});
test("revoked device is denied", async () => {
  const f = await fixture(); f.record.enabled = 0;
  assert.equal(await verifyDeviceProof(await f.signed(), f.env, f.deps), false);
});
test("expired device is denied", async () => {
  const f = await fixture(); f.record.expires_at = now / 1000;
  assert.equal(await verifyDeviceProof(await f.signed(), f.env, f.deps), false);
});
test("expired proof is denied before DB lookup", async () => {
  const f = await fixture();
  assert.equal(await verifyDeviceProof(await f.signed(undefined, { offset: -61 }), f.env, f.deps), false);
  assert.equal(f.count(), 0);
});
test("tampered path cannot be replayed", async () => {
  const f = await fixture();
  assert.equal(await verifyDeviceProof(await f.signed(undefined,
    { requestPath: "/v1/resolve/movie/330/prime" }), f.env, f.deps), false);
});
test("missing configuration and forged signature fail closed", async () => {
  const f = await fixture(); const req = await f.signed();
  assert.equal(await verifyDeviceProof(req, {}, f.deps), false);
  const headers = new Headers(req.headers);
  headers.set("x-anytv-signature", "A".repeat(86));
  assert.equal(await verifyDeviceProof(new Request(req.url, { headers }), f.env, f.deps), false);
});
test("unknown device and DB errors never authorize", async () => {
  const f = await fixture();
  f.env.DB.prepare = () => ({ bind: () => ({ first: async () => null }) });
  assert.equal(await verifyDeviceProof(await f.signed(), f.env, f.deps), false);
  f.env.DB.prepare = () => { throw Error("database outage"); };
  assert.equal(await verifyDeviceProof(await f.signed(), f.env, f.deps), false);
});
