import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes, webcrypto } from "node:crypto";
import { activateDevice } from "./device-activation.mjs";
import { handleRequest } from "./worker.mjs";

const now = 1792530000000;
const device_id = "12345678-1234-4234-8234-123456789abc";
const b64 = bytes => Buffer.from(bytes).toString("base64url");
const code = b64(randomBytes(32));

async function setup() {
  const keys = await webcrypto.subtle.generateKey("Ed25519", true, ["sign", "verify"]);
  const public_key = b64(await webcrypto.subtle.exportKey("raw", keys.publicKey));
  const digest = b64(await webcrypto.subtle.digest("SHA-256", Buffer.from(code, "base64url")));
  const activation = { code_hash: digest, expires_at: Math.floor(now / 1000) + 200,
    valid_until: Math.floor(now / 1000) + 1000, used_at: null };
  const registered = new Map();
  const env = {
    ANYTV_DEVICE_ACTIVATION_ENABLED: "true",
    RATE_LIMIT: { limit: async () => ({ success: true }) },
    DB: {
      prepare(sql) { return { bind(...args) { return { sql, args }; } }; },
      async batch(statements) {
        assert.equal(statements.length, 2);
        const [insert, consume] = statements;
        assert.match(insert.sql, /INSERT INTO device_keys/);
        assert.match(consume.sql, /UPDATE device_activations/);
        const [id, key, hash, time] = insert.args;
        if (hash !== activation.code_hash || activation.used_at !== null ||
            activation.expires_at <= time || registered.has(id)) {
          return [{ meta: { changes: 0 } }, { meta: { changes: 0 } }];
        }
        registered.set(id, { public_key: key, enabled: 1 });
        activation.used_at = time;
        return [{ meta: { changes: 1 } }, { meta: { changes: 1 } }];
      }
    }
  };
  const request = async (body = { device_id, public_key, activation_code: code }, signingKey = keys.privateKey) => {
    const payload = ["ANYTV-ACTIVATE1", body.device_id, body.public_key, body.activation_code].join("\n");
    const algorithm = body.public_key.startsWith("p256:") ?
      { name: "ECDSA", hash: "SHA-256" } : "Ed25519";
    const proof = b64(await webcrypto.subtle.sign(algorithm, signingKey,
      new TextEncoder().encode(payload)));
    return new Request("https://resolver.test/v1/device/activate",
      { method: "POST", body: JSON.stringify({ ...body, proof }) });
  };
  return { env, request, registered, activation, deps: { now: () => now, subtle: webcrypto.subtle } };
}

test("operator-approved one-use code activates once, replay denied", async () => {
  const f = await setup();
  assert.equal((await activateDevice(await f.request(), f.env, f.deps)).status, 201);
  assert.equal(f.registered.size, 1);
  assert.equal((await activateDevice(await f.request(), f.env, f.deps)).status, 403);
  assert.equal(f.registered.size, 1);
});

test("invalid activation codes and malformed public keys are refused", async () => {
  const f = await setup();
  const bad = await f.request({ device_id, public_key: "invalid", activation_code: code });
  assert.equal((await activateDevice(bad, f.env, f.deps)).status, 400);
  assert.equal((await activateDevice(await f.request({
    device_id, public_key: "invalid", activation_code: code
  }), f.env, f.deps)).status, 400);
  assert.equal(f.registered.size, 0);
});

test("denies when rate-limited or D1 transactional batch is absent", async () => {
  const f = await setup();
  f.env.RATE_LIMIT.limit = async () => ({ success: false });
  assert.equal((await activateDevice(await f.request(), f.env, f.deps)).status, 429);
  f.env.DB.batch = undefined;
  assert.equal((await activateDevice(await f.request(), f.env, f.deps)).status, 503);
});

test("activation endpoint is closed unless explicitly enabled", async () => {
  const f = await setup();
  delete f.env.ANYTV_DEVICE_ACTIVATION_ENABLED;
  const response = await handleRequest(await f.request(), f.env, f.deps);
  assert.equal(response.status, 405);
  assert.equal(f.registered.size, 0);
});

test("Android Keystore P-256 public key activates with operator code", async () => {
  const f = await setup();
  const pair = await webcrypto.subtle.generateKey(
    { name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const key = "p256:" + b64(await webcrypto.subtle.exportKey("raw", pair.publicKey));
  const response = await activateDevice(
    await f.request({ device_id, public_key: key, activation_code: code }, pair.privateKey), f.env, f.deps);
  assert.equal(response.status, 201);
  assert.equal(f.registered.get(device_id).public_key, key);
});

test("forged possession proof cannot consume a valid activation code", async () => {
  const f = await setup();
  const attacker = await webcrypto.subtle.generateKey("Ed25519", true, ["sign", "verify"]);
  const forged = await f.request(undefined, attacker.privateKey);
  assert.equal((await activateDevice(forged, f.env, f.deps)).status, 403);
  assert.equal(f.registered.size, 0);
  assert.equal((await activateDevice(await f.request(), f.env, f.deps)).status, 201);
});
