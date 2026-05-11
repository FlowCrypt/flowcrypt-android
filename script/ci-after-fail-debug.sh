#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail
set -o xtrace

check_ping_or_fail() {
  local host="$1"
  local label="$2"

  for attempt in {1..30}; do
    if adb shell "ping -c 1 ${host}"; then
      return 0
    fi

    if [[ "$attempt" -eq 30 ]]; then
      echo "Failed to ping ${host}: ${label}"
      exit 1
    fi

    sleep 2
  done
}

###################################################################################################
check_ping_or_fail "www.google.com" "internet connection"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test forwarding"

set +o xtrace