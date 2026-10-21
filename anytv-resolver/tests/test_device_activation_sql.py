"""SQLite-level test of D1-compatible activation SQL. Never opens remote D1."""
import pathlib
import sqlite3
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCHEMA = ("0002_device_proof.sql", "0003_device_activations.sql")

# Same SQL as device-activation.mjs; tests real SQLite execution/constraints.
INSERT = """INSERT INTO device_keys(device_id,public_key,enabled,expires_at)
SELECT ?,?,1,valid_until FROM device_activations
WHERE code_hash=? AND used_at IS NULL AND expires_at>? AND valid_until>?
ON CONFLICT(device_id) DO NOTHING"""
CONSUME = """UPDATE device_activations SET used_at=?,device_id=?
WHERE code_hash=? AND used_at IS NULL AND expires_at>?
AND EXISTS(SELECT 1 FROM device_keys WHERE device_id=? AND public_key=?)"""

class DeviceActivationSql(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.db = sqlite3.connect(pathlib.Path(self.temp.name) / "devices.db")
        self.addCleanup(self.db.close)
        for filename in SCHEMA:
            self.db.executescript((ROOT / "migrations" / filename).read_text())
        self.now = 1792530000
        self.hash = "h" * 43
        self.public = "p256:" + "A" * 87
        self.db.execute(
            "INSERT INTO device_activations(code_hash,expires_at,valid_until) VALUES (?,?,?)",
            (self.hash, self.now + 600, self.now + 2592000))
        self.db.commit()

    def activate(self, device):
        with self.db:
            inserted = self.db.execute(INSERT, (
                device, self.public, self.hash, self.now, self.now)).rowcount
            consumed = self.db.execute(CONSUME, (
                self.now, device, self.hash, self.now, device, self.public)).rowcount
            # D1 batch is atomic on SQL errors, not on zero changes.
            # Roll back zero-change partial operations explicitly.
            if inserted != 1 or consumed != 1:
                raise ValueError("activation denied")

    def test_single_activation_and_replay(self):
        self.activate("device-a")
        with self.assertRaises(ValueError):
            self.activate("device-b")
        self.assertEqual(self.db.execute("SELECT count(*) FROM device_keys").fetchone()[0], 1)
        self.assertEqual(self.db.execute(
            "SELECT device_id FROM device_activations").fetchone()[0], "device-a")

    def test_expired_code_does_not_create_device(self):
        self.db.execute("UPDATE device_activations SET expires_at=?",
                        (self.now - 1,))
        self.db.commit()
        with self.assertRaises(ValueError):
            self.activate("device-a")
        self.assertEqual(self.db.execute("SELECT count(*) FROM device_keys").fetchone()[0], 0)

    def test_existing_key_collision_does_not_consume_code(self):
        self.db.execute(
            "INSERT INTO device_keys(device_id,public_key,enabled,expires_at) VALUES (?,?,1,?)",
            ("device-a", self.public, self.now + 1000))
        self.db.commit()
        with self.assertRaises(ValueError):
            self.activate("device-a")
        self.assertIsNone(self.db.execute(
            "SELECT used_at FROM device_activations").fetchone()[0])

if __name__ == "__main__":
    unittest.main()
