#!/bin/bash

set -euo pipefail

read -s -p "Android Keystore Password: " STORE_PWD
echo ""
read -s -p "Android Signing Key Password: " KEY_PWD
echo ""

./gradlew --console=verbose clean lintConsumerRelease assembleConsumerRelease assembleEnterpriseRelease renameReleaseBuilds copyReleaseApks -PruntimeSign -PstorePassword="$STORE_PWD" -PkeyPassword="$KEY_PWD"
