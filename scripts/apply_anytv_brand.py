#!/usr/bin/env python3
"""AnyTV display branding, without changing app ids or Android signing identity."""
from pathlib import Path
phone = Path("source/app/src/main/res/values/strings.xml")
s=phone.read_text(encoding="utf-8")
assert '<string name="app_name">Controle TCL 1.1</string>' in s
phone.write_text(s.replace('<string name="app_name">Controle TCL 1.1</string>','<string name="app_name">AnyTV</string>'),encoding="utf-8")
screen=Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/SettingsScreen.kt")
t=screen.read_text(encoding="utf-8")
t=t.replace('Controle TCL Livre','AnyTV')
screen.write_text(t,encoding="utf-8")
print("ANYTV_PHONE_DISPLAY_NAME_READY")
