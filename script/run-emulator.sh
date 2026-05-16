#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_NAME="${AVD_NAME:-ci-emulator}"
EMULATOR_GPU_MODE="${EMULATOR_GPU_MODE:-swiftshader_indirect}"
EMULATOR_READ_ONLY="${EMULATOR_READ_ONLY:-1}"
EMULATOR_WIPE_DATA="${EMULATOR_WIPE_DATA:-1}"

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
  -no-metrics
)

if [[ "$EMULATOR_WIPE_DATA" == "1" ]]; then
  emulator_args+=(-wipe-data)
fi

if [[ "$EMULATOR_READ_ONLY" == "1" ]]; then
  emulator_args+=(-read-only)
fi

"$ANDROID_HOME/emulator/emulator" "${emulator_args[@]}" &
