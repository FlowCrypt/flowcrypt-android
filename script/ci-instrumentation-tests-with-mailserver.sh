#!/bin/bash

./gradlew --console=plain :FlowCrypt:connectedDevTestDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DependsOnMailServerFilter \
    -Pandroid.testInstrumentationRunnerArguments.clearPackageData=true
