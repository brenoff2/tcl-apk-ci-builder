import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SQL = ROOT / "supabase/migrations/202610310001_anytv_library.sql"

class LibraryRlsContract(unittest.TestCase):
    def test_isolation_and_guest_mode(self):
        src = SQL.read_text(encoding="utf-8").lower()
        self.assertIn("references auth.users(id)", src)
        self.assertIn("enable row level security", src)
        self.assertIn("revoke all on public.anytv_library_items from anon", src)
        self.assertIn("grant select, insert, update", src)
        self.assertEqual(src.count("(select auth.uid()) = user_id"), 4)
        self.assertIn("primary key (user_id, kind, item_key)", src)
        self.assertIn("deleted boolean not null default false", src)
        self.assertNotIn("grant delete", src)
        self.assertNotIn("service_role", src)

if __name__ == "__main__":
    unittest.main()
