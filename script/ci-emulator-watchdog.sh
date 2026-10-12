#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ "$#" -ne 1 || ( "$1" != "--check-log" && ! "$1" =~ ^[0-9]+$ ) ]]; then
  echo "Usage: $0 <test-process-group-id|--check-log>"
  exit 2
fi

watchdog_mode="watch"
test_process_group_id="$1"
if [[ "$1" == "--check-log" ]]; then
  watchdog_mode="check-log"
  test_process_group_id=""
fi

adb_bin="${ADB_BIN:-$(command -v adb)}"
emulator_pid_file="${EMULATOR_PID_FILE:-$HOME/emulator.pid}"
emulator_log_file="${EMULATOR_LOG_FILE:-$HOME/emulator.log}"
test_command_log_file="${INSTRUMENTATION_COMMAND_LOG_FILE:-$HOME/instrumentation-command.log}"
watchdog_log_file="${EMULATOR_WATCHDOG_LOG_FILE:-$HOME/emulator-watchdog.log}"
watchdog_failure_file="${EMULATOR_WATCHDOG_FAILURE_FILE:-$HOME/emulator-watchdog.failed}"
probe_timeout="${EMULATOR_WATCHDOG_PROBE_TIMEOUT:-10s}"
poll_interval_seconds="${EMULATOR_WATCHDOG_INTERVAL_SECONDS:-15}"
max_consecutive_failures="${EMULATOR_WATCHDOG_MAX_FAILURES:-6}"
term_grace_seconds="${EMULATOR_WATCHDOG_TERM_GRACE_SECONDS:-10}"
fatal_device_error_regex="device([[:space:]]+['\"]?[^[:space:]]+['\"]?[[:space:]]+is)?[[:space:]]+offline|device[[:space:]]+['\"]?[^[:space:]]+['\"]?[[:space:]]+(not found|disconnected)|no devices/emulators found|Can.t find service: (package|settings)"

timestamp() {
  date -u '+%Y-%m-%d %H:%M:%S UTC'
}

adb_with_timeout() {
  timeout "$probe_timeout" "$adb_bin" "$@"
}

emulator_process_is_alive() {
  [[ -s "$emulator_pid_file" ]] || return 0

  local emulator_pid
  emulator_pid="$(<"$emulator_pid_file")"
  [[ "$emulator_pid" =~ ^[0-9]+$ ]] && kill -0 "$emulator_pid" 2>/dev/null
}

test_process_group_is_alive() {
  kill -0 -- "-$test_process_group_id" 2>/dev/null
}

test_output_has_fatal_device_error() {
  [[ -r "$test_command_log_file" ]] || return 1

  grep -Eiq "$fatal_device_error_regex" "$test_command_log_file"
}

if [[ "$watchdog_mode" == "check-log" ]]; then
  if test_output_has_fatal_device_error; then
    grep -Ein "$fatal_device_error_regex" "$test_command_log_file" | tail -n 10 >&2 || true
    exit 0
  fi
  exit 1
fi

: > "$watchdog_log_file"
exec > >(tee -a "$watchdog_log_file") 2>&1

device_is_healthy() {
  local device_state
  if ! device_state="$(adb_with_timeout get-state 2>&1)"; then
    echo "adb get-state failed: $device_state"
    return 1
  fi

  if [[ "$device_state" != "device" ]]; then
    echo "adb state is '$device_state'"
    return 1
  fi

  local guest_health
  if ! guest_health="$(adb_with_timeout shell '
    boot_completed="$(getprop sys.boot_completed)"
    echo "sys.boot_completed=$boot_completed"
    test "$boot_completed" = "1" || exit 10

    system_server_pid="$(pidof system_server)"
    echo "system_server=$system_server_pid"
    test -n "$system_server_pid" || exit 11

    package_service="$(service check package)"
    echo "$package_service"
    echo "$package_service" | grep -q "found" || exit 12

    settings_service="$(service check settings)"
    echo "$settings_service"
    echo "$settings_service" | grep -q "found" || exit 13
  ' 2>&1)"; then
    echo "Android guest health probe failed:"
    echo "$guest_health"
    return 1
  fi
}

