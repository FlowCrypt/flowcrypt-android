#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_NAME="${AVD_NAME:-ci-emulator}"
EMULATOR_GPU_MODE="${EMULATOR_GPU_MODE:-swiftshader_indirect}"
EMULATOR_WIPE_DATA="${EMULATOR_WIPE_DATA:-1}"
EMULATOR_DNS_SERVER="${EMULATOR_DNS_SERVER:-127.0.0.1}"

"$ANDROID_HOME/emulator/emulator" -accel-check

emulator_args=(
  -avd "$AVD_NAME"
  -no-window
  -no-boot-anim
  -no-audio
  -no-snapshot
  -no-snapshot-load
  -no-snapshot-save
  -gpu "$EMULATOR_GPU_MODE"

  # This value is used by the emulator process on the host/container.
  # Android guest will still see 10.0.2.3 as DNS, but that proxy will forward
  # to the DNS server configured here.
  -dns-server "$EMULATOR_DNS_SERVER"

  -read-only
  -no-metrics
)

if [[ "$EMULATOR_WIPE_DATA" == "1" ]]; then
  emulator_args+=(-wipe-data)
fi

"$ANDROID_HOME/emulator/emulator" "${emulator_args[@]}" &

echo "Started emulator with DNS server: $EMULATOR_DNS_SERVER"
