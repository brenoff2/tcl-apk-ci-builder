#!/usr/bin/env bash
set -euo pipefail
# LOCAL-ONLY D1 / Miniflare smoke; never uses --remote or production IDs.
WRANGLER="npx --yes wrangler@4.45.3"
CFG="wrangler.local.toml"
DB="anytv-d1-local-smoke"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --file migrations/0002_device_proof.sql --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --file migrations/0003_device_activations.sql --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "INSERT INTO device_activations(code_hash,expires_at,valid_until) VALUES('d1-smoke-code',2000000000,2000200000);" --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "INSERT INTO device_keys(device_id,public_key,enabled,expires_at) SELECT 'd1-test-device','p256:local-key',1,valid_until FROM device_activations WHERE code_hash='d1-smoke-code' AND used_at IS NULL AND expires_at>1792530000 AND valid_until>1792530000 ON CONFLICT(device_id) DO NOTHING; UPDATE device_activations SET used_at=1792530000,device_id='d1-test-device' WHERE code_hash='d1-smoke-code' AND used_at IS NULL AND expires_at>1792530000 AND changes()=1 AND EXISTS(SELECT 1 FROM device_keys WHERE device_id='d1-test-device' AND public_key='p256:local-key');" --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "SELECT CASE WHEN (SELECT count(*) FROM device_keys)=1 AND (SELECT count(*) FROM device_activations WHERE used_at IS NOT NULL)=1 THEN 'D1_LOCAL_ACTIVATION_OK' ELSE 'D1_LOCAL_ACTIVATION_FAILED' END AS outcome;" --json --yes | grep -q D1_LOCAL_ACTIVATION_OK
# A second unused approval must not be consumed if the device_id exists.
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "INSERT INTO device_activations(code_hash,expires_at,valid_until) VALUES('collision-code',2000000000,2000200000);" --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "INSERT INTO device_keys(device_id,public_key,enabled,expires_at) SELECT 'd1-test-device','p256:local-key',1,valid_until FROM device_activations WHERE code_hash='collision-code' AND used_at IS NULL AND expires_at>1792530000 AND valid_until>1792530000 ON CONFLICT(device_id) DO NOTHING; UPDATE device_activations SET used_at=1792530000,device_id='d1-test-device' WHERE code_hash='collision-code' AND used_at IS NULL AND expires_at>1792530000 AND changes()=1 AND EXISTS(SELECT 1 FROM device_keys WHERE device_id='d1-test-device' AND public_key='p256:local-key');" --yes
$WRANGLER d1 execute "$DB" --config "$CFG" --local --persist-to "$DIR" --command "SELECT CASE WHEN (SELECT used_at FROM device_activations WHERE code_hash='collision-code') IS NULL AND (SELECT count(*) FROM device_keys)=1 THEN 'D1_LOCAL_COLLISION_OK' ELSE 'D1_LOCAL_COLLISION_FAILED' END AS outcome;" --json --yes | grep -q D1_LOCAL_COLLISION_OK
printf 'D1_LOCAL_COLLISION_OK\n'
printf 'D1_LOCAL_ACTIVATION_OK\n'
