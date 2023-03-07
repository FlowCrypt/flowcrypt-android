#!/bin/bash

./gradlew --console=plain :FlowCrypt:connectedConsumerUiTestsAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.FlakyTestsFilter
