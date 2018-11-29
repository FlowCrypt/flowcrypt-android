#!/bin/bash

./gradlew clean assembleProductionRelease -PruntimeSetup --no-daemon
mv FlowCrypt/build/outputs/apk/production/release/FlowCrypt-production-release_*.apk release/
git add release/FlowCrypt-production-release_*.apk
