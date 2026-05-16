#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

IMAGE_NAME="flowcrypt/android-test-env"
CONTAINER_NAME="flowcrypt-android-test-env"

docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

docker run --rm -it \
  --name "$CONTAINER_NAME" \
  --device /dev/kvm \
  "$IMAGE_NAME"
