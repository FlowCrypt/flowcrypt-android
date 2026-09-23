#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

echo "==> Pulling pre-built artifacts from Semaphore workflow storage..."

mkdir -p "$HOME/.gradle/caches"
if artifact pull workflow /tmp/build-cache.tar 2>/dev/null; then
  echo "Extracting Gradle build cache into ~/.gradle/caches/..."
  tar -xf /tmp/build-cache.tar -C "$HOME/.gradle/caches"
  rm -f /tmp/build-cache.tar
  echo "Build cache restored successfully."
else
  echo "Notice: Workflow build-cache artifact not available; will compile incrementally."
fi

mkdir -p "FlowCrypt/build/outputs"
if artifact pull workflow /tmp/apks.tar 2>/dev/null; then
  echo "Extracting pre-built APKs into FlowCrypt/build/outputs/..."
  tar -xf /tmp/apks.tar -C "FlowCrypt/build/outputs"
  rm -f /tmp/apks.tar
  echo "APKs restored successfully."
else
  echo "Notice: Workflow APKs artifact not available; will build as needed."
fi

echo "==> Build artifacts preparation finished."
