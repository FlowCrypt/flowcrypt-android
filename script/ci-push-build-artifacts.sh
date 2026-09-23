#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

echo "==> Pushing build artifacts to Semaphore workflow storage..."

if [[ -d "$HOME/.gradle/caches/build-cache-1" ]]; then
  echo "Packaging Gradle build cache (uncompressed tar for speed)..."
  tar -cf /tmp/build-cache.tar -C "$HOME/.gradle/caches" build-cache-1
  echo "Uploading build-cache.tar ($(du -h /tmp/build-cache.tar | cut -f1))..."
  artifact push workflow /tmp/build-cache.tar
  rm -f /tmp/build-cache.tar
  echo "Build cache pushed successfully."
else
  echo "Warning: ~/.gradle/caches/build-cache-1 not found, skipping build cache push."
fi

if [[ -d "FlowCrypt/build/outputs/apk" ]]; then
  echo "Packaging built APKs..."
  tar -cf /tmp/apks.tar -C "FlowCrypt/build/outputs" apk
  echo "Uploading apks.tar ($(du -h /tmp/apks.tar | cut -f1))..."
  artifact push workflow /tmp/apks.tar
  rm -f /tmp/apks.tar
  echo "APKs pushed successfully."
else
  echo "Warning: FlowCrypt/build/outputs/apk not found, skipping APKs push."
fi

echo "==> Build artifacts published to workflow storage."
