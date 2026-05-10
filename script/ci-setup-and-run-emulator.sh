#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_NAME="ci-emulator"
SYSTEM_IMAGE="system-images;android-36;google_apis;x86_64"
DEVICE_NAME="pixel_9"
ABI="google_apis/x86_64"

"$ANDROID_HOME/emulator/emulator" -accel-check

# Keep the emulator fresh for every CI job. Do not reuse AVD state.
rm -rf "$HOME/.android/avd/${AVD_NAME}.avd" "$HOME/.android/avd/${AVD_NAME}.ini"

avdmanager list devices #debug

echo -ne '\n' | avdmanager -v create avd \
  --name "$AVD_NAME" \
  --package "$SYSTEM_IMAGE" \
  --device "$DEVICE_NAME" \
  --abi "$ABI"

# Keep RAM modest for e2-standard-2. This file belongs to the fresh AVD created above.
echo "hw.ramSize=2048" >> "$HOME/.android/avd/${AVD_NAME}.avd/config.ini"
cat "$HOME/.android/avd/${AVD_NAME}.avd/config.ini"

"$ANDROID_HOME/emulator/emulator" -list-avds #debug

"$ANDROID_HOME/emulator/emulator" \
  -avd "$AVD_NAME" \
  -no-window \
  -no-boot-anim \
  -no-audio \
  -no-snapshot \
  -no-snapshot-load \
  -no-snapshot-save \
  -wipe-data \
  -gpu swiftshader_indirect \
  -read-only \
  -no-metrics \
  -dns-server 10.0.2.2 &
