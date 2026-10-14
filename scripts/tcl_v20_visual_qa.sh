#!/usr/bin/env bash
set -euo pipefail
mkdir -p screenshot-output

APK='qa-base/BASE_APP_2_0_REASSINAR_NAO_INSTALAR.apk'
test -s "$APK"
adb install -r -t "$APK"

# The compiled screenshot uses the same v2.0 UI code. The debug signer is
# disposable, on a fresh emulator ONLY; never distribute this QA APK.
# Bypass first-run onboarding by changing only the emulator's local preference.
adb shell am force-stop com.tclremote.livre || true
adb shell run-as com.tclremote.livre mkdir -p shared_prefs
cat <<'XML' | adb shell run-as com.tclremote.livre tee shared_prefs/android_tv_prefs.xml >/dev/null
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <boolean name="first_launch" value="false" />
</map>
XML

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell wm density 160

check_and_capture() {
  local name=$1
  local dims=$2
  local mode=$3
  adb shell wm size "$dims"
  adb shell cmd uimode night "$mode"
  adb shell am force-stop com.tclremote.livre || true
  adb shell am start -n com.tclremote.livre/com.hari.androidtvremote.MainActivity
  sleep 9
  adb shell screencap -p "/sdcard/$name.png"
  adb pull "/sdcard/$name.png" "screenshot-output/$name.png" >/dev/null
  test -s "screenshot-output/$name.png"
  adb shell uiautomator dump "/sdcard/$name.xml" >/dev/null 2>&1 || true
  adb pull "/sdcard/$name.xml" "screenshot-output/$name.xml" >/dev/null 2>&1 || true
  if ! adb shell pidof com.tclremote.livre >/dev/null; then
    echo "FAIL: app crashed on viewport $name" >&2
    exit 1
  fi
}

check_and_capture "tcl-320x640-dark" "320x640" yes
check_and_capture "tcl-393x852-dark" "393x852" yes
check_and_capture "tcl-393x852-light" "393x852" no
check_and_capture "tcl-480x960-light" "480x960" no

printf '%s\n' 'Four screenshot renders captured; visual signoff requires manual inspection.'
