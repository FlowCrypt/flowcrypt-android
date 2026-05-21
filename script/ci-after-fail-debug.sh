#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

SUMMARY_FAILURES=0

print_section() {
  local title="$1"

  echo
  echo "======================================================================"
  echo "${title}"
  date -u '+UTC time: %Y-%m-%d %H:%M:%S'
  echo "======================================================================"
}

run_debug_cmd() {
  local description="$1"
  shift

  echo
  echo "[debug] ${description}"
  "$@" || true
}

adb_root_shell() {
  local cmd="$1"

  adb shell "su 0 sh -c '${cmd}' 2>/dev/null || sh -c '${cmd}'"
}

summary_check() {
  local label="$1"
  shift

  if "$@" >/dev/null 2>&1; then
    echo "[summary] PASS: ${label}"
  else
    echo "[summary] FAIL: ${label}"
    SUMMARY_FAILURES=$((SUMMARY_FAILURES + 1))
  fi
}

collect_network_debug_info() {
  print_section "Host-side adb state"
  run_debug_cmd "adb devices" adb devices -l
  run_debug_cmd "adb server version" adb version
  run_debug_cmd "adb forward list" adb forward --list
  run_debug_cmd "adb reverse list" adb reverse --list

  print_section "Device network overview"
  run_debug_cmd "boot completed" adb shell getprop sys.boot_completed
  run_debug_cmd "net/dns props" adb shell "getprop | grep -E 'net\\.|dns|dhcp|private_dns'"
  run_debug_cmd "ip addr" adb shell ip addr
  run_debug_cmd "ip route" adb shell ip route
  run_debug_cmd "connectivity dumpsys" adb shell dumpsys connectivity
  run_debug_cmd "private dns mode" adb shell settings get global private_dns_mode

  print_section "DNS and internet checks"

  run_debug_cmd \
    "ping raw IP 8.8.8.8" \
    adb shell ping -c 1 8.8.8.8

  run_debug_cmd \
    "ping www.google.com" \
    adb shell ping -c 1 www.google.com

  run_debug_cmd \
    "ping emulator host gateway 10.0.2.2" \
    adb shell ping -c 1 10.0.2.2

  run_debug_cmd \
    "ping fes.flowcrypt.test" \
    adb shell ping -c 1 fes.flowcrypt.test

  print_section "NAT and sockets"

  run_debug_cmd \
    "iptables nat OUTPUT rules" \
    adb_root_shell "iptables -t nat -S OUTPUT"

  run_debug_cmd \
    "iptables nat OUTPUT counters" \
    adb_root_shell "iptables -t nat -L OUTPUT -n -v"

  run_debug_cmd \
    "listeners netstat" \
    adb shell netstat -lntp
}

print_summary() {
  print_section "Network summary"

  summary_check \
    "internet raw IP (8.8.8.8)" \
    adb shell ping -c 1 8.8.8.8

  summary_check \
    "internet DNS (www.google.com)" \
    adb shell ping -c 1 www.google.com

  summary_check \
    "emulator host gateway 10.0.2.2" \
    adb shell ping -c 1 10.0.2.2

  summary_check \
    "flowcrypt.test ping" \
    adb shell ping -c 1 fes.flowcrypt.test

  summary_check \
    "iptables OUTPUT redirect 443->1212 present" \
    adb_root_shell \
      "iptables -t nat -S OUTPUT | grep -F -- '--dport 443 -j REDIRECT --to-ports 1212'"

  summary_check \
    "listener on :1212 present" \
    adb shell \
      "netstat -lnt 2>/dev/null | grep -E '(^|[:.])1212([[:space:]]|$)'"

  if [[ "$SUMMARY_FAILURES" -eq 0 ]]; then
    echo "[summary] RESULT: PASS"
  else
    echo "[summary] RESULT: FAIL (${SUMMARY_FAILURES} checks failed)"
  fi
}

check_ping_or_fail() {
  local host="$1"
  local label="$2"

  for attempt in {1..10}; do
    if adb shell "ping -c 1 ${host}"; then
      return 0
    fi

    if [[ "$attempt" -eq 10 ]]; then
      echo "Failed to ping ${host}: ${label}"
      return 1
    fi

    sleep 2
  done
}

###################################################################################################

NETWORK_FAILURE=0

if ! check_ping_or_fail "www.google.com" "internet connection"; then
  NETWORK_FAILURE=1
fi

if ! check_ping_or_fail "10.0.2.2" "emulator host gateway"; then
  NETWORK_FAILURE=1
fi

if ! check_ping_or_fail "fes.flowcrypt.test" "flowcrypt.test DNS/reachability"; then
  NETWORK_FAILURE=1
fi

if [[ "$NETWORK_FAILURE" -eq 1 ]]; then
  print_section "Network failure diagnostics"
  collect_network_debug_info
  print_summary
  exit 1
fi

print_section "Network status"
echo "All network checks passed"