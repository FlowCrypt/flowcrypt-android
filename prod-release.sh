#!/bin/bash

set -euxo pipefail

./gradlew clean assembleProdRelease -PruntimeSetup --no-daemon
mv FlowCrypt/build/outputs/apk/production/release/FlowCrypt-production-release_*.apk release/
