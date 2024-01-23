#!/bin/bash

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
    # print debug info about connected device
    echo "Print connected devices"
    adb devices

    # store logcat log
    echo "Store logcat log"
    artifact push job ~/logcat_log.txt

    # store screenshots
    echo "Store screenshots"
    adb pull "/sdcard/Pictures"
    adb shell ls /sdcard/Pictures
    artifact push job Pictures
fi

