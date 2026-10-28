#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

# Keys must match script/ci-push-build-artifacts.sh
BUILD_CACHE_KEY="build-cache-${SEMAPHORE_WORKFLOW_ID}"
APKS_CACHE_KEY="apks-${SEMAPHORE_WORKFLOW_ID}"

echo "==> Restoring pre-built outputs from Semaphore cache..."

# 'cache restore' exits with 0 on a miss, so check the key explicitly.
if cache has_key "$BUILD_CACHE_KEY"; then
  # Restored content is merged into the build cache directory from the global Gradle cache.
  cache restore "$BUILD_CACHE_KEY"
  echo "Build cache restored successfully."
else
  echo "Notice: Workflow build cache not available; will compile incrementally."
fi

if cache has_key "$APKS_CACHE_KEY"; then
  cache restore "$APKS_CACHE_KEY"
  echo "APKs restored successfully."
else
  echo "Notice: Workflow APKs not available; will build as needed."
fi

echo "==> Build outputs preparation finished."
