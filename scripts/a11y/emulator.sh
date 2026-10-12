#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

# Headless emulator for the a11y review: emulator.sh {boot [avd]|stop}
# Copied from scripts/hsm-e2e/emulator.sh on the test/hsm-e2e branch.

set -euo pipefail

API="${ANDROID_API:-34}"
AVD="${2:-a11y}"
# Pixel 9-like screen (412x915 dp at xxhdpi). capture.sh checks the running emulator matches.
LCD_WIDTH=1236
LCD_HEIGHT=2745
LCD_DENSITY=480

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

SDK="$(find_sdk)"
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"

# Newer cmdline-tools (the "Android CLI") create AVDs under the XDG config dir, while the
# emulator only looks in ~/.android/avd. Pinning ANDROID_AVD_HOME makes both use one dir:
# the classic one if it exists, else the XDG one.
if [ -z "${ANDROID_AVD_HOME:-}" ]; then
  ANDROID_AVD_HOME="${XDG_CONFIG_HOME:-$HOME/.config}/.android/avd"
  [ -d "$HOME/.android/avd" ] && ANDROID_AVD_HOME="$HOME/.android/avd"
fi
export ANDROID_AVD_HOME
mkdir -p "$ANDROID_AVD_HOME"

cmdline_bin() {
  for d in "$SDK/cmdline-tools/latest/bin" "$SDK/cmdline-tools"/*/bin "$SDK/tools/bin"; do
    [ -x "$d/sdkmanager" ] && {
      echo "$d"
      return
    }
  done
  echo ""
}

host_abi() {
  case "$(uname -m)" in
  arm64 | aarch64) echo "arm64-v8a" ;;
  *) echo "x86_64" ;;
  esac
}

# google_apis (not google_play) images, as on the test/hsm-e2e branch.
ensure_avd() {
  local avds bin img requested="$AVD"
  avds="$("$EMU" -list-avds 2>/dev/null || true)"

  # No fallback to another AVD: its screen size would change what the report shows.
  if echo "$avds" | grep -qx "$AVD"; then
    configure_avd
    return
  fi

  img="system-images;android-${API};google_apis;$(host_abi)"
  bin="$(cmdline_bin)"
  [ -n "$bin" ] || {
    echo "No AVDs found and no cmdline-tools to create one. Run:" >&2
    echo "  sdkmanager '$img'" >&2
    echo "  avdmanager create avd -n '$requested' -k '$img'" >&2
    exit 1
  }
  echo "creating AVD '$AVD' ($img)..."
  { yes || true; } | "$bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  { yes || true; } | "$bin/sdkmanager" "$img" >/dev/null
  printf 'no\n' | "$bin/avdmanager" create avd --force -n "$AVD" -k "$img"
  configure_avd
}

# avdmanager's default hardware is a 320x640 mdpi screen. Applied on every boot, so an AVD
# created by an earlier version of this script gets the right size too.
configure_avd() {
  local ini="$ANDROID_AVD_HOME/$AVD.avd/config.ini"
  [ -f "$ini" ] || {
    echo "AVD config not found: $ini" >&2
    exit 1
  }
  set_ini "$ini" hw.lcd.width "$LCD_WIDTH"
  set_ini "$ini" hw.lcd.height "$LCD_HEIGHT"
  set_ini "$ini" hw.lcd.density "$LCD_DENSITY"
}

# Portable replacement for `sed -i` (differs between GNU and BSD).
set_ini() {
  local tmp
  tmp="$(mktemp)"
  grep -v "^$2=" "$1" >"$tmp" || true
  echo "$2=$3" >>"$tmp"
  cat "$tmp" >"$1"
  rm -f "$tmp"
}

# Citrix App Protection preloads a lib that segfaults the emulator.
citrix_guard() {
  grep -qiE '^[^#]*AppProtection' /etc/ld.so.preload 2>/dev/null || return 0
  cat >&2 <<'EOF'
Citrix App Protection is active — it segfaults the Android emulator.
Comment its line out of /etc/ld.so.preload for the run:

  sudo sed -i.bak -E 's|^([^#].*AppProtection.*)|#\1|' /etc/ld.so.preload
  sudo mv /etc/ld.so.preload.bak /etc/ld.so.preload   # afterwards
EOF
  exit 1
}

booted() {
  [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]
}

boot() {
  # Asks the device rather than trusting `adb devices`, which still lists an emulator that is
  # shutting down.
  if booted; then
    echo "emulator already running"
    return
  fi
  ensure_avd
  citrix_guard

  local log="${TMPDIR:-/tmp}/a11y-emulator.log"
  echo "booting '$AVD' (headless)... log: $log"
  nohup "$EMU" -avd "$AVD" -no-window -no-audio -no-boot-anim -no-snapshot -no-metrics \
    -gpu swiftshader_indirect >"$log" 2>&1 &
  local pid=$!

  local deadline=$(($(date +%s) + 300))
  until booted; do
    kill -0 "$pid" 2>/dev/null || {
      echo "emulator process died — see $log" >&2
      exit 1
    }
    [ "$(date +%s)" -lt "$deadline" ] || {
      echo "emulator boot timed out — see $log" >&2
      exit 1
    }
    sleep 3
  done
  "$ADB" shell input keyevent 82 >/dev/null 2>&1 || true
  echo "emulator ready."
}

stop() {
  [ -x "$ADB" ] || return 0
  for s in $("$ADB" devices | grep -oE '^emulator-[0-9]+'); do
    "$ADB" -s "$s" emu kill >/dev/null && echo "stopping $s..."
  done
  # `emu kill` returns before the emulator has gone; a boot started right after would
  # otherwise mistake it for a running one.
  local deadline=$(($(date +%s) + 60))
  while "$ADB" devices | grep -qE '^emulator-[0-9]+'; do
    [ "$(date +%s)" -lt "$deadline" ] || {
      echo "emulator did not stop within 60 s" >&2
      exit 1
    }
    sleep 1
  done
  echo "stopped."
}

case "${1:-boot}" in
boot) boot ;;
stop) stop ;;
*)
  echo "usage: $0 {boot [avd]|stop}" >&2
  exit 2
  ;;
esac
