#!/bin/bash

./gradlew --info --console=plain :FlowCrypt:connectedEnterpriseUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.EnterpriseTestsFilter
