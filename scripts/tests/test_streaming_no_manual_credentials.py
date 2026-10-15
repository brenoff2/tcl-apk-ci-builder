#!/usr/bin/env python3
"""P0: prohibit shipping streaming UI that asks end users for credentials.

This test deliberately fails against the existing token-based prototype until
the client uses a dedicated server-side resolver/catalogue. Do not weaken it.
"""
from pathlib import Path
import re
import unittest

ROOT = Path("overrides")
SCREEN = ROOT / "TclStreamingScreens.kt"
PERSONAL = ROOT / "AnyTvPersonalLinks.kt"

class StreamingNoManualCredentials(unittest.TestCase):
    def test_no_user_entered_tokens(self):
        code = SCREEN.read_text(encoding="utf-8")
        banned = (
            "Token TMDB",
            "Obter token",
            "Chave de streaming (opcional)",
            "Configurar catálogo",
            'prefs.getString("tmdb_token"',
            'prefs.edit().putString("tmdb_token"',
            "personalKeyDraft",
            "tokenDraft",
            "editToken",
            "Informe seu token de leitura TMDB",
        )
        for marker in banned:
            with self.subTest(marker=marker):
                self.assertNotIn(marker, code, "P0: streaming still requires manual API/token setup")

    def test_no_direct_api_secret_sent_from_phone(self):
        screen = SCREEN.read_text(encoding="utf-8")
        personal = PERSONAL.read_text(encoding="utf-8")
        self.assertNotIn('setRequestProperty("Authorization", "Bearer $token")', screen)
        self.assertNotIn('setRequestProperty("X-API-Key", apiKey)', personal)
        self.assertNotIn("Configure sua chave da API de streaming", personal)

if __name__ == "__main__":
    unittest.main()
