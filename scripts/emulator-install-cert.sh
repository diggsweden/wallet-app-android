#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

set -euo pipefail

if ! adb -e get-state >/dev/null 2>&1; then
  echo "No Android emulator running."
  exit 1
fi

DEFAULT="$(mkcert -CAROOT)/rootCA.pem"
CERT_PEM="${1:-$DEFAULT}"

adb push "$CERT_PEM" /sdcard/Download/

adb shell am start -n com.google.android.settings.intelligence/.modules.search.SearchActivity >/dev/null 2>&1 || true
sleep 1
adb shell input text "certificate"

echo "Certificate copied to /sdcard/Download/$CERT_PEM on the emulator."
echo "Tap 'CA certificate', then select $CERT_PEM from Downloads."
