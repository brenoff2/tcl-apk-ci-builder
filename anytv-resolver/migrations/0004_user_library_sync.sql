-- AnyTV user library: prepared schema, NOT a live authentication service.
-- Deploy only after a user JWT verifier and per-user authorization are installed.
-- Distinct from device_keys: never grant links access based on library account.
CREATE TABLE IF NOT EXISTS user_library_items (
  user_id TEXT NOT NULL,
  kind TEXT NOT NULL CHECK(kind IN ('favorite','watched_movie','tracked_series','watched_episode','preferred_genre','preferred_provider')),
  item_key TEXT NOT NULL,
  payload TEXT NOT NULL DEFAULT '{}',
  revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
  updated_at INTEGER NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0 CHECK (deleted IN (0,1)),
  PRIMARY KEY (user_id,kind,item_key)
);
CREATE INDEX IF NOT EXISTS idx_user_library_changes
 ON user_library_items (user_id,updated_at,kind,item_key);
CREATE TABLE IF NOT EXISTS user_library_operations (
  user_id TEXT NOT NULL,
  operation_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  PRIMARY KEY(user_id,operation_id)
);
-- Never expose the D1 table directly to a phone. A backend must verify the JWT
-- and derive user_id from its validated subject, not client-supplied JSON.
