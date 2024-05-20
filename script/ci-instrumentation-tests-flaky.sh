#!/bin/bash

./gradlew --console=plain :FlowCrypt:connectedEnterpriseUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.ReadyForCIAndFlakyFilter
