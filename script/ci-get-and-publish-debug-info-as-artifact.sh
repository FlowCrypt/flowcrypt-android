#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

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
  if adb get-state 2>/dev/null | grep -q "device"; then
    echo "Collect logcat logs as logcat.txt.gz for $SEMAPHORE_JOB_NAME"
    timeout 30s adb logcat -d | gzip > "$HOME/logcat.txt.gz" || true
    artifact push job "$HOME/logcat.txt.gz" || true

    echo "Store the device's screenshot for $SEMAPHORE_JOB_NAME"
    if timeout 15s adb shell screencap -p /sdcard/screencap.png 2>/dev/null; then
      if timeout 15s adb pull "/sdcard/screencap.png" 2>/dev/null; then
        artifact push job screencap.png || true
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

