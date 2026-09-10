#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

IMAGE_NAME="flowcrypt/android-test-env"
CONTAINER_NAME="flowcrypt-android-test-env"
EMULATOR_DNS_SERVER="${EMULATOR_DNS_SERVER:-127.0.0.1}"

docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

docker run --rm -it \
  --name "$CONTAINER_NAME" \
  --network host \
  --dns 127.0.0.1 \
  -e EMULATOR_DNS_SERVER="$EMULATOR_DNS_SERVER" \
  --device /dev/kvm \
  "$IMAGE_NAME"
