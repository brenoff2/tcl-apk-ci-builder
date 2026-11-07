"""Fail-closed static contract for future Cloudflare Workers Previews.

No network, credentials, remote databases or Wrangler preview calls.
Cloudflare Access and remote D1 existence still need separate readbacks.
"""
import copy
import json
from pathlib import Path
import re
import unittest


CONFIG = Path(__file__).resolve().parents[1] / "wrangler.jsonc"
UUID = re.compile(r"^[a-fA-F0-9]{8}-(?:[a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12}$")
# No storage/worker bindings except D1 until each new binding has a reviewed
# isolation policy. Observability/limits do not point at production data.
ALLOWED_PREVIEW_FIELDS = {"d1_databases", "vars", "observability", "logpush",
                          "limits", "placement"}
ALLOWED_PREVIEW_VARS = {"ANYTV_DEVICE_ACTIVATION_ENABLED",
                        "ANYTV_RESOLVER_AUTH_MODE"}


def validate_config(config):
    prod = config.get("d1_databases")
    if not isinstance(prod, list) or len(prod) != 1:
        raise ValueError("Production DB binding missing or ambiguous")
    production_db = prod[0]
    if production_db.get("binding") != "DB" or not UUID.fullmatch(
            str(production_db.get("database_id", ""))):
        raise ValueError("Production DB binding invalid")

    if "previews" not in config:
        return "PREVIEW_NOT_CONFIGURED"
    preview = config["previews"]
    if not isinstance(preview, dict):
        raise ValueError("Preview configuration invalid")
    forbidden = set(preview) - ALLOWED_PREVIEW_FIELDS
    if forbidden:
        raise ValueError("Unreviewed Preview bindings or production routes")
    dbs = preview.get("d1_databases")
    if not isinstance(dbs, list) or len(dbs) != 1:
        raise ValueError("Preview requires exactly one separate DB binding")
    db = dbs[0]
    if db.get("binding") != "DB" or not UUID.fullmatch(
            str(db.get("database_id", ""))):
        raise ValueError("Preview DB identity invalid or placeholder")
    if db["database_id"].lower() == production_db["database_id"].lower():
        raise ValueError("Preview must not bind production D1")
    if not db.get("database_name") or db.get("database_name") == production_db.get("database_name"):
        raise ValueError("Preview requires separate D1 name")
    # Preview D1 migrations are applied using a separate top-level Wrangler
    # migration config; this optional hint is NOT proof of remote migrations.
    if "migrations_dir" in db and db["migrations_dir"] != production_db.get("migrations_dir"):
        raise ValueError("Preview migration directory must match schema")
    vars_ = preview.get("vars")
    if not isinstance(vars_, dict) or not (vars_.get("ANYTV_DEVICE_ACTIVATION_ENABLED") is False or
                                          vars_.get("ANYTV_DEVICE_ACTIVATION_ENABLED") == "false"):
        raise ValueError("Preview device activation must be explicitly disabled")
    # The actual Worker checks literal string \"true\", so \"false\" is a safe
    # explicit Wrangler var, and boolean false is also non-activating.
    if vars_.get("ANYTV_RESOLVER_AUTH_MODE") != "pilot":
        raise ValueError("Preview resolver auth mode must be explicit pilot")
    if set(vars_) - ALLOWED_PREVIEW_VARS:
        raise ValueError("Secret or unreviewed plaintext Preview var")
    return "PREVIEW_STATIC_ISOLATION_OK"


def load_current():
    raw = CONFIG.read_text(encoding="utf-8")
    # Existing checked-in JSONC uses full-line comments only. Fail closed
    # on other syntax rather than loosely deleting arbitrary // in strings.
    raw = re.sub(r"(?m)^[ \t]*//[^\n]*(?:\n|$)", "", raw)
    return json.loads(raw)


class PreviewIsolationContract(unittest.TestCase):
    def setUp(self):
        self.base = load_current()
        self.preview_id = "11111111-2222-4333-8444-555555555555"

    def with_preview(self):
        c = copy.deepcopy(self.base)
        c["previews"] = {
            "d1_databases": [{
                "binding": "DB",
                "database_name": "isolated-test-d1",
                "database_id": self.preview_id,
                "migrations_dir": c["d1_databases"][0]["migrations_dir"],
            }],
            "vars": {"ANYTV_DEVICE_ACTIVATION_ENABLED": "false",
                     "ANYTV_RESOLVER_AUTH_MODE": "pilot"},
        }
        return c

    def test_checked_in_config_meets_static_gate(self):
        self.assertIn(validate_config(self.base),
                      ("PREVIEW_NOT_CONFIGURED", "PREVIEW_STATIC_ISOLATION_OK"))

    def test_future_separate_preview_static_config_passes(self):
        self.assertEqual(validate_config(self.with_preview()), "PREVIEW_STATIC_ISOLATION_OK")

    def test_rejects_production_database_id(self):
        c = self.with_preview()
        c["previews"]["d1_databases"][0]["database_id"] = c["d1_databases"][0]["database_id"]
        with self.assertRaisesRegex(ValueError, "production D1"):
            validate_config(c)

    def test_rejects_missing_preview_db(self):
        c = self.with_preview()
        del c["previews"]["d1_databases"]
        with self.assertRaisesRegex(ValueError, "separate DB"):
            validate_config(c)

    def test_rejects_enabled_or_missing_activation_gate(self):
        for value in (True, "true", None, "0", 0):
            c = self.with_preview()
            if value is None:
                del c["previews"]["vars"]["ANYTV_DEVICE_ACTIVATION_ENABLED"]
            else:
                c["previews"]["vars"]["ANYTV_DEVICE_ACTIVATION_ENABLED"] = value
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "activation"):
                validate_config(c)

    def test_accepts_boolean_false_but_rejects_unreviewed_vars(self):
        c = self.with_preview()
        c["previews"]["vars"]["ANYTV_DEVICE_ACTIVATION_ENABLED"] = False
        self.assertEqual(validate_config(c), "PREVIEW_STATIC_ISOLATION_OK")
        c["previews"]["vars"]["NEW_SERVICE_TOKEN"] = "not-allowed"
        with self.assertRaisesRegex(ValueError, "Secret"):
            validate_config(c)

    def test_rejects_missing_auth_mode_and_other_bindings(self):
        c = self.with_preview()
        del c["previews"]["vars"]["ANYTV_RESOLVER_AUTH_MODE"]
        with self.assertRaisesRegex(ValueError, "auth mode"):
            validate_config(c)
        for field in ("services", "kv_namespaces", "r2_buckets", "queues"):
            c = self.with_preview()
            c["previews"][field] = []
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, "Unreviewed"):
                validate_config(c)

    def test_rejects_plaintext_credentials_and_routes(self):
        c = self.with_preview()
        c["previews"]["vars"]["TRAKT_CLIENT_ID"] = "forbidden"
        with self.assertRaisesRegex(ValueError, "Secret"):
            validate_config(c)
        c = self.with_preview()
        c["previews"]["routes"] = [{"pattern": "example.com/*"}]
        with self.assertRaisesRegex(ValueError, "routes"):
            validate_config(c)

    def test_rejects_unknown_db_identity_or_migration_path(self):
        c = self.with_preview()
        c["previews"]["d1_databases"][0]["database_id"] = "REPLACE_WITH_UUID"
        with self.assertRaisesRegex(ValueError, "invalid"):
            validate_config(c)
        c = self.with_preview()
        c["previews"]["d1_databases"][0]["migrations_dir"] = "other"
        with self.assertRaisesRegex(ValueError, "migration"):
            validate_config(c)


if __name__ == "__main__":
    unittest.main()
