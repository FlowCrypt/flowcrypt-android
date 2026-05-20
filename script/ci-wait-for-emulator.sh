#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail
set -o xtrace

FLOWCRYPT_TEST_DOMAINS=(
  "flowcrypt.test"
  "api.flowcrypt.test"
  "attester.flowcrypt.test"
  "fes.flowcrypt.test"
)

wait_for_boot_completed() {
  adb wait-for-device

  adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done;'
}

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

write_flowcrypt_hosts() {
  local hosts_content

  hosts_content=$(
    {
      echo "127.0.0.1 localhost"
      echo "::1 ip6-localhost"

      for domain in "${FLOWCRYPT_TEST_DOMAINS[@]}"; do
        echo "10.0.2.2 ${domain}"
      done
    }
  )

  adb shell "cat > /system/etc/hosts <<'EOF'
${hosts_content}
EOF"
}

wait_for_boot_completed

adb shell wm dismiss-keyguard || true

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

if adb root; then
  echo "adb root succeeded"
else
  echo "adb root failed, restarting adb and waiting for emulator..."

  adb kill-server || true
  adb start-server
fi

wait_for_boot_completed

adb disable-verity || true
adb reboot

wait_for_boot_completed

adb root
adb remount

write_flowcrypt_hosts

adb shell cat /system/etc/hosts

check_ping_or_fail "10.0.2.2" "host machine"
check_ping_or_fail "api.flowcrypt.test" "FlowCrypt test domain routing"
check_ping_or_fail "www.google.com" "internet connection"

echo "Emulator is ready"

set +o xtrace
