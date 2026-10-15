"""Offline tests for licensed source parser; no API calls or credentials."""
import unittest
from scripts.audit_movieofthenight import Quota, collect_offers, inspect_link


class AuditTests(unittest.TestCase):
    def test_no_secret_no_connection(self):
        with self.assertRaises(ValueError):
            Quota("")

    def test_api_call_limit(self):
        quota = Quota("unused-in-memory", limit=0)
        with self.assertRaises(RuntimeError):
            quota.get("/shows/top")

    def test_disney_entity(self):
        self.assertEqual(inspect_link(
            "disney", "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185"),
            "candidate")

    def test_prime_gti(self):
        self.assertEqual(inspect_link("prime",
            "https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef"),
            "candidate")

    def test_netflix_title(self):
        self.assertEqual(inspect_link("netflix", "https://www.netflix.com/title/80175798"),
                         "candidate")

    def test_reject_wrong_provider_host_and_credentials(self):
        self.assertEqual(inspect_link("netflix", "https://evil.example.com/title/80175798"),
                         "unsafe")
        self.assertEqual(inspect_link("netflix", "https://www.netflix.com@evil.com/title/80175798"),
                         "unsafe")
        self.assertEqual(inspect_link("prime", "http://app.primevideo.com/detail?gti=id"),
                         "unsafe")

    def test_reject_unknown_prime_query(self):
        self.assertEqual(inspect_link("prime",
             "https://app.primevideo.com/detail?url=https://evil.example"),
             "unknown_query")

    def test_region_and_addons_are_not_subscription(self):
        obj = {"streamingOptions": {"br": [
            {"service": {"id": "prime"}, "type": "addon",
             "link": "https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef"},
            {"service": {"id": "prime"}, "type": "rent",
             "link": "https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef"}]}}
        self.assertFalse(collect_offers(obj, "prime")["found"])

    def test_other_country_not_brazil(self):
        obj = {"streamingOptions": {"us": [{"service": {"id": "disney"}, "type": "subscription",
              "link": "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185"}]}}
        self.assertFalse(collect_offers(obj, "disney")["found"])

    def test_hbo_tracking_link_classification(self):
        self.assertEqual(inspect_link(
            "hbo", "https://play.hbomax.com/show/a0a085b8-6657-46ca-9476-6af0070b9cce?utm_source=universal_search"),
            "candidate_with_query")

    def test_no_untrusted_redirect(self):
        self.assertEqual(inspect_link(
            "disney", "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185?redirect=https://evil.com"),
            "unknown_query")


if __name__ == "__main__":
    unittest.main()
