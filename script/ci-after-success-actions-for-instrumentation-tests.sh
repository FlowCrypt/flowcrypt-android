#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

ADB_BIN="$(command -v adb)"
ADB_COMMAND_TIMEOUT="${ADB_COMMAND_TIMEOUT:-15s}"

adb() {
  timeout "$ADB_COMMAND_TIMEOUT" "$ADB_BIN" "$@"
}

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
  ./script/ci-monitor-host-resources.sh stop || true
  ./script/ci-monitor-host-resources.sh summary || true

  for resource_file in \
    "${HOST_RESOURCE_METRICS_FILE:-$HOME/host-resource-metrics.csv}" \
    "${HOST_RESOURCE_MONITOR_LOG_FILE:-$HOME/host-resource-monitor.log}" \
    "/tmp/system-metrics"; do
    if [[ -e "$resource_file" ]]; then
      echo "Store host resource metrics: $resource_file"
      artifact push job "$resource_file" || true
    fi
  done

    # print debug info about connected device
    echo "Print connected devices"
    adb devices

  if [[ -f "$HOME/logcat_log.txt" ]]; then
    echo "Store logcat log"
    artifact push job "$HOME/logcat_log.txt"
  else
    echo "No logcat_log.txt found, skipping"
  fi

  echo "Store screenshots"
  if adb shell test -d /sdcard/Pictures; then
    rm -rf Pictures
    if adb pull "/sdcard/Pictures" Pictures 2>/dev/null && [[ -d Pictures && "$(ls -A Pictures 2>/dev/null)" ]]; then
      artifact push job Pictures || true
    else
      echo "No screenshots found in /sdcard/Pictures, skipping"
    fi
  else
    echo "No /sdcard/Pictures directory found, skipping"
  fi
fi
