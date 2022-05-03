#!/bin/bash

#print test names
#adb shell am instrument -r -w \
# -e filter com.flowcrypt.email.junit.filters.DependsOnMailServerFilter \
# -e log true \
# com.flowcrypt.email.debug.test/androidx.test.runner.AndroidJUnitRunner

./gradlew --console=plain :FlowCrypt:connectedDevTestDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.annotation=com.flowcrypt.email.junit.annotations.DebugTest
