#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_NAME="${AVD_NAME:-ci-emulator}"
SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-36;google_apis;x86_64}"
DEVICE_NAME="${DEVICE_NAME:-pixel_9}"
ABI="${ABI:-google_apis/x86_64}"
AVD_DIR="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
AVD_RAM_SIZE="${AVD_RAM_SIZE:-}"

mkdir -p "$AVD_DIR" "$HOME/.android"
touch "$HOME/.android/repositories.cfg"

if avdmanager list avd | grep -q "Name: ${AVD_NAME}"; then
  echo "Removing existing AVD: ${AVD_NAME}"
  avdmanager delete avd --name "$AVD_NAME" || true
fi

rm -rf \
  "${AVD_DIR}/${AVD_NAME}.avd" \
  "${AVD_DIR}/${AVD_NAME}.ini"

echo -ne '\n' | avdmanager -v create avd \
  --force \
  --name "$AVD_NAME" \
  --package "$SYSTEM_IMAGE" \
  --device "$DEVICE_NAME" \
  --abi "$ABI"

if [[ -n "$AVD_RAM_SIZE" ]]; then
  echo "hw.ramSize=${AVD_RAM_SIZE}" >> "${AVD_DIR}/${AVD_NAME}.avd/config.ini"
fi

echo "Created AVD ${AVD_NAME}"
