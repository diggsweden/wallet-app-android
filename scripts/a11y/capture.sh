#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

# Runs the a11y capture tests on a running emulator (see emulator.sh) and pulls the output to
# app/build/a11y-snapshots. Uses `am instrument` rather than connectedAndroidTest, which would
# uninstall the app, and the captures in its files dir, before they can be pulled.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

APP_ID="se.digg.wallet.demo"
RUNNER="$APP_ID.test/androidx.test.runner.AndroidJUnitRunner"
DEVICE_DIR="files/a11y-snapshots"
OUT="app/build/a11y-snapshots"
# Must match the screen emulator.sh gives the AVD (Pixel 9-like, 412x915 dp).
SIZE="1236x2745"
DENSITY="480"

find_sdk() {
  for d in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" \
    "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/AppData/Local/Android/Sdk"; do
    [ -n "$d" ] && [ -d "$d/platform-tools" ] && {
      echo "$d"
      return
    }
  done
  echo "Android SDK not found. Set ANDROID_HOME." >&2
  exit 1
}

ANDROID_HOME="$(find_sdk)"
export ANDROID_HOME
ADB="$ANDROID_HOME/platform-tools/adb"

# Gradle installs on every attached device unless ANDROID_SERIAL narrows it down.
if [ -z "${ANDROID_SERIAL:-}" ]; then
  ANDROID_SERIAL="$("$ADB" devices | grep -oE '^emulator-[0-9]+' | head -1 || true)"
  [ -n "$ANDROID_SERIAL" ] || {
    echo "No emulator running. Start one with scripts/a11y/emulator.sh boot." >&2
    exit 1
  }
fi
export ANDROID_SERIAL

restore() {
  "$ADB" shell cmd uimode night no >/dev/null 2>&1 || true
}
trap restore EXIT

# The screen comes from the AVD, not a `wm size` override: an override with a different aspect
# ratio than the panel left the app a letterboxed window. Leftovers from such overrides go too.
"$ADB" shell wm size reset
"$ADB" shell wm density reset
size="$("$ADB" shell wm size | tr -d '\r' | sed -n 's/^Physical size: //p')"
density="$("$ADB" shell wm density | tr -d '\r' | sed -n 's/^Physical density: //p')"
[ "$size" = "$SIZE" ] && [ "$density" = "$DENSITY" ] || {
  echo "Emulator screen is ${size:-?} at ${density:-?} dpi, expected $SIZE at $DENSITY dpi." >&2
  echo "Restart it so it picks up the AVD settings: just a11y-emulator-stop && just a11y-review" >&2
  exit 1
}

./gradlew :app:installDemoDebug :app:installDemoDebugAndroidTest

"$ADB" shell run-as "$APP_ID" rm -rf "$DEVICE_DIR"

# am instrument exits 0 even when tests fail, so the verdict comes from its output.
log="$("$ADB" shell am instrument -w -e package se.digg.wallet.a11y "$RUNNER" | tr -d '\r')"
echo "$log"
grep -q '^OK (' <<<"$log" || {
  echo "a11y capture tests failed." >&2
  exit 1
}

rm -rf "$OUT"
mkdir -p "$OUT"
"$ADB" exec-out run-as "$APP_ID" tar -cf - -C "$DEVICE_DIR" . | tar -xf - -C "$OUT"
echo "Pulled $(find "$OUT" -type f | wc -l) files to $OUT"
