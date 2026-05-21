#!/bin/bash
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

wait_for_network_after_adb_root() {
  for attempt in {1..30}; do
    if adb shell "ping -c 1 10.0.2.2" && adb shell "ping -c 1 8.8.8.8"; then
      return 0
    fi

    echo "Waiting for emulator network after adb root... attempt ${attempt}/30"
    sleep 2
  done

  echo "Emulator network did not become ready after adb root"
  print_network_debug "network after adb root"
  exit 1
}

check_ping_or_fail() {
  local host="$1"
  local label="$2"

  for attempt in {1..10}; do
    if adb shell "ping -c 1 ${host}"; then
      return 0
    fi

    echo "Waiting for ${label}... attempt ${attempt}/10"
    sleep 2
  done

  echo "Failed to ping ${host}: ${label}"
  print_network_debug "${label}"
  exit 1
}

print_network_debug() {
  local reason="$1"

  set +e

  echo
  echo "======================================================================"
  echo "Network failure diagnostics: ${reason}"
  echo "UTC time: $(date -u '+%Y-%m-%d %H:%M:%S')"
  echo "======================================================================"

  echo
  echo "[debug] adb devices"
  adb devices -l

  echo
  echo "[debug] adb forward list"
  adb forward --list

  echo
  echo "[debug] net/dns props"
  adb shell getprop | grep -iE 'dns|net' || true

  echo
  echo "[debug] ip addr"
  adb shell ip addr || true

  echo
  echo "[debug] ip route"
  adb shell ip route || true

  echo
  echo "[debug] connectivity DNS"
  adb shell dumpsys connectivity | grep -iE 'DnsAddresses|ServerAddress|Active default network' || true

  echo
  echo "[debug] ping gateway"
  adb shell ping -c 1 10.0.2.2 || true

  echo
  echo "[debug] ping raw internet IP"
  adb shell ping -c 1 8.8.8.8 || true

  echo
  echo "[debug] ping public DNS name"
  adb shell ping -c 1 www.google.com || true

  echo
  echo "[debug] ping flowcrypt test domain"
  adb shell ping -c 1 fes.flowcrypt.test || true

  set -e
}

wait_for_boot_completed

adb shell wm dismiss-keyguard
sleep 1

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

###################################################################################################
# To test WKD we need to route all traffic for localhost:443 to localhost:1212
# as we can't use 443 directly for a mock web server.

adb root

# adb root restarts adbd, so wait until the device is available again.
wait_for_boot_completed

# adb root can temporarily reset Android networking.
wait_for_network_after_adb_root

adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"

adb shell "iptables -t nat -D OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 1212" || true
adb shell "iptables -t nat -A OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 1212"

adb shell "iptables -t nat -S OUTPUT"

###################################################################################################

# https://developer.android.com/tools/adb#forwardports
# Forwards requests on a specific host port to a different port on a device.
# It can be helpful for debugging a mock web server.
adb forward tcp:1212 tcp:1212

# Check emulator network before running tests.
check_ping_or_fail "10.0.2.2" "emulator host gateway"
check_ping_or_fail "8.8.8.8" "internet raw IP connectivity"
check_ping_or_fail "www.google.com" "internet DNS"
check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test DNS/reachability"

echo "Emulator is ready"

set +o xtrace