#!/bin/bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

./gradlew --console=plain --no-daemon --build-cache --max-workers=2 --no-daemon --build-cache --max-workers=2 :FlowCrypt:connectedEnterpriseUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.EnterpriseTestsFilter
