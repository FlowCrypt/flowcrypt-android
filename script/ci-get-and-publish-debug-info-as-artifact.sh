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
  # store full logcat log
  echo "Collect logcat logs as logcat.txt.gz for $SEMAPHORE_JOB_NAME"
  adb logcat -d | gzip > "$HOME/logcat.txt.gz"
  artifact push job "$HOME/logcat.txt.gz"

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
fi
