#!/bin/bash

set -euxo pipefail

./gradlew clean assembleProdRelease -PruntimeSign -PstorePassword="CHANGE_ME" -PkeyPassword="CHANGE_ME"
mv FlowCrypt/build/outputs/apk/prod/release/FlowCrypt-production-release_*.apk release/
