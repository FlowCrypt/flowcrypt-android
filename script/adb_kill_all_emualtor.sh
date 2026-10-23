#!/usr/bin/env bash
#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

echo "Killing running emulators..."
adb devices | awk '/emulator-/{print $1}' | while read -r line; do
  echo "  Killing $line..."
  adb -s "$line" emu kill 2>/dev/null || true
done
