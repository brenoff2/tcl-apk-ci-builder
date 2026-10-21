/**
 * AnyTV P0: verifier only. Enrollment is intentionally NOT implemented.
 * A signed request is accepted only for a previously authorized device key
 * stored in D1. Never ship a server bearer token or private key in an APK.
 *
 * D1 schema required before integration (NOT deployed):
 * CREATE TABLE device_keys (
 *   device_id TEXT PRIMARY KEY, public_key TEXT NOT NULL,
 *   enabled INTEGER NOT NULL DEFAULT 0, expires_at INTEGER NOT NULL
 * );
 * CREATE TABLE device_nonces (
 *   device_id TEXT NOT NULL, nonce TEXT NOT NULL,
 *   expires_at INTEGER NOT NULL,
 *   PRIMARY KEY(device_id, nonce)
 * );
 */
const deviceIdPattern = /^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/;
const noncePattern = /^[a-f0-9]{32,64}$/;
const signaturePattern = /^[A-Za-z0-9_-]{86}$/;
const keyPattern = /^[A-Za-z0-9_-]{43}$/;

function decode64url(value) {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - value.length % 4) % 4);
  return Uint8Array.from(atob(padded), c => c.charCodeAt(0));
}

/**
 * Header contract: X-AnyTV-Device, X-AnyTV-Time, X-AnyTV-Nonce,
 * X-AnyTV-Signature. Sign UTF-8:
 * ANYTV1\nMETHOD\nPATH_AND_QUERY\nTIME\nNONCE
 * Ed25519 keys are generated on the device and preapproved out of band.
 * Possession of an unapproved private key NEVER authorizes enrollment.
 */
export async function verifyDeviceProof(request, env, deps = {}) {
  if (!env?.DB || request.method !== "GET") return false;
  const id = request.headers.get("x-anytv-device") || "";
  const stamp = request.headers.get("x-anytv-time") || "";
  const nonce = request.headers.get("x-anytv-nonce") || "";
  const signature = request.headers.get("x-anytv-signature") || "";
  if (!deviceIdPattern.test(id) || !/^[1-9][0-9]{9}$/.test(stamp) ||
      !noncePattern.test(nonce) || !signaturePattern.test(signature)) return false;
  const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
  const time = Number(stamp);
  if (!Number.isSafeInteger(time) || Math.abs(now - time) > 60) return false;
  try {
    const device = await env.DB.prepare(
      "SELECT public_key,enabled,expires_at FROM device_keys WHERE device_id = ? LIMIT 1"
    ).bind(id).first();
    if (!device || device.enabled !== 1 || device.expires_at <= now ||
        !keyPattern.test(device.public_key)) return false;
    const url = new URL(request.url);
    const payload = ["ANYTV1", request.method, url.pathname + url.search, stamp, nonce].join("\n");
    const subtle = deps.subtle || crypto.subtle;
    const key = await subtle.importKey("raw", decode64url(device.public_key),
      { name: "Ed25519" }, false, ["verify"]);
    const valid = await subtle.verify("Ed25519", key, decode64url(signature),
      new TextEncoder().encode(payload));
    if (!valid) return false;
    // Atomic insert makes the first signature usable once, preventing replay.
    // Deletion/retention of expired nonces must be handled separately.
    const replay = await env.DB.prepare(
      "INSERT OR IGNORE INTO device_nonces(device_id,nonce,expires_at) VALUES (?,?,?)"
    ).bind(id, nonce, now + 120).run();
    return replay?.meta?.changes === 1;
  } catch {
    return false;
  }
}
