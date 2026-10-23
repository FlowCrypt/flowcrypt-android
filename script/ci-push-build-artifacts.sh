#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

# Build outputs are shared via the Semaphore cache instead of workflow artifacts:
# artifact pulls are billed as egress, cache traffic is not.
# Keys are bound to the workflow, so partial reruns of the same workflow can reuse them.
# ACCESS_TIME cleanup evicts leftover keys of failed workflows before the SDK and Gradle caches,
# which are stored once and then only restored (the default STORE_TIME would evict them first).
BUILD_CACHE_KEY="build-cache-${SEMAPHORE_WORKFLOW_ID}"
APKS_CACHE_KEY="apks-${SEMAPHORE_WORKFLOW_ID}"

echo "==> Storing build outputs in Semaphore cache..."

if [[ -d "$HOME/.gradle/caches/build-cache-1" ]]; then
  echo "Storing Gradle build cache ($(du -sh "$HOME/.gradle/caches/build-cache-1" | cut -f1))..."
  # Replace a key left by a previous run of the Build block in the same workflow.
  cache delete "$BUILD_CACHE_KEY" > /dev/null 2>&1 || true
  cache store --cleanup-by ACCESS_TIME "$BUILD_CACHE_KEY" "$HOME/.gradle/caches/build-cache-1"
  echo "Build cache stored successfully."
else
  echo "Warning: ~/.gradle/caches/build-cache-1 not found, skipping build cache store."
fi

if [[ -d "FlowCrypt/build/outputs/apk" ]]; then
  echo "Storing built APKs ($(du -sh FlowCrypt/build/outputs/apk | cut -f1))..."
  cache delete "$APKS_CACHE_KEY" > /dev/null 2>&1 || true
  cache store --cleanup-by ACCESS_TIME "$APKS_CACHE_KEY" FlowCrypt/build/outputs/apk
  echo "APKs stored successfully."
else
  echo "Warning: FlowCrypt/build/outputs/apk not found, skipping APKs store."
fi

echo "==> Build outputs stored in Semaphore cache."
