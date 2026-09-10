#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
  results_dir="$HOME/git/flowcrypt-android/FlowCrypt/build/outputs/androidTest-results/connected/"
  if [[ -d "$results_dir" ]]; then
    test-results publish "$results_dir" --name "$SEMAPHORE_JOB_NAME"
  else
    echo "Instrumentation test results directory does not exist: $results_dir"
  fi
fi

if [[ "$SEMAPHORE_JOB_NAME" =~ ^JUnit.* ]]; then
  results_dir="$HOME/git/flowcrypt-android/FlowCrypt/build/test-results/"
  if [[ -d "$results_dir" ]]; then
    test-results publish "$results_dir" --name "$SEMAPHORE_JOB_NAME"
  else
    echo "JUnit test results directory does not exist: $results_dir"
  fi
fi
