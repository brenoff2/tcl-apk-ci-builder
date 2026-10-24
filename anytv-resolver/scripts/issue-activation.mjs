// Offline operator-only activation issuer. Do not run in CI or store stdout.
// The code is a 256-bit random secret; ONLY its SHA-256 hash is stored in D1.
import { randomBytes, createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
const base64url = b => Buffer.from(b).toString("base64url");

export function issueActivation({ now = Math.floor(Date.now() / 1000),
  activationSeconds = 600, deviceDays = 365, random = randomBytes } = {}) {
  if (!Number.isSafeInteger(now) || now <= 0 ||
      !Number.isSafeInteger(activationSeconds) || activationSeconds < 60 ||
      activationSeconds > 3600 || !Number.isSafeInteger(deviceDays) ||
      deviceDays < 1 || deviceDays > 365) throw Error("invalid operator policy");
  const bytes = random(32);
  if (!(bytes instanceof Uint8Array) || bytes.length !== 32) {
    throw Error("secure random source must produce 32 bytes");
  }
  const code = base64url(bytes);
  const hash = base64url(createHash("sha256").update(bytes).digest());
  const expires = now + activationSeconds;
  const validUntil = now + deviceDays * 86400;
  // Hash is base64url, safe for parameterized SQL; SQL here is illustrative.
  // Administrative insertion must use a privileged D1 credential out-of-band.
  return { code, codeHash: hash, expiresAt: expires, validUntil };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  if (!process.stdin.isTTY || !process.stdout.isTTY) {
    console.error("Refusing non-interactive activation issuance");
    process.exitCode = 1;
  } else {
    const result = issueActivation();
    console.log("ACTIVATION CODE (sensitive, show once):", result.code);
    console.log("D1 OPERATOR-ONLY PARAMETERS (no raw code):",
      JSON.stringify({ code_hash: result.codeHash,
        expires_at: result.expiresAt, valid_until: result.validUntil }));
    console.log("Parameterized INSERT INTO device_activations(code_hash,expires_at,valid_until) VALUES (?,?,?)");
  }
}
