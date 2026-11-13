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
# Adaptive launcher resources override the pinned upstream drawables.
adaptive = res / "mipmap-anydpi-v26"
adaptive.mkdir(parents=True, exist_ok=True)
drawables = res / "drawable"
drawables.mkdir(parents=True, exist_ok=True)
(drawables / "ic_launcher_background.xml").write_text(
    '<shape xmlns:android="http://schemas.android.com/apk/res/android">'
    '<solid android:color="#000000"/></shape>', encoding="utf-8")
adaptive_xml = ('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">'
                '<background android:drawable="@drawable/ic_launcher_background"/>'
                '<foreground android:drawable="@mipmap/ic_launcher_foreground"/>'
                '<monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>'
                '</adaptive-icon>')
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    (adaptive / name).write_text(adaptive_xml, encoding="utf-8")
for qualifier, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    canvas = size * 108 // 48
    sample = art.resize((int(canvas * .72), int(canvas * .72)), Image.Resampling.LANCZOS)
    silhouette = Image.new("L", sample.size)
    silhouette.putdata([
        a if max(r,g,b) >= 55 else 0
        for r,g,b,a in sample.getdata()
    ])
    alpha = Image.new("L", (canvas,canvas), 0)
    alpha.paste(silhouette, ((canvas-sample.width)//2,(canvas-sample.height)//2))
    mono = Image.new("RGBA",(canvas,canvas),(255,255,255,255))
    mono.putalpha(alpha)
    mono.save(res / ("mipmap-"+qualifier) / "ic_launcher_monochrome.png", "PNG", optimize=True)
drawable = res/"drawable-nodpi"
drawable.mkdir(parents=True, exist_ok=True)
copyfile(icon_path, drawable/"anytv_icon.png")
copyfile(logo_path, drawable/"anytv_wordmark.png")
copyfile("assets/anytv_horizontal.png", drawable/"anytv_horizontal.png")
copyfile("overrides/AnyTvSplashScreen.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/SplashScreen.kt")
# Prepare guest-first account entry; activate the route only with live auth callbacks.
copyfile("overrides/AnyTvAccountEntry.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvAccountEntry.kt")
copyfile("overrides/AnyTvSignInScreen.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvSignInScreen.kt")
copyfile("overrides/AnyTvAccountSession.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvAccountSession.kt")
copyfile("overrides/AnyTvEntryHost.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvEntryHost.kt")
copyfile("overrides/AnyTvSupabaseAuthGateway.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvSupabaseAuthGateway.kt")
copyfile("overrides/AnyTvAccountFlow.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvAccountFlow.kt")
copyfile("overrides/AnyTvAuthClientFactory.kt", "source/app/src/main/java/com/hari/androidtvremote/ui/app/AnyTvAuthClientFactory.kt")
# Install the Google-only OAuth callback after copying the approved overlays.
import runpy
runpy.run_path("scripts/apply_anytv_google_login.py", run_name="__main__")
# Keep the supplied brand palette consistent on all devices, including when
# Android changes wallpaper/dynamic colors. Preserve user data and package.
theme = Path("source/app/src/main/java/com/hari/androidtvremote/ui/theme/Theme.kt")
theme_source = theme.read_text(encoding="utf-8")
assert theme_source.count(
    "colorScheme = if (useDark) dynamicDarkColorScheme() else dynamicLightColorScheme(),"
) == 1, "Upstream theme changed: check before applying brand"
theme_source = theme_source.replace(
    "import androidx.compose.material3.MaterialTheme",
    "import androidx.compose.material3.MaterialTheme\n"
    "import androidx.compose.material3.darkColorScheme\n"
    "import androidx.compose.ui.graphics.Color"
)
theme_source = theme_source.replace(
    "colorScheme = if (useDark) dynamicDarkColorScheme() else dynamicLightColorScheme(),",
    "colorScheme = anyTvRedDarkScheme,"
)
theme_source += """
private val anyTvRedDarkScheme = darkColorScheme(
    // Every screen uses one matte graphite substrate; red selection is Streaming-only.
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF242424),
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color(0xFFF4F4F4),
    secondary = Color(0xFFC8C8C8),
    onSecondary = Color(0xFF242424),
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFF4F4F4),
    tertiary = Color(0xFFBDBDBD),
    onTertiary = Color(0xFF242424),
    background = Color(0xFF242424),
    onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF242424),
    onSurface = Color(0xFFF4F4F4),
    surfaceVariant = Color(0xFF242424),
    surfaceContainerLowest = Color(0xFF242424),
    surfaceContainerLow = Color(0xFF242424),
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF272727),
    surfaceContainerHighest = Color(0xFF292929),
    onSurfaceVariant = Color(0xFFC9C9C9),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF111111),
    error = Color(0xFFFF637A)
)
"""
theme.write_text(theme_source, encoding="utf-8")

gradle = Path("source/app/build.gradle.kts")
cfg = gradle.read_text(encoding="utf-8")
# Official SDK pinned to upstream 3.8.0; OAuth secrets are never shipped.
assert 'dependencies {' in cfg, 'Gradle dependency block unavailable'
cfg = cfg.replace('dependencies {', 'dependencies {\n    implementation("io.github.jan-tennert.supabase:auth-kt:3.8.0")\n    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.8.0")\n    implementation("io.ktor:ktor-client-okhttp:3.3.1")', 1)
assert cfg.count("versionCode = 23") == 1 and cfg.count('versionName = "2.3-TCL"') == 1
cfg = cfg.replace("versionCode = 23","versionCode = 34").replace('versionName = "2.3-TCL"','versionName = "2.9.5-AnyTV"')
gradle.write_text(cfg, encoding="utf-8")
print("ANYTV_BRAND_ICON_WORDMARK_VERSION31_READY")

print("ANYTV_PHONE_DISPLAY_NAME_READY")
