#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
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
    adb pull "/sdcard/Pictures" Pictures
    artifact push job Pictures
  else
    echo "No /sdcard/Pictures directory found, skipping"
  fi
fi
