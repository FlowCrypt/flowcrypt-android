#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail
set -o xtrace

wait_for_boot_completed() {
  adb wait-for-device

  # shellcheck disable=SC2016
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

check_iptables_rule_or_fail() {
  local chain="$1"
  local expected_rule="$2"

  if ! adb shell "iptables -t nat -S ${chain}" | grep -F -- "${expected_rule}"; then
    echo "iptables rule was not applied:"
    echo "${expected_rule}"
    exit 1
  fi
}

wait_for_boot_completed

adb shell wm dismiss-keyguard || true

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

###################################################################################################
# To test WKD we need to route all traffic for localhost:443 to localhost:1212
# as we can't use 443 directly for a mock web server.
###################################################################################################
if adb root; then
  echo "adb root succeeded"
else
  echo "adb root failed, restarting adb and waiting for emulator..."

  adb kill-server || true
  adb start-server
fi

wait_for_boot_completed

adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"

adb shell "iptables -t nat -D OUTPUT -s 127.0.0.1/32 -p tcp -m tcp --dport 443 -j REDIRECT --to-ports 1212" || true
adb shell "iptables -t nat -D PREROUTING -s 127.0.0.1/32 -p tcp -m tcp --dport 443 -j REDIRECT --to-ports 1212" || true

adb shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to-ports 1212"
adb shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to-ports 1212"

check_iptables_rule_or_fail \
  "PREROUTING" \
  "-A PREROUTING -s 127.0.0.1/32 -p tcp -m tcp --dport 443 -j REDIRECT --to-ports 1212"

check_iptables_rule_or_fail \
  "OUTPUT" \
  "-A OUTPUT -s 127.0.0.1/32 -p tcp -m tcp --dport 443 -j REDIRECT --to-ports 1212"
###################################################################################################

# https://developer.android.com/tools/adb#forwardports
# Forwards requests on a specific host port to a different port on a device.
# It can be helpful for debugging a mock web server
adb forward --remove tcp:1212 || true
adb forward tcp:1212 tcp:1212
if ! adb forward --list | grep -F -- "tcp:1212 tcp:1212"; then
  echo "adb forward was not applied: tcp:1212 tcp:1212"
  exit 1
fi

###################################################################################################
check_ping_or_fail "www.google.com" "internet connection"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test after iptables and adb forward"

echo "Emulator is ready"
set +o xtrace