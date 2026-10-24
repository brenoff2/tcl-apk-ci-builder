import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes, webcrypto as crypto, createHash } from "node:crypto";
import { handleRequest } from "./worker.mjs";

const now = 1792530000000;
const seconds = Math.floor(now / 1000);
const id = "12345678-1234-4234-8234-123456789abc";
const b64 = b => Buffer.from(b).toString("base64url");

test("activation -> idempotent acknowledgment -> resolver; no upstream on denied traffic", async () => {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const publicKey = "p256:" + b64(await crypto.subtle.exportKey("raw", pair.publicKey));
  const code = b64(randomBytes(32));
  const codeHash = createHash("sha256").update(Buffer.from(code, "base64url")).digest("base64url");
  let activated = false, upstream = 0;
  const nonceUsed = new Set();
  const device = { public_key: publicKey, enabled: 1, expires_at: seconds + 3600 };
  const db = {
    prepare(sql) { return { bind(...args) { return {
      async first() {
        if (sql.includes("FROM device_activations a JOIN device_keys")) return activated && args[0] === codeHash && args[1] === id && args[2] === publicKey && args[3] < device.expires_at ? {matched:1} : null;
        if (sql.includes("FROM device_keys")) return activated ? device : null;
        if (sql.includes("FROM link_cache")) return {
          payload: JSON.stringify({ status: "not_available" }), expires_at: seconds + 900
        };
        throw Error("unexpected first statement");
      },
      async run() {
        assert.match(sql, /INSERT OR IGNORE INTO device_nonces/);
        if (nonceUsed.has(args[1])) return { meta: { changes: 0 } };
        nonceUsed.add(args[1]); return { meta: { changes: 1 } };
      },
      sql, args
    }; } }; },
    async batch(statements) {
      assert.equal(statements.length, 2);
      assert.equal(statements[0].args[2], codeHash);
      if (activated) return [{ meta: { changes: 0 } }, { meta: { changes: 0 } }];
      activated = true;
      return [{ meta: { changes: 1 } }, { meta: { changes: 1 } }];
    }
  };
  const env = {
    DB: db, ANYTV_DEVICE_ACTIVATION_ENABLED: "true",
    ANYTV_RESOLVER_AUTH_MODE: "device", ANYTV_MOVIE_API_KEY: "server-only",
    RATE_LIMIT: { limit: async () => ({ success: true }) }, MAX_UPSTREAM_CALLS_MONTH: "1"
  };
  const deps = { now: () => now, subtle: crypto.subtle,
    fetch: async () => { upstream++; throw Error("upstream reached"); } };
  const path = "/v1/resolve/movie/329/prime";
  const stamp = String(seconds);
  const nonce = "c".repeat(48);
  const signature = b64(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" },
    pair.privateKey, new TextEncoder().encode(["ANYTV1", "GET", path, stamp, nonce].join("\n"))));
  const signed = () => new Request("https://resolver.test" + path, {
    headers: { "x-anytv-device": id, "x-anytv-time": stamp,
      "x-anytv-nonce": nonce, "x-anytv-signature": signature }
  });
  assert.equal((await handleRequest(signed(), env, deps)).status, 401);
  assert.equal(upstream, 0);
  const activationProof = b64(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" },
    pair.privateKey, new TextEncoder().encode(
      ["ANYTV-ACTIVATE1", id, publicKey, code].join("\n"))));
  const activation = () => new Request("https://resolver.test/v1/device/activate", {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ activation_code: code, device_id: id,
      public_key: publicKey, proof: activationProof })
  });
  assert.equal((await handleRequest(activation(), env, deps)).status, 201);
  assert.equal((await handleRequest(activation(), env, deps)).status, 201);
  const statusPath = "/v1/device/status";
  const statusNonce = "a".repeat(48);
  const statusSig = b64(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" },
    pair.privateKey, new TextEncoder().encode(
      ["ANYTV1", "GET", statusPath, stamp, statusNonce].join("\n"))));
  const statusRequest = () => new Request("https://resolver.test" + statusPath, {
    headers: { "x-anytv-device": id, "x-anytv-time": stamp,
      "x-anytv-nonce": statusNonce, "x-anytv-signature": statusSig }
  });
  const active = await handleRequest(statusRequest(), env, deps);
  assert.equal(active.status, 200);
  assert.deepEqual(await active.json(), { status: "activated" });
  assert.equal((await handleRequest(statusRequest(), env, deps)).status, 401);
  const accepted = await handleRequest(signed(), env, deps);
  assert.equal(accepted.status, 404);
  assert.equal((await accepted.json()).cached, true);
  assert.equal(upstream, 0); // cache response: provider never contacted
  assert.equal((await handleRequest(signed(), env, deps)).status, 401);
  assert.equal(upstream, 0);
});
