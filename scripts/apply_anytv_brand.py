#!/usr/bin/env python3
"""AnyTV display branding, without changing app ids or Android signing identity."""
from pathlib import Path
phone = Path("source/app/src/main/res/values/strings.xml")
s=phone.read_text(encoding="utf-8")
import re
s, count = re.subn(r'(<string name="app_name">)[^<]*(</string>)', r'\g<1>AnyTV\g<2>', s)
assert count == 1, count
phone.write_text(s, encoding="utf-8")
screen=Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/SettingsScreen.kt")
t=screen.read_text(encoding="utf-8")
t=t.replace('Controle TCL Livre','AnyTV')
screen.write_text(t,encoding="utf-8")

# Final approved AnyTV identity assets; no external network at build time.
from PIL import Image
from shutil import copyfile
icon_path = Path("assets/anytv_icon.png")
logo_path = Path("assets/anytv_logo.png")
assert icon_path.is_file() and logo_path.is_file(), "Approved branding assets absent"
res = Path("source/app/src/main/res")
art = Image.open(icon_path).convert("RGBA")
for qualifier, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    target = res / ("mipmap-" + qualifier)
    target.mkdir(parents=True, exist_ok=True)
    square = art.resize((size,size),Image.Resampling.LANCZOS)
    for name in ("ic_launcher.png","ic_launcher_round.png"):
        square.save(target/name,"PNG",optimize=True)
    fg_size = size * 108 // 48
    foreground = Image.new("RGBA",(fg_size,fg_size),(0,0,0,0))
    inner = art.resize((int(fg_size*.72),int(fg_size*.72)),Image.Resampling.LANCZOS)
    foreground.alpha_composite(inner,((fg_size-inner.width)//2,(fg_size-inner.height)//2))
    foreground.save(target/"ic_launcher_foreground.png","PNG",optimize=True)
drawable = res/"drawable-nodpi"
drawable.mkdir(parents=True, exist_ok=True)
copyfile(icon_path, drawable/"anytv_icon.png")
copyfile(logo_path, drawable/"anytv_wordmark.png")
copyfile("overrides/AnyTvSplashScreen.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/SplashScreen.kt")
gradle = Path("source/app/build.gradle.kts")
cfg = gradle.read_text(encoding="utf-8")
assert cfg.count("versionCode = 23") == 1 and cfg.count('versionName = "2.3-TCL"') == 1
cfg = cfg.replace("versionCode = 23","versionCode = 31").replace('versionName = "2.3-TCL"','versionName = "2.9.2-AnyTV"')
gradle.write_text(cfg, encoding="utf-8")
print("ANYTV_BRAND_ICON_WORDMARK_VERSION31_READY")

print("ANYTV_PHONE_DISPLAY_NAME_READY")
