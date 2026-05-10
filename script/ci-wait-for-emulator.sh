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

print_network_debug() {
  local label="$1"

  echo "========== Network debug: ${label} =========="

  adb devices || true
  adb shell getprop net.dns1 || true
  adb shell getprop net.dns2 || true
  adb shell ip route || true
  adb shell ip addr || true
  adb shell iptables -t nat -S || true
  adb forward --list || true

  echo "========== End network debug: ${label} =========="
}

check_ping_or_fail() {
  local host="$1"
  local label="$2"

  for attempt in {1..10}; do
    if adb shell "ping -c 1 ${host}"; then
      return 0
    fi

    if [[ "$attempt" -eq 30 ]]; then
      echo "Failed to ping ${host}: ${label}"
      print_network_debug "ping failed: ${host}"
      #exit 1
    fi

    sleep 2
  done
}

wait_for_boot_completed

adb shell wm dismiss-keyguard || true

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

print_network_debug "before adb root"
check_ping_or_fail "www.google.com" "internet before adb root"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test before adb root"

###################################################################################################
# To test WKD we need to route all traffic for localhost:443 to localhost:1212
# as we can't use 443 directly for a mock web server.
###################################################################################################

adb root || {
  echo "adb root failed, restarting adb and waiting for emulator..."

  adb kill-server || true
  adb start-server

  wait_for_boot_completed
}

sleep 2
wait_for_boot_completed

print_network_debug "after adb root"
check_ping_or_fail "www.google.com" "internet after adb root"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test after adb root"

adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"

adb shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
adb shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"

# https://developer.android.com/tools/adb#forwardports
# Forwards requests on a specific host port to a different port on a device.
adb forward tcp:1212 tcp:1212

print_network_debug "after iptables and adb forward"
check_ping_or_fail "www.google.com" "internet after iptables"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test after iptables"

echo "Emulator is ready"
set +o xtrace