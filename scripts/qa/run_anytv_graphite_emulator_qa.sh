#!/usr/bin/env bash
set -euo pipefail
cd source
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace
APP_APK=app/build/outputs/apk/debug/app-debug.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
test -s "$APP_APK"
test -s "$TEST_APK"
adb install -r "$APP_APK"
adb install -r "$TEST_APK"
echo ANYTV_EMULATOR_PACKAGES_BEGIN
adb shell pm list packages -3
echo ANYTV_EMULATOR_PACKAGES_END
# Use instrument package registered by Android, never a guessed release package.
INSTRUMENTATION=$(adb shell pm list instrumentation | tr -d '\r' | sed -n 's/^instrumentation:\([^ ]*\).*/\1/p' | grep -E 'tclremote|androidtvremote' | head -1)
test -n "$INSTRUMENTATION"
echo "ANYTV_INSTRUMENTATION=$INSTRUMENTATION"
adb shell am instrument -w -r -e class com.hari.androidtvremote.ui.app.AnyTvGraphiteVisualQaTest "$INSTRUMENTATION" | tee /tmp/anytv-qa-instrumentation.txt
grep -q 'OK (1 test)' /tmp/anytv-qa-instrumentation.txt
# Read screenshot before app is uninstalled by Gradle cleanup.
APP=$(adb shell pm list packages -3 | tr -d '\r' | sed -n 's/^package://p' | grep -E 'tclremote|androidtvremote' | grep -v 'test' | head -1)
test -n "$APP"
echo "ANYTV_EMULATOR_TARGET_PACKAGE=$APP"
adb shell run-as "$APP" ls files/anytv-code122-graphite-real-compose.png
cd ..
mkdir -p visual-qa
adb exec-out run-as "$APP" cat files/anytv-code122-graphite-real-compose.png > visual-qa/graphite-code122-compose.png
test -s visual-qa/graphite-code122-compose.png
file visual-qa/graphite-code122-compose.png
echo ANYTV_CODE122_REAL_COMPOSE_SCREENSHOT_CAPTURED
