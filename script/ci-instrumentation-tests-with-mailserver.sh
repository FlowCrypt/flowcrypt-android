#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

#print test names
#adb shell am instrument -r -w \
# -e filter com.flowcrypt.email.junit.filters.DependsOnMailServerFilter \
# -e log true \
# com.flowcrypt.email.debug.test/androidx.test.runner.AndroidJUnitRunner

./gradlew --console=plain --no-daemon --build-cache :FlowCrypt:connectedConsumerUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DependsOnMailServerFilter
