#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail
set -o xtrace

adb wait-for-device
# shellcheck disable=SC2016
adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done;'
adb shell wm dismiss-keyguard || true

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

###################################################################################################
# To test WKD we need to route all traffic for localhost:443 to localhost:1212
# as we can't use 443 directly for a mock web server.
###################################################################################################

adb root || {
  echo "adb root failed, restarting adb and waiting for emulator..."

  adb kill-server || true
  adb start-server

  adb wait-for-device

  # shellcheck disable=SC2016
  adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done;'
}

sleep 2

adb wait-for-device
# shellcheck disable=SC2016
adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done;'

adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"
adb shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
adb shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"

# https://developer.android.com/tools/adb#forwardports
# Forwards requests on a specific host port to a different port on a device.
adb forward tcp:1212 tcp:1212

# Print DNS configuration for easier CI debugging.
adb shell getprop net.dns1 || true
adb shell getprop net.dns2 || true

# Check that the emulator has internet connection.
for attempt in {1..30}; do
  if adb shell "ping -c 1 www.google.com"; then
    break
  fi

  if [[ "$attempt" -eq 30 ]]; then
    echo "Emulator has no internet connection"
    exit 1
  fi

  sleep 2
done

# Check that Android emulator can resolve FlowCrypt test domains.
# Host-side dnsmasq resolving is not enough: the emulator has its own DNS configuration.
for attempt in {1..30}; do
  if adb shell "ping -c 1 fes.flowcrypt.test"; then
    break
  fi

  if [[ "$attempt" -eq 30 ]]; then
    echo "Emulator can't resolve fes.flowcrypt.test"
    exit 1
  fi

  sleep 2
done

echo "Emulator is ready"
set +o xtrace