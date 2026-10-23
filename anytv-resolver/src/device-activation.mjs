// Operator-issued, single-use 256-bit activation codes. Never include codes
// in APKs. No self-registration: a code hash must exist in D1 before activation.
const uuid = /^[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}$/;
const token = /^[A-Za-z0-9_-]{43}$/;
const p256Key = /^p256:[A-Za-z0-9_-]{87}$/;
const reply = (status, code) => new Response(JSON.stringify({ status: code }), {
  status, headers: { "content-type": "application/json", "cache-control": "no-store" }
});
const decode = s => Uint8Array.from(atob(s.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - s.length % 4) % 4)),
  c => c.charCodeAt(0));
const encode = bytes => btoa(String.fromCharCode(...bytes))
  .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");

export async function activateDevice(request, env, deps = {}) {
  if (request.method !== "POST") return reply(405, "method_not_allowed");
  // No DB.batch, no enrollment: single-use code and key must commit together.
  if (!env?.DB?.batch || !env?.RATE_LIMIT) return reply(503, "not_configured");
  if (Number(request.headers.get("content-length") || 0) > 1024) return reply(400, "invalid_request");
  try {
    const ip = request.headers.get("cf-connecting-ip") || "unknown";
    if (!(await env.RATE_LIMIT.limit({ key: "activate:" + ip })).success) return reply(429, "rate_limited");
    const raw = await request.text();
    if (raw.length > 1024) return reply(400, "invalid_request");
    const item = JSON.parse(raw);
    if (!item || typeof item !== "object" || Array.isArray(item) ||
        Object.keys(item).sort().join(",") !== "activation_code,device_id,proof,public_key" ||
        !uuid.test(item.device_id) || !token.test(item.activation_code) ||
        !(token.test(item.public_key) || p256Key.test(item.public_key)) ||
        typeof item.proof !== "string" ||
        !/^[A-Za-z0-9_-]{86}$/.test(item.proof)) return reply(400, "invalid_request");
    const subtle = deps.subtle || crypto.subtle;
    const p256 = item.public_key.startsWith("p256:");
    const publicKey = await subtle.importKey("raw", decode(p256 ? item.public_key.slice(5) : item.public_key),
      p256 ? { name: "ECDSA", namedCurve: "P-256" } : { name: "Ed25519" }, false, ["verify"]);
    const possession = new TextEncoder().encode(["ANYTV-ACTIVATE1", item.device_id,
      item.public_key, item.activation_code].join("\n"));
    const verified = await subtle.verify(p256 ? { name: "ECDSA", hash: "SHA-256" } : "Ed25519",
      publicKey, decode(item.proof), possession);
    if (!verified) return reply(403, "activation_denied");
    const digest = new Uint8Array(await subtle.digest("SHA-256", decode(item.activation_code)));
    const hash = encode(digest);
    const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
    // A lost HTTP 201 response must not strand a legitimately enrolled device.
    // Reconfirm only if this exact code was consumed by the same device/key,
    // and the bound identity remains enabled and unexpired. Never allow reuse
    // with a different key, device or unverified possession proof.
    const previous = await env.DB.prepare(`SELECT 1 AS matched
      FROM device_activations a JOIN device_keys k ON k.device_id=a.device_id
      WHERE a.code_hash=? AND a.used_at IS NOT NULL
        AND a.device_id=? AND k.public_key=? AND k.enabled=1
        AND k.expires_at>? LIMIT 1`)
      .bind(hash, item.device_id, item.public_key, now).first();
    if (previous?.matched === 1) return reply(201, "activated");
    // D1 batch is transactional. INSERT must be conditional on an unused code;
    // UPDATE consumes that code only if this device/key was inserted.
    const statements = [
      env.DB.prepare(`INSERT INTO device_keys(device_id,public_key,enabled,expires_at)
        SELECT ?,?,1,valid_until FROM device_activations
        WHERE code_hash=? AND used_at IS NULL AND expires_at>? AND valid_until>?
        ON CONFLICT(device_id) DO NOTHING`).bind(item.device_id, item.public_key, hash, now, now),
      env.DB.prepare(`UPDATE device_activations SET used_at=?,device_id=?
        WHERE code_hash=? AND used_at IS NULL AND expires_at>?
          AND changes()=1
          AND EXISTS(SELECT 1 FROM device_keys WHERE device_id=? AND public_key=?)`)
        .bind(now, item.device_id, hash, now, item.device_id, item.public_key)
    ];
    const results = await env.DB.batch(statements);
    if (results?.[0]?.meta?.changes !== 1 || results?.[1]?.meta?.changes !== 1) {
      return reply(403, "activation_denied");
    }
    return reply(201, "activated");
  } catch {
    return reply(503, "activation_unavailable");
  }
}
