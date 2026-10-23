#!/bin/bash
#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

target_device="${1:-${ANDROID_SERIAL:-}}"
if [[ -n "$target_device" ]]; then
  export ANDROID_SERIAL="$target_device"
fi

wait_for_boot_completed() {
  adb wait-for-device

  local timeout_seconds=180
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    local boot_completed
    boot_completed=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
    if [[ "$boot_completed" == "1" ]]; then
      return 0
    fi
    sleep 1
  done

  echo "ERROR: Timed out after ${timeout_seconds}s waiting for sys.boot_completed${ANDROID_SERIAL:+ on $ANDROID_SERIAL}"
  exit 1
}

run_adb_root_or_fail() {
  for attempt in {1..10}; do
    if adb root; then
      adb wait-for-device
      return 0
    fi

    echo "adb root failed... attempt ${attempt}/10"
    sleep 2
  done

  echo "adb root failed after 10 attempts"
  exit 1
}

wait_for_network_after_adb_root() {
  local timeout_seconds="${EMULATOR_NETWORK_TIMEOUT_SECONDS:-180}"
  if [[ ! "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
    echo "EMULATOR_NETWORK_TIMEOUT_SECONDS must be a positive integer"
    exit 1
  fi

  # A loaded CI host can need several seconds just to start an ADB shell.
  local max_probe_seconds=10
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    local remaining_seconds=$((deadline - SECONDS))
    local probe_seconds=$(( remaining_seconds < max_probe_seconds ? remaining_seconds : max_probe_seconds ))

    # Bound both pings and the ADB call so a stalled guest cannot hang the wait.
    if timeout --kill-after=1s "${probe_seconds}s" adb shell \
      "ping -c 1 10.0.2.2 && ping -c 1 8.8.8.8" >/dev/null 2>&1; then
      echo "Emulator network is ready after adb root"
      return 0
    fi

    echo "Waiting for emulator network after adb root... ${remaining_seconds}s left"
    sleep 2
  done

  echo "Emulator network did not become ready after adb root within ${timeout_seconds}s"

  print_network_debug "network after adb root"

  exit 1
}

check_ping_or_fail() {
  local host="$1"
  local label="$2"

  for attempt in {1..15}; do
    if adb shell "ping -c 1 ${host}" >/dev/null 2>&1; then
      echo "PASS: ${label}"
      return 0
    fi

    echo "Waiting for ${label}... attempt ${attempt}/15"

    sleep 2
  done

  echo "FAIL: ${label}"

  print_network_debug "${label}"

  exit 1
}

print_network_debug() {
  local reason="$1"

  set +e

  echo
  echo "========================================================================"
  echo "Network failure diagnostics: ${reason}"
  echo "UTC time: $(date -u '+%Y-%m-%d %H:%M:%S')"
  echo "========================================================================"

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
###################################################################################################

echo "[debug] DNS before adb root"

adb shell dumpsys connectivity | grep -iE 'DnsAddresses|ServerAddress|Active default network' || true
adb shell ping -c 1 www.google.com || true
adb shell ping -c 1 fes.flowcrypt.test || true

run_adb_root_or_fail

# adb root restarts adbd, so wait until the device is available again.
wait_for_boot_completed

# Android networking may take some time to recover after adb root.
wait_for_network_after_adb_root

echo "[debug] DNS after adb root"

adb shell dumpsys connectivity | grep -iE 'DnsAddresses|ServerAddress|Active default network' || true
adb shell ping -c 1 www.google.com || true
adb shell ping -c 1 fes.flowcrypt.test || true

adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"

adb shell "iptables -t nat -D OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 1212" || true

adb shell "iptables -t nat -A OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 1212"

adb shell "iptables -t nat -S OUTPUT"

###################################################################################################

# https://developer.android.com/tools/adb#forwardports
# Forwards requests on a specific host port to a different port on a device.
# It can be helpful for debugging a mock web server.
if [[ "${FORWARD_PORT_1212:-1}" == "1" ]]; then
  adb forward tcp:1212 tcp:1212
fi

###################################################################################################
# Final network validation before running tests.
###################################################################################################

if [[ "${VALIDATE_NETWORK:-1}" == "1" ]]; then
  check_ping_or_fail "10.0.2.2" "emulator host gateway"
  check_ping_or_fail "8.8.8.8" "internet raw IP connectivity"
  check_ping_or_fail "www.google.com" "internet DNS"
  check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test DNS/reachability"
else
  if adb shell ping -c 1 fes.flowcrypt.test >/dev/null 2>&1; then
    echo "PASS: flowcrypt.test DNS/reachability"
  else
    echo "WARNING: fes.flowcrypt.test is not reachable (expected if host DNS is not configured)"
  fi
fi

echo "Emulator is ready${ANDROID_SERIAL:+ ($ANDROID_SERIAL)}"
