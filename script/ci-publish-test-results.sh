#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ "$SEMAPHORE_JOB_NAME" =~ ^Instrumentation.* ]]; then
  results_dir="$HOME/git/flowcrypt-android/FlowCrypt/build/outputs/androidTest-results/connected/"
  if [[ -d "$results_dir" ]]; then
    # Android reports can retain failed entries even when Gradle's final result is successful.
    # For a passed job, publish its authoritative final outcome instead of non-final failures.
    job_result="${SEMAPHORE_JOB_RESULT:-}"
    if [[ "${job_result,,}" == "passed" ]]; then
      python3 ./script/ci-normalize-passed-instrumentation-results.py "$results_dir"
    fi
    test-results publish "$results_dir" --name "Instrumentation tests" --generate-mcp-summary
  else
    echo "Instrumentation test results directory does not exist: $results_dir"
  fi
fi

if [[ "$SEMAPHORE_JOB_NAME" =~ ^JUnit.* ]]; then
  results_dir="$HOME/git/flowcrypt-android/FlowCrypt/build/test-results/"
  if [[ -d "$results_dir" ]]; then
    test-results publish "$results_dir" --name "JUnit tests" --generate-mcp-summary
  else
    echo "JUnit test results directory does not exist: $results_dir"
  fi
fi
