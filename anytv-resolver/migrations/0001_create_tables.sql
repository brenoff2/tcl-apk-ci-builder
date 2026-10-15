-- Private AnyTV resolver; only aggregated quota and safe, short-lived URL candidates.
CREATE TABLE IF NOT EXISTS monthly_quota (
  month TEXT PRIMARY KEY,
  used INTEGER NOT NULL DEFAULT 0 CHECK(used >= 0)
);
CREATE TABLE IF NOT EXISTS link_cache (
  cache_key TEXT PRIMARY KEY,
  payload TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_anytv_cache_expiry ON link_cache(expires_at);
