/**
 * Operator-gated registration of a device public key. This is NOT activation:
 * anyone can request review, but only an authorized operator can enable the
 * D1 device_keys row. No reusable code or server credential is shipped in APKs.
 */
const idPattern = /^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/;
const publicKeyPattern = /^p256:[A-Za-z0-9_-]{87}$/;
const signaturePattern = /^[A-Za-z0-9_-]{86}$/;
const noncePattern = /^[a-f0-9]{48}$/;
const reply = (status, message) => new Response(JSON.stringify({ status: message }), {
  status, headers: { "content-type": "application/json", "cache-control": "no-store" }
});
const decode = text => Uint8Array.from(
  atob(text.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - text.length % 4) % 4)),
  x => x.charCodeAt(0));

export async function requestDeviceEnrollment(request, env, deps = {}) {
  if (request.method !== "POST") return reply(405, "method_not_allowed");
  if (!env?.DB?.prepare || env.ANYTV_RESOLVER_AUTH_MODE !== "device" ||
      env.ANYTV_DEVICE_ACTIVATION_ENABLED !== "true") return reply(503, "not_configured");
  if (Number(request.headers.get("content-length") || 0) > 1200)
    return reply(400, "invalid_request");
  try {
    const raw = await request.text();
    if (raw.length > 1200) return reply(400, "invalid_request");
    const data = JSON.parse(raw);
    if (!data || Array.isArray(data) || typeof data !== "object" ||
        Object.keys(data).sort().join(",") !== "device_id,nonce,proof,public_key" ||
        !idPattern.test(data.device_id) ||
        !publicKeyPattern.test(data.public_key) ||
        !noncePattern.test(data.nonce) ||
        !signaturePattern.test(data.proof)) return reply(400, "invalid_request");
    const subtle = deps.subtle || crypto.subtle;
    const publicKey = await subtle.importKey("raw", decode(data.public_key.slice(5)),
      { name: "ECDSA", namedCurve: "P-256" }, false, ["verify"]);
    const payload = ["ANYTV-REQUEST1", data.device_id, data.public_key, data.nonce].join("\n");
    if (!await subtle.verify({ name: "ECDSA", hash: "SHA-256" }, publicKey,
      decode(data.proof), new TextEncoder().encode(payload))) return reply(403, "invalid_proof");
    const existing = await env.DB.prepare(
      "SELECT public_key,enabled,expires_at FROM device_keys WHERE device_id=? LIMIT 1"
    ).bind(data.device_id).first();
    if (existing && existing.public_key !== data.public_key)
      return reply(409, "different_device_key");
    const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
    if (existing?.enabled === 1 && existing.expires_at > now) return reply(200, "activated");
    // Pending rows do NOT authorize any link; operator must approve separately.
    // Bound unauthenticated requests to protect D1 storage.
    if (!existing) {
      const row = await env.DB.prepare(
        "SELECT COUNT(*) AS total FROM device_keys WHERE enabled=0 AND expires_at>?"
      ).bind(now).first();
      if ((row?.total || 0) >= 100) return reply(503, "registration_unavailable");
    }
    await env.DB.prepare(
      "INSERT INTO device_keys(device_id,public_key,enabled,expires_at) VALUES (?,?,0,?) " +
      "ON CONFLICT(device_id) DO UPDATE SET expires_at=excluded.expires_at " +
      "WHERE device_keys.enabled=0 AND device_keys.public_key=excluded.public_key"
    ).bind(data.device_id, data.public_key, now + 7 * 86400).run();
    return reply(202, "pending_approval");
  } catch {
    return reply(503, "registration_unavailable");
  }
}
