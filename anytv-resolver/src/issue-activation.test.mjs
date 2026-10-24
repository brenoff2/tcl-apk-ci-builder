import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { issueActivation } from "../scripts/issue-activation.mjs";

test("operator issuance returns 256-bit code and only hash for D1", () => {
  const item = issueActivation({ now: 1700000000, random: size => Buffer.alloc(size, 7) });
  assert.match(item.code, /^[A-Za-z0-9_-]{43}$/);
  const bytes = Buffer.from(item.code, "base64url");
  assert.equal(bytes.length, 32);
  assert.equal(item.codeHash, createHash("sha256").update(bytes).digest("base64url"));
  assert.notEqual(item.code, item.codeHash);
  assert.equal(item.expiresAt, 1700000600);
  assert.equal(item.validUntil, 1700000000 + 365 * 86400);
});
test("operator policy rejects weak randomness and unsafe expiration", () => {
  assert.throws(() => issueActivation({ random: () => Buffer.alloc(10) }));
  assert.throws(() => issueActivation({ activationSeconds: 86400 }));
  assert.throws(() => issueActivation({ deviceDays: 366 }));
  assert.throws(() => issueActivation({ deviceDays: 0 }));
});
