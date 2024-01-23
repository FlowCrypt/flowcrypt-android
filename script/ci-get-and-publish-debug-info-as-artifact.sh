#!/bin/bash

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Lint.* ]]; then
    # don't do any things for 'Lint(structural quality)' job
    exit 0
fi

echo "Store tests results for $SEMAPHORE_JOB_NAME"
artifact push job FlowCrypt/build/reports/

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
    # store full logcat log
    echo "Collect logcat logs as logcat.txt.gz for $SEMAPHORE_JOB_NAME"
    adb logcat -d | gzip > ~/logcat.txt.gz
    artifact push job ~/logcat.txt.gz

    # store the device's screenshot. it may help to debug a failure
    echo "Store the device's screenshot for $SEMAPHORE_JOB_NAME"
    adb shell screencap -p /sdcard/screencap.png
    adb pull "/sdcard/screencap.png"
    artifact push job screencap.png
fi

