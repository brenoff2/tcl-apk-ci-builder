-- This migration is NOT deployed. Operator must provision a cryptographically
-- random 32-byte code out of band; store only base64url(SHA-256(code bytes)).
-- An activation record must be individually approved and expire quickly.
CREATE TABLE IF NOT EXISTS device_activations (
  code_hash TEXT PRIMARY KEY,
  expires_at INTEGER NOT NULL,
  valid_until INTEGER NOT NULL,
  used_at INTEGER,
  device_id TEXT
);
CREATE INDEX IF NOT EXISTS idx_device_activations_expiry
  ON device_activations(expires_at);
