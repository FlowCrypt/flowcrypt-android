#!/bin/bash

set -euo pipefail

read -s -p "Android Keystore Password: " STORE_PWD
echo ""
read -s -p "Android Signing Key Password: " KEY_PWD
echo ""

./gradlew clean assembleProdRelease -PruntimeSign -PstorePassword="$STORE_PWD" -PkeyPassword="$KEY_PWD"
mv FlowCrypt/build/outputs/apk/prod/release/FlowCrypt-prod-release_*.apk release/
