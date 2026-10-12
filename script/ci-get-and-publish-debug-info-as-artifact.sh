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

push_file_if_present() {
  local path="$1"
  local label="$2"

  if [[ -f "$path" ]]; then
    echo "Store $label for $SEMAPHORE_JOB_NAME"
    artifact push job "$path" || true
  else
    echo "$label does not exist: $path"
  fi
}

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Lint.* ]]; then
  # Do nothing for 'Lint(structural quality)' job.
  exit 0
fi

reports_dir="FlowCrypt/build/reports/"
if [[ -d "$reports_dir" ]]; then
  echo "Store test reports for $SEMAPHORE_JOB_NAME"
  artifact push job "$reports_dir"
else
  echo "Reports directory does not exist: $reports_dir"
fi

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
  push_file_if_present "${EMULATOR_LOG_FILE:-$HOME/emulator.log}" "emulator log"
  push_file_if_present "${INSTRUMENTATION_COMMAND_LOG_FILE:-$HOME/instrumentation-command.log}" "instrumentation command log"
  push_file_if_present "${EMULATOR_WATCHDOG_LOG_FILE:-$HOME/emulator-watchdog.log}" "emulator watchdog log"
  push_file_if_present "${EMULATOR_WATCHDOG_FAILURE_FILE:-$HOME/emulator-watchdog.failed}" "emulator watchdog failure marker"

  if adb get-state 2>/dev/null | grep -q "device"; then
    # store full logcat log
    echo "Collect logcat logs as logcat.txt.gz for $SEMAPHORE_JOB_NAME"
    if adb logcat -d | gzip > "$HOME/logcat.txt.gz"; then
      artifact push job "$HOME/logcat.txt.gz"
    else
      echo "Could not collect logcat within $ADB_COMMAND_TIMEOUT, skipping"
    fi

    echo "Store the device's screenshot for $SEMAPHORE_JOB_NAME"
    if adb shell screencap -p /sdcard/screencap.png; then
      if adb pull "/sdcard/screencap.png"; then
        artifact push job screencap.png
      else
        echo "Could not pull screencap.png"
      fi
    else
      echo "Could not create screencap.png"
    fi
  else
    echo "No connected device found for $SEMAPHORE_JOB_NAME, skipping logcat and screenshot."
  fi
fi
