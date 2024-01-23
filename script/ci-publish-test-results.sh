#!/bin/bash

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
    # publish test results for Instrumentation tests
    test-results publish ~/git/flowcrypt-android/FlowCrypt/build/outputs/androidTest-results/connected/ --name "$SEMAPHORE_JOB_NAME"
fi

if [[ "$SEMAPHORE_JOB_NAME" =~ ^JUnit.* ]]; then
    # publish test results for JUnit tests
    test-results publish ~/git/flowcrypt-android/FlowCrypt/build/test-results/ --name "$SEMAPHORE_JOB_NAME"
fi

