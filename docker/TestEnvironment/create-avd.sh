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

mkdir -p "${ANDROID_AVD_HOME:-$HOME/.android/avd}" "$HOME/.android"
touch "$HOME/.android/repositories.cfg"

if avdmanager list avd | grep -q "Name: ${AVD_NAME}"; then
  avdmanager delete avd --name "${AVD_NAME}" || true
fi

rm -rf \
  "${ANDROID_AVD_HOME:-$HOME/.android/avd}/${AVD_NAME}.avd" \
  "${ANDROID_AVD_HOME:-$HOME/.android/avd}/${AVD_NAME}.ini"

echo "no" | avdmanager create avd \
  --force \
  --name "${AVD_NAME}" \
  --package "${SYSTEM_IMAGE}" \
  --device "${DEVICE_NAME}" \
  --abi "${ABI}"

echo "Created AVD ${AVD_NAME}"
