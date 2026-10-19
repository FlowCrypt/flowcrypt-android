#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_NAME="${AVD_NAME:-ci-emulator}"
EMULATOR_CORES="${EMULATOR_CORES:-1}"
EMULATOR_GPU_MODE="${EMULATOR_GPU_MODE:-auto}"
EMULATOR_MEMORY="${EMULATOR_MEMORY:-2048}"
EMULATOR_WIPE_DATA="${EMULATOR_WIPE_DATA:-1}"
EMULATOR_DNS_SERVER="${EMULATOR_DNS_SERVER:-127.0.0.1}"
EMULATOR_LOG_FILE="${EMULATOR_LOG_FILE:-$HOME/emulator.log}"
EMULATOR_PID_FILE="${EMULATOR_PID_FILE:-$HOME/emulator.pid}"
EMULATOR_PORT="${EMULATOR_PORT:-}"

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
  -cores "$EMULATOR_CORES"
  -memory "$EMULATOR_MEMORY"
  -gpu "$EMULATOR_GPU_MODE"

  # This value is used by the emulator process on the host/container.
  # Android guest will still see 10.0.2.3 as DNS, but that proxy will forward
  # to the DNS server configured here.
  -dns-server "$EMULATOR_DNS_SERVER"

  -read-only
  -no-metrics
)

if [[ -n "$EMULATOR_PORT" ]]; then
  emulator_args+=(-port "$EMULATOR_PORT")
fi

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

echo "Started emulator PID $emulator_pid with $EMULATOR_CORES cores, $EMULATOR_MEMORY MB RAM, and DNS server $EMULATOR_DNS_SERVER"
