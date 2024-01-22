#!/bin/bash

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
    # store full logcat log
    echo "Collect logcat logs as logcat.txt.gz"
    adb logcat -d | gzip > ~/logcat.txt.gz
    artifact push job ~/logcat.txt.gz

    # store the device's screenshot. it may help to debug a failure
    echo "Store the device's screenshot"
    adb shell screencap -p /sdcard/screencap.png
    adb pull "/sdcard/screencap.png"
    artifact push job screencap.png
fi

