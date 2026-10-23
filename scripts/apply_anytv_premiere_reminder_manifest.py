#!/usr/bin/env python3
"""Declare the local, inexact Android premiere notification receiver.

This changes ONLY the new AnyTV phone app's generated manifest. No TV companion,
remote pairing code, cloud token or installer signature is modified.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

manifest = Path("source/app/src/main/AndroidManifest.xml")
source = manifest.read_text(encoding="utf-8")
permission = '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'
receiver = (
    '<receiver android:name="com.hari.androidtvremote.ui.app.AnyTvReleaseReminderReceiver"'
    ' android:enabled="true" android:exported="false" />'
)
if source.count("</application>") != 1:
    raise SystemExit("ANYTV_REMINDER_MANIFEST_APPLICATION_ANCHOR_INVALID")
if source.count("<application") != 1:
    raise SystemExit("ANYTV_REMINDER_MANIFEST_APPLICATION_COUNT_INVALID")
if permission not in source:
    source = source.replace("<application", permission + "\n    <application", 1)
if receiver not in source:
    source = source.replace("</application>", "    " + receiver + "\n    </application>", 1)
ET.fromstring(source)
manifest.write_text(source, encoding="utf-8")
print("ANYTV_PREMIERE_REMINDER_LOCAL_RECEIVER_DECLARED")
