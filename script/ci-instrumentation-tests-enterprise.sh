#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

./gradlew --console=plain --no-daemon --build-cache :FlowCrypt:connectedEnterpriseUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.EnterpriseTestsFilter
