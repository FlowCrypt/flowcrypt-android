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
EMULATOR_CORES="${EMULATOR_CORES:-2}"
EMULATOR_LOG_FILE="${EMULATOR_LOG_FILE:-$HOME/emulator.log}"
EMULATOR_PID_FILE="${EMULATOR_PID_FILE:-$HOME/emulator.pid}"

"$ANDROID_HOME/emulator/emulator" -accel-check
"$ANDROID_HOME/emulator/emulator" -version

emulator_args=(
  -avd "$AVD_NAME"
  -no-window
  -no-boot-anim
  -no-audio
  -no-snapshot
  -no-snapshot-load
  -no-snapshot-save
  -gpu "$EMULATOR_GPU_MODE"
  # f1-standard-2 has two vCPUs. Do not make a four-core guest compete for
  # more host CPUs than are available during instrumentation tests.
  -cores "$EMULATOR_CORES"

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

rm -f "$EMULATOR_PID_FILE"
: > "$EMULATOR_LOG_FILE"

"$ANDROID_HOME/emulator/emulator" "${emulator_args[@]}" \
  > >(tee -a "$EMULATOR_LOG_FILE") 2>&1 &
emulator_pid=$!
printf '%s\n' "$emulator_pid" > "$EMULATOR_PID_FILE"

sleep 1
if ! kill -0 "$emulator_pid" 2>/dev/null; then
  echo "Emulator process $emulator_pid exited during startup"
  exit 1
fi

echo "Started emulator PID $emulator_pid with $EMULATOR_CORES cores and DNS server $EMULATOR_DNS_SERVER"
