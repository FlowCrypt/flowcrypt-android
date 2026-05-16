#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

docker rm -f flowcrypt-https-test-server 2>/dev/null || true

docker run --rm \
  --name flowcrypt-https-test-server \
  -p 1212:443 \
  flowcrypt-https-test-server