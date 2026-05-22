#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ -z "${1:-}" ]]; then
  echo "numShards is unset or set to the empty string"
  exit 1
fi

if [[ -z "${2:-}" ]]; then
  echo "shardIndex is unset or set to the empty string"
  exit 1
fi

numShards="$1"
shardIndex="$2"

if (( shardIndex >= numShards )); then
  echo "shardIndex should be lower than numShards"
  exit 1
fi

#print test names
#adb shell am instrument -w \
# -e filter com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter \
# -e numShards 3 \
# -e shardIndex 1 \
# -e log true \
# com.flowcrypt.email.debug.test/androidx.test.runner.AndroidJUnitRunner

./gradlew --console=plain --no-daemon --build-cache :FlowCrypt:connectedConsumerUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter \
  -Pandroid.testInstrumentationRunnerArguments.numShards="${numShards}" \
  -Pandroid.testInstrumentationRunnerArguments.shardIndex="${shardIndex}"
