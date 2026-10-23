-- P0 device identity storage. Schema only: migration is NOT applied remotely.
-- Provisioning is intentionally absent: device keys must be authenticated and
-- approved by a separate authority before enabled=1 is ever permitted.
CREATE TABLE IF NOT EXISTS device_keys (
  device_id TEXT PRIMARY KEY,
  public_key TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 0 CHECK (enabled IN (0, 1)),
  expires_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS device_nonces (
  device_id TEXT NOT NULL,
  nonce TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  PRIMARY KEY (device_id, nonce)
);
CREATE INDEX IF NOT EXISTS idx_device_nonces_expiry ON device_nonces(expires_at);