print_failure_diagnostics() {
  echo
  echo "======================================================================"
  echo "Emulator watchdog failure diagnostics"
  echo "UTC time: $(timestamp)"
  echo "======================================================================"

  timeout 5s "$adb_bin" devices -l || true
  timeout 5s "$adb_bin" get-state || true

  if [[ -s "$emulator_pid_file" ]]; then
    local emulator_pid
    emulator_pid="$(<"$emulator_pid_file")"
    ps -o pid,ppid,pgid,stat,etime,%cpu,%mem,cmd -p "$emulator_pid" || true
  fi

  echo
  echo "[debug] Test process group"
  ps -eo pid,ppid,pgid,stat,etime,%cpu,%mem,cmd \
    | awk -v process_group="$test_process_group_id" '$3 == process_group || NR == 1' || true

  echo
  echo "[debug] Host memory"
  free -h || true

  echo
  echo "[debug] Host disk"
  df -h || true

  echo
  echo "[debug] Recent kernel messages"
  timeout 5s dmesg --ctime 2>&1 | tail -n 200 || true

  if [[ -f "$emulator_log_file" ]]; then
    echo
    echo "[debug] Last 200 emulator log lines"
    tail -n 200 "$emulator_log_file" || true
  fi

  if [[ -f "$test_command_log_file" ]]; then
    echo
    echo "[debug] Last 200 instrumentation command log lines"
    tail -n 200 "$test_command_log_file" || true
  fi
}

terminate_test_process_group() {
  echo "[$(timestamp)] Stopping test process group $test_process_group_id"
  kill -TERM -- "-$test_process_group_id" 2>/dev/null || true

  local waited=0
  while test_process_group_is_alive && (( waited < term_grace_seconds )); do
    sleep 1
    waited=$((waited + 1))
  done

  if test_process_group_is_alive; then
    echo "[$(timestamp)] Test process group did not stop after ${term_grace_seconds}s; sending SIGKILL"
    kill -KILL -- "-$test_process_group_id" 2>/dev/null || true
  fi
}

consecutive_failures=0

echo "[$(timestamp)] Emulator watchdog started: interval=${poll_interval_seconds}s, max_failures=$max_consecutive_failures, probe_timeout=$probe_timeout"

while test_process_group_is_alive; do
  if test_output_has_fatal_device_error; then
    echo "[$(timestamp)] Instrumentation output contains a fatal device transport/service error:"
    grep -Ein "$fatal_device_error_regex" "$test_command_log_file" | tail -n 10 || true
    consecutive_failures="$max_consecutive_failures"
  elif ! emulator_process_is_alive; then
    echo "[$(timestamp)] Emulator process is no longer running"
    consecutive_failures="$max_consecutive_failures"
  elif device_is_healthy; then
    if (( consecutive_failures > 0 )); then
      echo "[$(timestamp)] Emulator recovered after $consecutive_failures failed health probe(s)"
    fi
    consecutive_failures=0
  else
    consecutive_failures=$((consecutive_failures + 1))
    echo "[$(timestamp)] Emulator health probe failed ($consecutive_failures/$max_consecutive_failures)"

    if (( consecutive_failures == 2 )); then
      echo "[$(timestamp)] Asking adb to reconnect offline devices"
      adb_with_timeout reconnect offline || true
    fi
  fi

  if (( consecutive_failures >= max_consecutive_failures )); then
    echo "[$(timestamp)] Emulator stayed unhealthy; failing the job early"
    printf 'Emulator unhealthy at %s after %s consecutive probes\n' \
      "$(timestamp)" "$consecutive_failures" > "$watchdog_failure_file"
    print_failure_diagnostics
    terminate_test_process_group
    exit 86
  fi

  sleep "$poll_interval_seconds"
done

echo "[$(timestamp)] Test process exited; emulator watchdog stopped"
