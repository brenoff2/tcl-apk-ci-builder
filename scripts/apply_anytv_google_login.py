#!/usr/bin/env python3
"""Integrate the existing AnyTV Google OAuth flow into the pinned Android app.

Public Supabase endpoint settings come from release/anytv-auth-public.json.
An empty pair keeps the existing guest app working without showing fake login.
No Google client secret, service-role key, signer, updater or OTA is changed.
"""
import json
from pathlib import Path
from urllib.parse import urlparse
from xml.sax.saxutils import escape
import xml.etree.ElementTree as ET


def replace_once(source: str, old: str, new: str, what: str) -> str:
    if source.count(old) != 1:
        raise RuntimeError(f"{what}: expected exactly one anchor; got {source.count(old)}")
    return source.replace(old, new, 1)


config = json.loads(Path("release/anytv-auth-public.json").read_text(encoding="utf-8"))
if set(config) != {"apiUrl", "publishableKey"}:
    raise RuntimeError("AnyTV auth public config must contain only apiUrl and publishableKey")
url, key = config["apiUrl"], config["publishableKey"]
if not isinstance(url, str) or not isinstance(key, str):
    raise RuntimeError("AnyTV public auth settings must be strings")
if bool(url) != bool(key):
    raise RuntimeError("AnyTV auth URL and public key must be supplied together")
if url:
    parsed = urlparse(url)
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username
            or parsed.password or parsed.query or parsed.fragment or len(key) < 20):
        raise RuntimeError("AnyTV public auth settings require HTTPS and a publishable key")
    if not key.startswith("sb_publishable_"):
        raise RuntimeError("AnyTV must embed only a public Supabase publishable key")

values = Path("source/app/src/main/res/values/anytv_auth_public.xml")
values.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<resources>\n'
    f'    <string name="anytv_auth_public_url" translatable="false">{escape(url)}</string>\n'
    f'    <string name="anytv_auth_public_key" translatable="false">{escape(key)}</string>\n'
    '</resources>\n', encoding="utf-8")
ET.parse(values)

main_file = Path("source/app/src/main/java/com/hari/androidtvremote/MainActivity.kt")
main = main_file.read_text(encoding="utf-8")
main = replace_once(main, "import android.content.Context\n",
    "import android.content.Context\nimport android.content.Intent\n", "MainActivity imports")
main = replace_once(main, "import androidx.compose.runtime.CompositionLocalProvider\n",
    "import androidx.compose.runtime.CompositionLocalProvider\n"
    "import androidx.compose.runtime.Composable\n"
    "import com.hari.androidtvremote.ui.app.AnyTvAccountFlow\n"
    "import com.hari.androidtvremote.ui.app.AnyTvPublicAuthConfig\n"
    "import com.hari.androidtvremote.ui.app.AnyTvSupabaseAuthGateway\n"
    "import com.hari.androidtvremote.ui.app.createAnyTvAuthClient\n",
    "MainActivity Compose/Auth imports")
main = replace_once(main, "class MainActivity : ComponentActivity() {\n",
    """class MainActivity : ComponentActivity() {

    // A Google login is only offered after an independent AnyTV Auth endpoint
    // and its public client key have been configured and verified.
    private val anyTvGoogleGateway: AnyTvSupabaseAuthGateway? by lazy {
        val url = getString(R.string.anytv_auth_public_url)
        val key = getString(R.string.anytv_auth_public_key)
        if (url.isBlank() || key.isBlank()) null
        else AnyTvSupabaseAuthGateway(
            createAnyTvAuthClient(AnyTvPublicAuthConfig(url, key))
        )
    }
""", "MainActivity class")
main = replace_once(main, "        super.onCreate(savedInstanceState)\n",
    "        super.onCreate(savedInstanceState)\n"
    "        anyTvGoogleGateway?.handleAuthCallback(intent)\n",
    "MainActivity OAuth onCreate")
main = replace_once(main,
    "                AndroidTVRemoteTheme {\n                    AppNavGraph(",
    "                AndroidTVRemoteTheme {\n"
    "                    val existingApp: @Composable () -> Unit = {\n"
    "                        AppNavGraph(",
    "MainActivity existing app wrapper")
main = replace_once(main,
    "                    )\n                }\n            }\n        }\n    }\n\n    override fun onResume()",
    """                    )
                    }
                    val gateway = anyTvGoogleGateway
                    AnyTvAccountFlow(gateway = gateway, existingApp = existingApp)
                }
            }
        }
    }

    // A running activity receives the browser callback without losing navigation.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        anyTvGoogleGateway?.handleAuthCallback(intent)
    }

    override fun onResume()""",
    "MainActivity account flow")

main_file.write_text(main, encoding="utf-8")
manifest_file = Path("source/app/src/main/AndroidManifest.xml")
manifest = manifest_file.read_text(encoding="utf-8")
manifest = replace_once(manifest,
    '            android:name=".MainActivity"\n',
    '            android:name=".MainActivity"\n'
    '            android:launchMode="singleTop"\n',
    "MainActivity launch mode")
manifest = replace_once(manifest,
    '        </activity>\n',
    """            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="com.tclremote.livre.v2" android:host="auth" />
            </intent-filter>
        </activity>
""", "MainActivity OAuth deep link")
ET.fromstring(manifest)
manifest_file.write_text(manifest, encoding="utf-8")
print("ANYTV_GOOGLE_PKCE_ANDROID_WIRING_READY")
