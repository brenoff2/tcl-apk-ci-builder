import test from "node:test";
import assert from "node:assert/strict";
import { webcrypto } from "node:crypto";
import { handleRequest } from "./worker.mjs";

const subtle = webcrypto.subtle;
const b64 = x => Buffer.from(x).toString("base64url");
const requestUrl = "https://resolver.test/v1/device/request";

function dbMock() {
  const entries = new Map();
  const db = {
    entries,
    prepare(sql) {
      return {
        bind(...args) {
          return {
            async first() {
              if (sql.startsWith("SELECT public_key")) {
                const row = entries.get(args[0]);
                return row ? { ...row } : null;
              }
              if (sql.startsWith("SELECT COUNT(*) AS total")) {
                return { total: [...entries.values()].filter(x =>
                  x.enabled === 0 && x.expires_at > args[0]).length };
              }
              throw Error("Unexpected query " + sql);
            },
            async run() {
              if (sql.startsWith("INSERT INTO device_keys")) {
                const [id, key, expires] = args;
                const previous = entries.get(id);
                if (!previous || (previous.enabled === 0 && previous.public_key === key)) {
                  entries.set(id, { public_key: key, enabled: 0, expires_at: expires });
                }
                return { meta: { changes: 1 } };
              }
              throw Error("Unexpected write " + sql);
            }
          };
        }
      };
    }
  };
  return db;
}

async function signedEnrollment(pair, deviceId,
  nonce = "a".repeat(48), signedBy = pair) {
  const key = "p256:" + b64(await subtle.exportKey("raw", pair.publicKey));
  const payload = ["ANYTV-REQUEST1", deviceId, key, nonce].join("\n");
  const proof = b64(await subtle.sign({ name:"ECDSA", hash:"SHA-256" },
    signedBy.privateKey, new TextEncoder().encode(payload)));
  return new Request(requestUrl, { method: "POST",
    headers: { "content-type":"application/json" },
    body: JSON.stringify({ device_id:deviceId, public_key:key, nonce, proof }) });
}

test("public key registration remains pending until approved by operator", async () => {
  const db = dbMock();
  const env = { DB:db, ANYTV_RESOLVER_AUTH_MODE:"device",
    ANYTV_DEVICE_ACTIVATION_ENABLED:"true" };
  const pair = await subtle.generateKey({ name:"ECDSA", namedCurve:"P-256" },
    true, ["sign","verify"]);
  const id = "11111111-2222-4333-8444-555555555555";
  const accepted = await handleRequest(await signedEnrollment(pair,id),env,{ subtle });
  assert.equal(accepted.status,202);
  assert.deepEqual(await accepted.json(),{status:"pending_approval"});
  assert.equal(db.entries.get(id).enabled,0);
  const again = await handleRequest(await signedEnrollment(pair,id,"b".repeat(48)),env,{subtle});
  assert.equal(again.status,202);
  const another = await subtle.generateKey({ name:"ECDSA",namedCurve:"P-256" },
    true,["sign","verify"]);
  const forged = await handleRequest(await signedEnrollment(pair,id,"c".repeat(48),another),env,{subtle});
  assert.equal(forged.status,403);
  const conflicting = await handleRequest(await signedEnrollment(another,id,"d".repeat(48)),env,{subtle});
  assert.equal(conflicting.status,409);
  db.entries.get(id).enabled=1;
  const approved = await handleRequest(await signedEnrollment(pair,id,"e".repeat(48)),env,{subtle});
  assert.equal(approved.status,200);
});

test("registration never bypasses auth mode or grants a credential", async () => {
  const pair=await subtle.generateKey({name:"ECDSA",namedCurve:"P-256"},true,["sign","verify"]);
  const request=await signedEnrollment(pair,"11111111-2222-4333-8444-555555555555");
  const denied=await handleRequest(request, {DB:dbMock(),
    ANYTV_RESOLVER_AUTH_MODE:"pilot", ANYTV_DEVICE_ACTIVATION_ENABLED:"true" });
  assert.equal(denied.status,405);
  const env={DB:dbMock(),ANYTV_RESOLVER_AUTH_MODE:"device",
    ANYTV_DEVICE_ACTIVATION_ENABLED:"true"};
  const bad=await handleRequest(new Request(requestUrl,{method:"POST",
    body:JSON.stringify({device_id:"not-a-uuid",public_key:"bad",nonce:"bad",proof:"bad"})}),env);
  assert.equal(bad.status,400);
  assert.equal(env.DB.entries.size,0);
});
