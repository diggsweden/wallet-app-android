#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

# Non-interactive variant of emulator-install-cert.sh: installs a CA as a user
# certificate via `adb root` (google_apis images only).

set -euo pipefail

CERT_PEM="${1:-$(mkcert -CAROOT)/rootCA.pem}"
DIR=/data/misc/user/0/cacerts-added

adb -e get-state >/dev/null 2>&1 || {
  echo "No Android emulator running." >&2
  exit 1
}

hash="$(openssl x509 -inform PEM -subject_hash_old -noout -in "$CERT_PEM")"

adb root >/dev/null
adb wait-for-device
adb shell mkdir -p "$DIR"
adb push "$CERT_PEM" "$DIR/$hash.0" >/dev/null
adb shell "chown system:system $DIR $DIR/$hash.0 && chmod 644 $DIR/$hash.0 && restorecon -R $DIR"

echo "Installed $CERT_PEM as user CA $hash.0"
