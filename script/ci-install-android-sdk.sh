#!/bin/bash

# Install Android SDK
(echo "y" | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "system-images;android-32;google_apis;x86_64" > /dev/null | grep -v = || true)

